package com.rdf112.sweep.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.text.format.Formatter
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rdf112.sweep.MainActivity
import com.rdf112.sweep.R
import com.rdf112.sweep.data.WhatsAppCleanerDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Pure anti-spam & utility policy for the Weekly Storage Digest notification:
 * - Never notifies more than once every 7 days (`MIN_DIGEST_INTERVAL_MS`).
 * - Never notifies if the user opened Midiastore in the last 48 hours (`MIN_IDLE_SINCE_OPEN_MS`).
 * - Strictly silent unless recoverable clutter reaches `>= 300 MB` (`MIN_RECOVERABLE_BYTES`).
 */
object WeeklyDigestPolicy {
    const val MIN_RECOVERABLE_BYTES: Long = 300L * 1024L * 1024L // 314,572,800 bytes (300 MB)
    const val MIN_DIGEST_INTERVAL_MS: Long = 7L * 24L * 60L * 60L * 1000L // 7 days
    const val MIN_IDLE_SINCE_OPEN_MS: Long = 48L * 60L * 60L * 1000L // 48 hours

    fun shouldSendWeeklyDigest(
        nowEpochMs: Long,
        lastDigestSentEpochMs: Long,
        lastAppOpenedEpochMs: Long,
        recoverableBytes: Long,
        notificationsEnabled: Boolean,
    ): Boolean {
        if (!notificationsEnabled) return false
        if (recoverableBytes < MIN_RECOVERABLE_BYTES) return false
        if (lastDigestSentEpochMs > 0L && (nowEpochMs - lastDigestSentEpochMs) < MIN_DIGEST_INTERVAL_MS) {
            return false
        }
        if (lastAppOpenedEpochMs > 0L && (nowEpochMs - lastAppOpenedEpochMs) < MIN_IDLE_SINCE_OPEN_MS) {
            return false
        }
        return true
    }
}

object WeeklyDigestScheduler {
    const val PREFS_NAME = "midiastore_prefs"
    const val KEY_LAST_DIGEST_EPOCH_MS = "weekly_digest_last_sent_ms"
    const val KEY_LAST_APP_OPENED_EPOCH_MS = "weekly_digest_last_app_open_ms"
    const val KEY_NOTIF_PROMPT_SHOWN = "weekly_digest_notif_prompt_shown_v1"
    const val UNIQUE_WORK_NAME = "midiastore_weekly_storage_digest_v1"
    const val CHANNEL_ID = "midiastore_weekly_digest"
    const val NOTIFICATION_ID = 2910

    fun recordAppOpenedAndSchedule(context: Context) {
        val appContext = context.applicationContext
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_APP_OPENED_EPOCH_MS, System.currentTimeMillis())
            .apply()

        runCatching {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<WeeklyStorageDigestWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }

    fun shouldPromptNotificationOptInAfterCleanup(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_NOTIF_PROMPT_SHOWN, false)
    }

    fun markNotificationOptInPromptShown(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTIF_PROMPT_SHOWN, true)
            .apply()
    }
}

class WeeklyStorageDigestWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val ctx = applicationContext
        val prefs = ctx.getSharedPreferences(WeeklyDigestScheduler.PREFS_NAME, Context.MODE_PRIVATE)
        val nowMs = System.currentTimeMillis()
        val lastDigestMs = prefs.getLong(WeeklyDigestScheduler.KEY_LAST_DIGEST_EPOCH_MS, 0L)
        val lastOpenMs = prefs.getLong(WeeklyDigestScheduler.KEY_LAST_APP_OPENED_EPOCH_MS, 0L)

        val hasNotifPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val notificationsEnabled = hasNotifPerm &&
            runCatching { NotificationManagerCompat.from(ctx).areNotificationsEnabled() }.getOrDefault(false)

        // Fast pre-check before touching MediaStore (< 1ms)
        if (!WeeklyDigestPolicy.shouldSendWeeklyDigest(
                nowEpochMs = nowMs,
                lastDigestSentEpochMs = lastDigestMs,
                lastAppOpenedEpochMs = lastOpenMs,
                recoverableBytes = WeeklyDigestPolicy.MIN_RECOVERABLE_BYTES,
                notificationsEnabled = notificationsEnabled,
            )
        ) {
            return@withContext Result.success()
        }

        if (!hasMediaReadAccess(ctx)) {
            return@withContext Result.success()
        }

        val recoverableBytes = estimateRecoverableBytesFast(ctx)
        if (!WeeklyDigestPolicy.shouldSendWeeklyDigest(
                nowEpochMs = nowMs,
                lastDigestSentEpochMs = lastDigestMs,
                lastAppOpenedEpochMs = lastOpenMs,
                recoverableBytes = recoverableBytes,
                notificationsEnabled = notificationsEnabled,
            )
        ) {
            return@withContext Result.success()
        }

        postDigestNotification(ctx, recoverableBytes)
        prefs.edit().putLong(WeeklyDigestScheduler.KEY_LAST_DIGEST_EPOCH_MS, nowMs).apply()
        Result.success()
    }

    private fun hasMediaReadAccess(ctx: Context): Boolean = when {
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 ->
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        else -> ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_IMAGES) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_VIDEO) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun estimateRecoverableBytesFast(ctx: Context): Long {
        var total = 0L
        val thirtyDaysAgoSec = (System.currentTimeMillis() / 1000L) - (30L * 24L * 3600L)

        // 1. Heavy videos >= 15 MB
        runCatching {
            val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val proj = arrayOf(MediaStore.MediaColumns.SIZE)
            val sel = "${MediaStore.MediaColumns.SIZE} >= ?"
            val args = arrayOf((15L * 1024L * 1024L).toString())
            ctx.contentResolver.query(uri, proj, sel, args, null)?.use { c ->
                var rows = 0
                while (c.moveToNext() && rows < 400) {
                    total += c.getLong(0).coerceAtLeast(0L)
                    rows++
                }
            }
        }

        // 2. Old screenshots (> 30 days)
        runCatching {
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val proj = arrayOf(MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.DISPLAY_NAME)
            val sel = "${MediaStore.MediaColumns.DATE_MODIFIED} <= ?"
            val args = arrayOf(thirtyDaysAgoSec.toString())
            ctx.contentResolver.query(uri, proj, sel, args, null)?.use { c ->
                var rows = 0
                while (c.moveToNext() && rows < 500) {
                    val size = c.getLong(0).coerceAtLeast(0L)
                    val rel = c.getString(1).orEmpty()
                    val name = c.getString(2).orEmpty()
                    if (rel.contains("Screenshot", ignoreCase = true) || name.contains("Screenshot", ignoreCase = true)) {
                        total += size
                    }
                    rows++
                }
            }
        }

        // 3. Social/Chat media if still under threshold
        if (total < WeeklyDigestPolicy.MIN_RECOVERABLE_BYTES) {
            runCatching {
                val chatItems = WhatsAppCleanerDataSource(ctx).scanViaMediaStore(ctx)
                total += chatItems.sumOf { it.sizeBytes.coerceAtLeast(0L) }
            }
        }
        return total
    }

    private fun postDigestNotification(ctx: Context, recoverableBytes: Long) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            WeeklyDigestScheduler.CHANNEL_ID,
            ctx.getString(R.string.weekly_digest_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = ctx.getString(R.string.weekly_digest_channel_desc)
            enableVibration(false)
        }
        nm.createNotificationChannel(channel)

        val formattedSize = Formatter.formatFileSize(ctx, recoverableBytes)
        val openIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_tab", "clean")
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx,
            WeeklyDigestScheduler.NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(ctx, WeeklyDigestScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_apple_clean)
            .setContentTitle(ctx.getString(R.string.weekly_digest_notif_title, formattedSize))
            .setContentText(ctx.getString(R.string.weekly_digest_notif_body, formattedSize))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(ctx.getString(R.string.weekly_digest_notif_body, formattedSize)),
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            nm.notify(WeeklyDigestScheduler.NOTIFICATION_ID, notification)
        }
    }
}

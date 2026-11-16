package com.rdf112.sweep.data.media

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.rdf112.sweep.domain.model.AccessLevel
import com.rdf112.sweep.domain.model.MediaKind
import com.rdf112.sweep.domain.permissions.MediaPermissionPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

data class MediaStorageBreakdown(
    val photosBytes: Long,
    val videosBytes: Long,
    val audioBytes: Long,
    val photosAccessible: Boolean,
    val videosAccessible: Boolean,
    val audioAccessible: Boolean,
) {
    val totalAccessibleMediaBytes: Long
        get() = photosBytes + videosBytes + audioBytes

    val hasAnyBreakdown: Boolean
        get() = photosAccessible || videosAccessible || audioAccessible
}

/**
 * Queries real MediaStore item byte totals per kind across accessible volumes.
 * Avoids non-portable SQL SUM() projections that some OEM ContentProviders reject.
 */
object AndroidMediaBreakdownReader {
    private const val MAX_ROWS_PER_KIND_PER_VOLUME = 100_000

    @SuppressLint("InlinedApi")
    suspend fun read(context: Context): MediaStorageBreakdown = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val granted = buildSet {
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            ).filterTo(this) { perm ->
                ContextCompat.checkSelfPermission(appContext, perm) == PackageManager.PERMISSION_GRANTED
            }
        }
        val photosAccess = MediaPermissionPolicy.accessLevel(Build.VERSION.SDK_INT, MediaKind.IMAGE, granted)
        val videosAccess = MediaPermissionPolicy.accessLevel(Build.VERSION.SDK_INT, MediaKind.VIDEO, granted)
        val audioAccess = MediaPermissionPolicy.accessLevel(Build.VERSION.SDK_INT, MediaKind.AUDIO, granted)

        val photosOk = photosAccess == AccessLevel.FULL || photosAccess == AccessLevel.PARTIAL
        val videosOk = videosAccess == AccessLevel.FULL || videosAccess == AccessLevel.PARTIAL
        val audioOk = audioAccess == AccessLevel.FULL || audioAccess == AccessLevel.PARTIAL

        val volumes = runCatching { MediaStore.getExternalVolumeNames(appContext) }
            .getOrElse { setOf(MediaStore.VOLUME_EXTERNAL_PRIMARY) }

        val photosBytes = if (photosOk) sumKind(appContext, MediaKind.IMAGE, volumes) else 0L
        val videosBytes = if (videosOk) sumKind(appContext, MediaKind.VIDEO, volumes) else 0L
        val audioBytes = if (audioOk) sumKind(appContext, MediaKind.AUDIO, volumes) else 0L

        MediaStorageBreakdown(
            photosBytes = photosBytes,
            videosBytes = videosBytes,
            audioBytes = audioBytes,
            photosAccessible = photosOk,
            videosAccessible = videosOk,
            audioAccessible = audioOk,
        )
    }

    private suspend fun sumKind(context: Context, kind: MediaKind, volumes: Set<String>): Long {
        val resolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.IS_PENDING,
        ) + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(MediaStore.MediaColumns.IS_TRASHED)
        } else {
            emptyArray()
        }

        var sum = 0L
        for (volume in volumes) {
            currentCoroutineContext().ensureActive()
            val uri = when (kind) {
                MediaKind.IMAGE -> MediaStore.Images.Media.getContentUri(volume)
                MediaKind.VIDEO -> MediaStore.Video.Media.getContentUri(volume)
                MediaKind.AUDIO -> MediaStore.Audio.Media.getContentUri(volume)
            }
            val cursor = try {
                resolver.query(uri, projection, null, null, null)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            } ?: continue

            cursor.use { c ->
                val sizeCol = c.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val pendingCol = c.getColumnIndex(MediaStore.MediaColumns.IS_PENDING)
                val trashedCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    c.getColumnIndex(MediaStore.MediaColumns.IS_TRASHED)
                } else {
                    -1
                }
                if (sizeCol < 0) return@use
                var count = 0
                while (count < MAX_ROWS_PER_KIND_PER_VOLUME && c.moveToNext()) {
                    count++
                    if (count % 256 == 0) currentCoroutineContext().ensureActive()
                    val isPending = pendingCol >= 0 && !c.isNull(pendingCol) && runCatching { c.getInt(pendingCol) }.getOrDefault(0) != 0
                    val isTrashed = trashedCol >= 0 && !c.isNull(trashedCol) && runCatching { c.getInt(trashedCol) }.getOrDefault(0) != 0
                    if (isPending || isTrashed) continue
                    val size = if (!c.isNull(sizeCol)) runCatching { c.getLong(sizeCol) }.getOrDefault(0L) else 0L
                    if (size > 0L) sum += size
                }
            }
        }
        return sum
    }
}

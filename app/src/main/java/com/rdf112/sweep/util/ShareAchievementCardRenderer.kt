package com.rdf112.sweep.util

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.format.Formatter
import android.widget.Toast
import androidx.core.content.FileProvider
import com.rdf112.sweep.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Pure Android Canvas OLED (#000000) viral achievement & invite card generator (+0 KB dependencies).
 *
 * - Renders a crisp 1080x1350 (4:5 portrait) PNG card in ~35ms with the Precision Storage Ring
 *   (zero AI sparkles), Midiastore brand lockup, real freed storage or on-device safety badge,
 *   and "< 8 MB • GOOGLE PLAY" pill.
 * - Shares natively via Android System Sharesheet (`Intent.createChooser` + `ClipData` preview +
 *   existing `${applicationId}.fileprovider`) so WhatsApp (Chats & Status), Telegram, and Instagram
 *   (Stories & Direct) appear at the top with zero `<intent-filter>` `ACTION_SEND` or `<queries>`.
 */
object ShareAchievementCardRenderer {
    private const val CARD_W = 1080
    private const val CARD_H = 1350
    private const val SHARE_SUBDIR = "share_cards"
    private const val SHARE_FILENAME = "midiastore_achievement_card.png"

    const val PLAY_STORE_VIRAL_CARD_URL =
        "https://play.google.com/store/apps/details?id=com.rdf112.midiastore&referrer=utm_source%3Dapp_share%26utm_medium%3Dviral_card"
    const val PLAY_STORE_INVITE_URL =
        "https://play.google.com/store/apps/details?id=com.rdf112.midiastore&referrer=utm_source%3Dapp_share%26utm_medium%3Dinvite_friend"

    suspend fun shareFreedSpaceOrInvite(
        activity: Activity,
        freedBytes: Long,
    ) {
        val isAchievement = freedBytes > 0L
        val targetUrl = if (isAchievement) PLAY_STORE_VIRAL_CARD_URL else PLAY_STORE_INVITE_URL
        val freedLabel = if (isAchievement) Formatter.formatFileSize(activity, freedBytes) else ""

        val shareText = if (isAchievement) {
            activity.getString(R.string.share_freed_result_message, freedLabel, targetUrl)
        } else {
            activity.getString(R.string.share_app_message, targetUrl)
        }

        val cardUri = withContext(Dispatchers.Default) {
            runCatching {
                renderCardUri(
                    context = activity,
                    freedBytes = freedBytes,
                    freedLabel = freedLabel,
                )
            }.getOrNull()
        }

        if (activity.isFinishing || activity.isDestroyed) return

        val sendIntent = if (cardUri != null) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, cardUri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_TITLE, activity.getString(R.string.share_card_chooser_subtitle))
                clipData = ClipData.newRawUri("Midiastore", cardUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
        }

        val chooser = Intent.createChooser(sendIntent, activity.getString(R.string.share_app_chooser_title)).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            activity.startActivity(chooser)
        }.onFailure {
            Toast.makeText(activity, R.string.share_app_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    fun renderCardBitmap(
        context: Context,
        freedBytes: Long,
        freedLabel: String,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // 1. Pure pitch-black OLED backdrop (#000000) + subtle studio radial blue glow
        canvas.drawColor(Color.BLACK)
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                CARD_W * 0.5f,
                CARD_H * 0.44f,
                CARD_W * 0.52f,
                intArrayOf(Color.argb(68, 10, 132, 255), Color.argb(18, 48, 209, 88), Color.TRANSPARENT),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, CARD_W.toFloat(), CARD_H.toFloat(), glowPaint)

        // 2. Outer Graphite OLED frame (#1C1C1E stroke on #08080B card)
        val cardRect = RectF(56f, 56f, CARD_W - 56f, CARD_H - 56f)
        val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B0B0E")
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2C2C2E")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, 64f, 64f, surfacePaint)
        canvas.drawRoundRect(cardRect, 64f, 64f, strokePaint)

        // 3. Top Brand Wordmark ("Midia" white + "store" #0A84FF)
        val boldSans = Typeface.create("sans-serif", Typeface.BOLD)
        val regSans = Typeface.create("sans-serif-medium", Typeface.NORMAL)

        val wordPaintWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = boldSans
            textSize = 62f
            color = Color.WHITE
        }
        val wordPaintBlue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = boldSans
            textSize = 62f
            color = Color.parseColor("#0A84FF")
        }
        val midiaStr = "Midia"
        val storeStr = "store"
        val totalWordW = wordPaintWhite.measureText(midiaStr) + wordPaintBlue.measureText(storeStr)
        val wordStartX = (CARD_W - totalWordW) / 2f
        canvas.drawText(midiaStr, wordStartX, 178f, wordPaintWhite)
        canvas.drawText(storeStr, wordStartX + wordPaintWhite.measureText(midiaStr), 178f, wordPaintBlue)

        // Top sub-pill: "< 8 MB • GOOGLE PLAY"
        val badgeText = context.getString(R.string.share_card_lightweight_badge)
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = boldSans
            textSize = 26f
            color = Color.parseColor("#64D2FF")
            letterSpacing = 0.06f
            textAlign = Paint.Align.CENTER
        }
        val badgePadX = 34f
        val badgeW = badgeTextPaint.measureText(badgeText) + badgePadX * 2f
        val badgeRect = RectF((CARD_W - badgeW) / 2f, 210f, (CARD_W + badgeW) / 2f, 266f)
        val badgeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#140A84FF")
            style = Paint.Style.FILL
        }
        val badgeBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#660A84FF")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(badgeRect, 999f, 999f, badgeFill)
        canvas.drawRoundRect(badgeRect, 999f, 999f, badgeBorder)
        canvas.drawText(badgeText, CARD_W / 2f, 248f, badgeTextPaint)

        // 4. Center Signature Precision Storage Ring (Zero AI sparkles — geometric dual-arc gauge)
        val cx = CARD_W / 2f
        val cy = 625f
        val radius = 235f
        val ringRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1C1E")
            style = Paint.Style.STROKE
            strokeWidth = 36f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(ringRect, 0f, 360f, false, trackPaint)

        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cx - radius,
                cy - radius,
                cx + radius,
                cy + radius,
                intArrayOf(Color.parseColor("#0A84FF"), Color.parseColor("#64D2FF"), Color.parseColor("#30D158")),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
            style = Paint.Style.STROKE
            strokeWidth = 36f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(ringRect, -90f, 286f, false, arcPaint)

        // Inner precision tick ring
        val innerRadius = 186f
        val innerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3364D2FF")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(cx, cy, innerRadius, innerRingPaint)

        // Metric inside the ring
        val isAchievement = freedBytes > 0L && freedLabel.isNotBlank()
        val mainMetric = if (isAchievement) freedLabel else "100%"
        val metricPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = boldSans
            textSize = if (mainMetric.length > 8) 74f else 92f
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(mainMetric, cx, cy + 16f, metricPaint)

        val ringSub = if (isAchievement) {
            context.getString(R.string.share_card_freed_label)
        } else {
            "ON-DEVICE"
        }
        val ringSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = boldSans
            textSize = 26f
            color = Color.parseColor("#30D158")
            letterSpacing = 0.08f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(ringSub, cx, cy + 72f, ringSubPaint)

        // 5. Headline & Trust Subtitle below ring
        val headline = if (isAchievement) {
            context.getString(R.string.share_card_headline_freed)
        } else {
            context.getString(R.string.brand_tagline)
        }
        drawCenteredMultiline(
            canvas = canvas,
            text = headline,
            cx = cx,
            startY = 980f,
            maxWidth = CARD_W - 180f,
            paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = boldSans
                textSize = 44f
                color = Color.WHITE
                textAlign = Paint.Align.CENTER
            },
            lineHeight = 56f,
        )

        val trustLine = context.getString(R.string.share_card_trust_footer)
        drawCenteredMultiline(
            canvas = canvas,
            text = trustLine,
            cx = cx,
            startY = 1118f,
            maxWidth = CARD_W - 180f,
            paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = regSans
                textSize = 28f
                color = Color.parseColor("#98989D")
                textAlign = Paint.Align.CENTER
            },
            lineHeight = 38f,
        )

        // Bottom URL pill
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = boldSans
            textSize = 26f
            color = Color.parseColor("#0A84FF")
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Google Play • Midiastore", cx, 1236f, footerPaint)

        return bmp
    }

    private fun renderCardUri(
        context: Context,
        freedBytes: Long,
        freedLabel: String,
    ): Uri {
        val bitmap = renderCardBitmap(context, freedBytes, freedLabel)
        val dir = File(context.cacheDir, SHARE_SUBDIR).apply { mkdirs() }
        val outFile = File(dir, SHARE_FILENAME)
        outFile.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile,
        )
    }

    private fun drawCenteredMultiline(
        canvas: Canvas,
        text: String,
        cx: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        lineHeight: Float,
    ) {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return
        val lines = ArrayList<String>(3)
        var current = ""
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) {
                current = candidate
            } else {
                lines += current
                current = word
            }
        }
        if (current.isNotEmpty()) lines += current
        lines.take(2).forEachIndexed { idx, line ->
            canvas.drawText(line, cx, startY + (idx * lineHeight), paint)
        }
    }
}

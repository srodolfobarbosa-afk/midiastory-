package com.rdf112.sweep.presentation.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.CancellationSignal
import android.text.TextUtils
import android.text.format.Formatter
import android.util.Size
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.rdf112.sweep.R
import com.rdf112.sweep.domain.analysis.VideoCompressionPlanner
import com.rdf112.sweep.domain.model.MediaKind
import com.rdf112.sweep.domain.model.MediaRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full-screen OLED (#000000) visual inspection modal before deletion:
 * - Photos: high-resolution sampled decode (up to 2048px) with smooth 1.0x..4.5x pinch-to-zoom,
 *   double-tap 2.5x zoom, and pan gestures.
 * - Videos / Audio: instant native hardware VideoView + MediaController playback + optional 1-tap
 *   entry into Hardware H.265/HEVC Pro Compression for large videos.
 */
object MediaFullscreenPreviewDialog {
    fun show(
        activity: AppCompatActivity,
        record: MediaRecord,
        onCompressVideoRequested: ((MediaRecord) -> Unit)? = null,
    ) {
        if (activity.isFinishing || activity.isDestroyed) return
        val density = activity.resources.displayMetrics.density
        fun dp(v: Int): Int = (v * density).toInt()

        val dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.let { win ->
            WindowCompat.setDecorFitsSystemWindows(win, false)
            win.statusBarColor = Color.BLACK
            win.navigationBarColor = Color.BLACK
        }

        val root = FrameLayout(activity).apply {
            setBackgroundColor(Color.BLACK)
        }

        val spinner = ProgressBar(activity).apply {
            indeterminateTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#0A84FF"))
        }
        root.addView(
            spinner,
            FrameLayout.LayoutParams(dp(42), dp(42), Gravity.CENTER),
        )

        var decodeJob: Job? = null
        var loadedBitmap: Bitmap? = null
        var videoViewRef: VideoView? = null

        when (record.kind) {
            MediaKind.IMAGE -> {
                val zoomView = PinchZoomImageView(activity).apply {
                    visibility = View.INVISIBLE
                }
                root.addView(
                    zoomView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
                val cancelSignal = CancellationSignal()
                decodeJob = activity.lifecycleScope.launch {
                    val bmp = withContext(Dispatchers.IO) {
                        decodePreviewBitmap(activity, Uri.parse(record.uri), maxSidePx = 2048, cancelSignal)
                    }
                    spinner.visibility = View.GONE
                    if (bmp != null) {
                        loadedBitmap = bmp
                        zoomView.setPreviewBitmap(bmp)
                        zoomView.visibility = View.VISIBLE
                    } else {
                        root.addView(
                            TextView(activity).apply {
                                text = activity.getString(R.string.preview_load_failed)
                                textSize = 14f
                                setTextColor(Color.parseColor("#8E8E93"))
                                gravity = Gravity.CENTER
                            },
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                Gravity.CENTER,
                            ),
                        )
                    }
                }
                decodeJob.invokeOnCompletion { cause ->
                    if (cause is CancellationException) cancelSignal.cancel()
                }
            }

            MediaKind.VIDEO, MediaKind.AUDIO -> {
                val playerHolder = FrameLayout(activity).apply {
                    setBackgroundColor(Color.BLACK)
                }
                val videoView = VideoView(activity)
                videoViewRef = videoView
                val controller = MediaController(activity)
                controller.setAnchorView(playerHolder)
                videoView.setMediaController(controller)
                videoView.setVideoURI(Uri.parse(record.uri))
                videoView.setOnPreparedListener { mp ->
                    spinner.visibility = View.GONE
                    mp.isLooping = false
                    runCatching { videoView.start() }
                }
                videoView.setOnErrorListener { _, _, _ ->
                    spinner.visibility = View.GONE
                    true
                }
                playerHolder.addView(
                    videoView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER,
                    ),
                )
                root.addView(
                    playerHolder,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        }

        // Top translucent OLED bar with metadata + protected badge + Close pill
        val topBar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val titleCol = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
        val fileName = record.displayName?.takeIf(String::isNotBlank) ?: activity.getString(R.string.unnamed_file)
        titleCol.addView(
            TextView(activity).apply {
                text = fileName
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.MIDDLE
            },
        )
        val sizeStr = Formatter.formatFileSize(activity, record.sizeBytes)
        val subLine = if (record.isProtected) {
            "$sizeStr · ${activity.getString(R.string.similar_best_photo_badge)}"
        } else {
            val hint = if (record.kind == MediaKind.IMAGE) {
                activity.getString(R.string.preview_pinch_zoom_hint)
            } else {
                activity.getString(R.string.video_label)
            }
            "$sizeStr · $hint"
        }
        titleCol.addView(
            TextView(activity).apply {
                text = subLine
                textSize = 11.5f
                setTextColor(if (record.isProtected) Color.parseColor("#30D158") else Color.parseColor("#98989D"))
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            },
        )
        topBar.addView(titleCol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(10) })

        val closeBtn = MaterialButton(activity).apply {
            text = activity.getString(R.string.preview_close_button)
            isAllCaps = false
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            minHeight = dp(40)
            cornerRadius = dp(20)
            insetTop = 0
            insetBottom = 0
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#2C2C2E"))
            setOnClickListener { dialog.dismiss() }
        }
        topBar.addView(closeBtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(
            topBar,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP,
            ),
        )

        // Optional bottom Pro CTA to compress heavy videos (>= 15 MB) right from preview
        val bottomBar = if (
            record.kind == MediaKind.VIDEO &&
            record.sizeBytes >= VideoCompressionPlanner.MIN_COMPRESSIBLE_VIDEO_BYTES &&
            onCompressVideoRequested != null
        ) {
            LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#CC000000"))
                setPadding(dp(18), dp(12), dp(18), dp(18))
                val compressBtn = MaterialButton(activity).apply {
                    text = activity.getString(R.string.video_compress_preview_cta)
                    isAllCaps = false
                    textSize = 13.5f
                    typeface = Typeface.DEFAULT_BOLD
                    minHeight = dp(50)
                    cornerRadius = dp(14)
                    insetTop = 0
                    insetBottom = 0
                    setTextColor(Color.WHITE)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#0A84FF"))
                    setOnClickListener {
                        dialog.dismiss()
                        onCompressVideoRequested.invoke(record)
                    }
                }
                addView(
                    compressBtn,
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
                )
            }
        } else {
            null
        }
        if (bottomBar != null) {
            root.addView(
                bottomBar,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM,
                ),
            )
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topBar.setPadding(dp(16), bars.top + dp(10), dp(16), dp(12))
            bottomBar?.setPadding(dp(18), dp(10), dp(18), bars.bottom + dp(14))
            insets
        }

        dialog.setContentView(root)
        dialog.setOnDismissListener {
            decodeJob?.cancel()
            runCatching { videoViewRef?.stopPlayback() }
            videoViewRef = null
            loadedBitmap = null
        }
        dialog.show()
    }

    private fun decodePreviewBitmap(
        context: Context,
        uri: Uri,
        maxSidePx: Int,
        signal: CancellationSignal,
    ): Bitmap? {
        val resolver = context.contentResolver
        // First read bounds without allocating pixels
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching {
            resolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            }
        }
        val rawW = bounds.outWidth
        val rawH = bounds.outHeight
        if (rawW > 0 && rawH > 0) {
            var sample = 1
            while ((rawW / sample) > maxSidePx || (rawH / sample) > maxSidePx) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = runCatching {
                resolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, opts)
                }
            }.getOrNull()
            if (decoded != null) return decoded
        }
        return runCatching {
            resolver.loadThumbnail(uri, Size(1024, 1024), signal)
        }.getOrNull()
    }
}

/**
 * Lightweight, dependency-free pinch-to-zoom + double-tap + pan ImageView.
 */
private class PinchZoomImageView(context: Context) : ImageView(context) {
    private val drawMatrix = Matrix()
    private val matrixValues = FloatArray(9)
    private var currentScale = 1f
    private var minScale = 1f
    private var maxScale = 4.5f
    private var bitmapW = 0f
    private var bitmapH = 0f
    private val lastTouch = PointF()
    private var isDragging = false

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val factor = detector.scaleFactor
                val target = (currentScale * factor).coerceIn(minScale, maxScale)
                val applied = target / currentScale
                drawMatrix.postScale(applied, applied, detector.focusX, detector.focusY)
                currentScale = target
                constrainTranslation()
                imageMatrix = drawMatrix
                return true
            }
        },
    )

    private val tapDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val nextScale = if (currentScale > minScale * 1.15f) minScale else (minScale * 2.5f).coerceAtMost(maxScale)
                val factor = nextScale / currentScale
                drawMatrix.postScale(factor, factor, e.x, e.y)
                currentScale = nextScale
                if (currentScale <= minScale * 1.02f) {
                    fitCenterInitial()
                } else {
                    constrainTranslation()
                    imageMatrix = drawMatrix
                }
                return true
            }
        },
    )

    init {
        scaleType = ScaleType.MATRIX
        setBackgroundColor(Color.BLACK)
    }

    fun setPreviewBitmap(bitmap: Bitmap) {
        bitmapW = bitmap.width.toFloat()
        bitmapH = bitmap.height.toFloat()
        setImageBitmap(bitmap)
        post { fitCenterInitial() }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0 && bitmapW > 0f && bitmapH > 0f) {
            fitCenterInitial()
        }
    }

    private fun fitCenterInitial() {
        if (width <= 0 || height <= 0 || bitmapW <= 0f || bitmapH <= 0f) return
        val scale = minOf(width / bitmapW, height / bitmapH)
        minScale = scale
        maxScale = (scale * 4.5f).coerceAtLeast(scale * 2f)
        currentScale = scale
        val dx = (width - bitmapW * scale) * 0.5f
        val dy = (height - bitmapH * scale) * 0.5f
        drawMatrix.reset()
        drawMatrix.postScale(scale, scale)
        drawMatrix.postTranslate(dx, dy)
        imageMatrix = drawMatrix
    }

    private fun constrainTranslation() {
        if (width <= 0 || height <= 0 || bitmapW <= 0f || bitmapH <= 0f) return
        drawMatrix.getValues(matrixValues)
        val transX = matrixValues[Matrix.MTRANS_X]
        val transY = matrixValues[Matrix.MTRANS_Y]
        val scaledW = bitmapW * currentScale
        val scaledH = bitmapH * currentScale

        val fixX = if (scaledW <= width) {
            (width - scaledW) * 0.5f - transX
        } else {
            when {
                transX > 0f -> -transX
                transX + scaledW < width -> width - (transX + scaledW)
                else -> 0f
            }
        }
        val fixY = if (scaledH <= height) {
            (height - scaledH) * 0.5f - transY
        } else {
            when {
                transY > 0f -> -transY
                transY + scaledH < height -> height - (transY + scaledH)
                else -> 0f
            }
        }
        if (fixX != 0f || fixY != 0f) {
            drawMatrix.postTranslate(fixX, fixY)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        tapDetector.onTouchEvent(event)
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouch.set(event.x, event.y)
                isDragging = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && !scaleDetector.isInProgress && currentScale > minScale * 1.02f) {
                    val dx = event.x - lastTouch.x
                    val dy = event.y - lastTouch.y
                    drawMatrix.postTranslate(dx, dy)
                    constrainTranslation()
                    imageMatrix = drawMatrix
                    lastTouch.set(event.x, event.y)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> {
                isDragging = false
            }
        }
        return true
    }
}

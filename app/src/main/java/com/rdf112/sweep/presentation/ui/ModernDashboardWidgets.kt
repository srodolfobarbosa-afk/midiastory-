package com.rdf112.sweep.presentation.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View

/**
 * Medidor Circular Premium do Sweep com arco gradiente Esmeralda -> Ciano -> Âmbar,
 * trilha translúcida e percentual centralizado de alta nitidez (sem warnings de compilação).
 */
class StorageRingGaugeView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val strokePx = 12f * density
    private val arcRect = RectF()

    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#1C2942")
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#00E5B0")
    }

    private val percentTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F8FAFC")
        textSize = sp(24f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5DF5D3")
        textSize = sp(10f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    var usedFraction: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            updateAccessibilityLabel()
            invalidate()
        }

    var centerLabel: String = "—"
        set(value) {
            field = value
            updateAccessibilityLabel()
            invalidate()
        }

    var bottomLabel: String = "USED"
        set(value) {
            field = value
            updateAccessibilityLabel()
            invalidate()
        }

    var isProVipTheme: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                trackPaint.color = Color.parseColor(if (value) "#2C2208" else "#1C2942")
                subTextPaint.color = Color.parseColor(if (value) "#FFD700" else "#5DF5D3")
                updateProgressShader()
                invalidate()
            }
        }

    private fun updateAccessibilityLabel() {
        contentDescription = "$centerLabel $bottomLabel"
    }

    private fun updateProgressShader() {
        if (arcRect.width() <= 0f || arcRect.height() <= 0f) return
        val colors = if (isProVipTheme) {
            intArrayOf(
                Color.parseColor("#FFD700"),
                Color.parseColor("#FFAB00"),
                Color.parseColor("#FEF08A"),
            )
        } else {
            intArrayOf(
                Color.parseColor("#00E5B0"),
                Color.parseColor("#38BDF8"),
                Color.parseColor("#FBBF24"),
            )
        }
        progressPaint.shader = LinearGradient(
            arcRect.left,
            arcRect.top,
            arcRect.right,
            arcRect.bottom,
            colors,
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP,
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        val pad = strokePx + 4f * density
        val size = minOf(w, h).toFloat()
        val left = (w - size) / 2f + pad
        val top = (h - size) / 2f + pad
        val right = (w - left).coerceAtLeast(left + 1f)
        val bottom = (h - top).coerceAtLeast(top + 1f)
        arcRect.set(left, top, right, bottom)
        updateProgressShader()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        canvas.drawArc(arcRect, 135f, 270f, false, trackPaint)
        val sweep = 270f * usedFraction
        canvas.drawArc(arcRect, 135f, sweep, false, progressPaint)

        val cx = width / 2f
        val cy = height / 2f
        canvas.drawText(centerLabel, cx, cy + 4f * density, percentTextPaint)
        canvas.drawText(bottomLabel, cx, cy + 22f * density, subTextPaint)
    }
}

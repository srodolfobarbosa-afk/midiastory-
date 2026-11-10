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
            invalidate()
        }

    var centerLabel: String = "—"
        set(value) {
            field = value
            invalidate()
        }

    var bottomLabel: String = "USED"
        set(value) {
            field = value
            invalidate()
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val pad = strokePx + 4f * density
        val size = minOf(w, h).toFloat()
        val left = (w - size) / 2f + pad
        val top = (h - size) / 2f + pad
        arcRect.set(left, top, w - left, h - top)
        progressPaint.shader = LinearGradient(
            arcRect.left,
            arcRect.top,
            arcRect.right,
            arcRect.bottom,
            intArrayOf(
                Color.parseColor("#00E5B0"),
                Color.parseColor("#38BDF8"),
                Color.parseColor("#FBBF24"),
            ),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP,
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawArc(arcRect, 135f, 270f, false, trackPaint)
        val sweep = 270f * usedFraction
        canvas.drawArc(arcRect, 135f, sweep, false, progressPaint)

        val cx = width / 2f
        val cy = height / 2f
        canvas.drawText(centerLabel, cx, cy + 4f * density, percentTextPaint)
        canvas.drawText(bottomLabel, cx, cy + 22f * density, subTextPaint)
    }
}

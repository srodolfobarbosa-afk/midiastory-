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
 * Medidor Circular Apple-Grade do Midiastore com arco gradiente Apple Blue / VIP Gold,
 * trilha neutra translúcida e percentual centralizado de alta nitidez.
 */
class StorageRingGaugeView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val strokePx = 11f * density
    private val arcRect = RectF()

    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#2C2C2E")
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#0A84FF")
    }

    private val percentTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = sp(24f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8E8E93")
        textSize = sp(10.5f)
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

    var bottomLabel: String = "USADO"
        set(value) {
            field = value
            updateAccessibilityLabel()
            invalidate()
        }

    var isProVipTheme: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                trackPaint.color = Color.parseColor(if (value) "#3A2E0F" else "#2C2C2E")
                subTextPaint.color = Color.parseColor(if (value) "#FFD60A" else "#8E8E93")
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
                Color.parseColor("#FFD60A"),
                Color.parseColor("#FF9F0A"),
            )
        } else {
            intArrayOf(
                Color.parseColor("#0A84FF"),
                Color.parseColor("#64D2FF"),
            )
        }
        progressPaint.shader = LinearGradient(
            arcRect.left,
            arcRect.top,
            arcRect.right,
            arcRect.bottom,
            colors,
            floatArrayOf(0f, 1f),
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

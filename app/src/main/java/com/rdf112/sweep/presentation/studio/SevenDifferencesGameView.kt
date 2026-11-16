package com.rdf112.sweep.presentation.studio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View

/**
 * 100% Offline Seven Differences Game View (Jogo dos 7 Erros).
 * Draws two coordinated vector scenes with 7 subtle differences, touch detection,
 * haptic feedback, hint system, and victory celebrations.
 */
class SevenDifferencesGameView(context: Context) : View(context) {

    data class DifferenceSpot(
        val id: Int,
        val nx: Float, // Normalized X (0.0 to 1.0)
        val ny: Float, // Normalized Y (0.0 to 1.0 inside panel)
        val descriptionPt: String,
        val descriptionEn: String,
        var isFound: Boolean = false,
    )

    data class SceneData(
        val titlePt: String,
        val titleEn: String,
        val spots: List<DifferenceSpot>,
    )

    private val density = resources.displayMetrics.density
    private var isPortuguese: Boolean = true

    private val bgPaint = Paint().apply { color = Color.parseColor("#1C1C1E") }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = Color.parseColor("#3A3A3C")
    }
    private val foundCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        color = Color.parseColor("#30D158")
    }
    private val foundFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#3330D158")
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f * density
        color = Color.parseColor("#FFD60A")
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 14f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8E8E93")
        textSize = 11f * density
        typeface = Typeface.DEFAULT_BOLD
    }

    // Vetores de desenho de cena
    private val skyPaint = Paint().apply { color = Color.parseColor("#1A2B4C") }
    private val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD60A") }
    private val grassPaint = Paint().apply { color = Color.parseColor("#1C3829") }
    private val housePaint = Paint().apply { color = Color.parseColor("#4A3728") }
    private val roofPaintA = Paint().apply { color = Color.parseColor("#E05638") }
    private val roofPaintB = Paint().apply { color = Color.parseColor("#3890E0") } // Erro 1
    private val treeTrunkPaint = Paint().apply { color = Color.parseColor("#3E2723") }
    private val treeFoliagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2E7D32") }
    private val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFFFFF") }

    private var activeSceneIndex = 0
    private var scenes: List<SceneData> = createScenes()

    var onProgressChanged: ((found: Int, total: Int, lastFoundName: String) -> Unit)? = null
    var onAllFound: (() -> Unit)? = null

    private var activeHintSpotId: Int? = null

    init {
        isClickable = true
        isFocusable = true
    }

    fun setLanguage(isPt: Boolean) {
        isPortuguese = isPt
        invalidate()
    }

    private fun createScenes(): List<SceneData> = listOf(
        SceneData(
            titlePt = "Cena 1: A Casa de Campo",
            titleEn = "Scene 1: Countryside Home",
            spots = listOf(
                DifferenceSpot(1, 0.50f, 0.44f, "Cor do telhado", "Roof color"),
                DifferenceSpot(2, 0.82f, 0.22f, "Sol com raios extras", "Sun extra rays"),
                DifferenceSpot(3, 0.18f, 0.28f, "Nuvem azul suave", "Extra sky cloud"),
                DifferenceSpot(4, 0.25f, 0.55f, "Maçã vermelha na árvore", "Red apple in tree"),
                DifferenceSpot(5, 0.62f, 0.38f, "Fumaça saindo da chaminé", "Chimney smoke"),
                DifferenceSpot(6, 0.78f, 0.78f, "Flor amarela no gramado", "Yellow flower in grass"),
                DifferenceSpot(7, 0.36f, 0.82f, "Caminho de pedras", "Stone walkway path"),
            )
        ),
        SceneData(
            titlePt = "Cena 2: O Lago e a Montanha",
            titleEn = "Scene 2: Mountain Lake",
            spots = listOf(
                DifferenceSpot(1, 0.30f, 0.25f, "Pássaro voando no pico", "Flying bird on peak"),
                DifferenceSpot(2, 0.75f, 0.65f, "Barquinho a vela no lago", "Sailboat in the lake"),
                DifferenceSpot(3, 0.15f, 0.70f, "Peixinho saltando", "Jumping fish"),
                DifferenceSpot(4, 0.55f, 0.32f, "Neve no topo da montanha", "Snow on mountain top"),
                DifferenceSpot(5, 0.85f, 0.38f, "Pinheiro extra na encosta", "Extra pine tree"),
                DifferenceSpot(6, 0.45f, 0.85f, "Pedra redonda na margem", "Round shoreline stone"),
                DifferenceSpot(7, 0.65f, 0.18f, "Estrela cadente / brilho", "Shooting star glow"),
            )
        )
    )

    fun foundCount(): Int = scenes[activeSceneIndex].spots.count { it.isFound }
    fun totalCount(): Int = scenes[activeSceneIndex].spots.size

    fun nextScene() {
        activeSceneIndex = (activeSceneIndex + 1) % scenes.size
        scenes[activeSceneIndex].spots.forEach { it.isFound = false }
        activeHintSpotId = null
        onProgressChanged?.invoke(0, totalCount(), "")
        invalidate()
    }

    fun restartCurrentScene() {
        scenes[activeSceneIndex].spots.forEach { it.isFound = false }
        activeHintSpotId = null
        onProgressChanged?.invoke(0, totalCount(), "")
        invalidate()
    }

    fun provideHint(): Boolean {
        val unfound = scenes[activeSceneIndex].spots.filter { !it.isFound }
        if (unfound.isEmpty()) return false
        val hintTarget = unfound.random()
        activeHintSpotId = hintTarget.id
        invalidate()
        postDelayed({
            if (activeHintSpotId == hintTarget.id) {
                activeHintSpotId = null
                invalidate()
            }
        }, 3000)
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return super.onTouchEvent(event)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return true

        val panelH = (h - 20f * density) / 2f
        val touchY = event.y
        val touchX = event.x

        // Identifica se tocou no painel superior ou inferior
        val isBottomPanel = touchY >= (panelH + 10f * density)
        val relativeY = if (isBottomPanel) touchY - (panelH + 10f * density) else touchY
        val nx = (touchX / w).coerceIn(0f, 1f)
        val ny = (relativeY / panelH).coerceIn(0f, 1f)

        // Raio de tolerância ao toque (~0.11 da largura)
        val tolerance = 0.11f
        val currentSpots = scenes[activeSceneIndex].spots

        for (spot in currentSpots) {
            if (spot.isFound) continue
            val dx = nx - spot.nx
            val dy = ny - spot.ny
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            if (dist <= tolerance) {
                spot.isFound = true
                if (activeHintSpotId == spot.id) activeHintSpotId = null
                try {
                    performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                } catch (_: Exception) {}

                val found = foundCount()
                val total = totalCount()
                val name = if (isPortuguese) spot.descriptionPt else spot.descriptionEn
                onProgressChanged?.invoke(found, total, name)
                if (found == total) {
                    onAllFound?.invoke()
                }
                invalidate()
                return true
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val gap = 10f * density
        val panelH = (h - gap) / 2f

        // 1. Painel Superior: Cena A (Original)
        drawScenePanel(canvas, 0f, 0f, w, panelH, isModifiedPanel = false)

        // 2. Painel Inferior: Cena B (Com os 7 Erros)
        drawScenePanel(canvas, 0f, panelH + gap, w, panelH, isModifiedPanel = true)

        // 3. Divisória e Labels
        val labelA = if (isPortuguese) "IMAGEM 1 (ORIGINAL)" else "IMAGE 1 (ORIGINAL)"
        val labelB = if (isPortuguese) "IMAGEM 2 (ENCONTRE OS 7 ERROS)" else "IMAGE 2 (SPOT THE 7 ERRORS)"
        canvas.drawText(labelA, 14f * density, 20f * density, labelPaint)
        canvas.drawText(labelB, 14f * density, panelH + gap + 20f * density, labelPaint)
    }

    private fun drawScenePanel(
        canvas: Canvas,
        left: Float,
        top: Float,
        w: Float,
        h: Float,
        isModifiedPanel: Boolean,
    ) {
        val rect = RectF(left, top, left + w, top + h)
        canvas.drawRoundRect(rect, 14f * density, 14f * density, bgPaint)
        canvas.drawRoundRect(rect, 14f * density, 14f * density, borderPaint)

        // Céu
        val skyRect = RectF(left, top, left + w, top + h * 0.65f)
        canvas.drawRect(skyRect, skyPaint)

        // Sol
        val sunCx = left + w * 0.82f
        val sunCy = top + h * 0.22f
        canvas.drawCircle(sunCx, sunCy, 18f * density, sunPaint)
        if (isModifiedPanel) {
            // Erro 2: Raios de sol extras
            for (i in 0 until 8) {
                val angle = Math.toRadians((i * 45).toDouble())
                val rx1 = sunCx + (22f * density * kotlin.math.cos(angle)).toFloat()
                val ry1 = sunCy + (22f * density * kotlin.math.sin(angle)).toFloat()
                val rx2 = sunCx + (28f * density * kotlin.math.cos(angle)).toFloat()
                val ry2 = sunCy + (28f * density * kotlin.math.sin(angle)).toFloat()
                canvas.drawLine(rx1, ry1, rx2, ry2, sunPaint.apply { strokeWidth = 2f * density })
            }
        }

        // Nuvem
        val cloudX = left + w * 0.18f
        val cloudY = top + h * 0.28f
        detailPaint.color = Color.parseColor("#44FFFFFF")
        canvas.drawCircle(cloudX, cloudY, 14f * density, detailPaint)
        canvas.drawCircle(cloudX + 12f * density, cloudY - 4f * density, 16f * density, detailPaint)
        canvas.drawCircle(cloudX + 24f * density, cloudY, 12f * density, detailPaint)
        if (isModifiedPanel) {
            // Erro 3: Nuvem a mais
            val extraCloudX = left + w * 0.44f
            val extraCloudY = top + h * 0.20f
            detailPaint.color = Color.parseColor("#7764D2FF")
            canvas.drawCircle(extraCloudX, extraCloudY, 12f * density, detailPaint)
            canvas.drawCircle(extraCloudX + 10f * density, extraCloudY - 3f * density, 14f * density, detailPaint)
        }

        // Gramado
        val grassRect = RectF(left, top + h * 0.65f, left + w, top + h)
        canvas.drawRect(grassRect, grassPaint)

        // Casa
        val houseL = left + w * 0.40f
        val houseT = top + h * 0.45f
        val houseR = left + w * 0.68f
        val houseB = top + h * 0.75f
        canvas.drawRect(houseL, houseT, houseR, houseB, housePaint)

        // Telhado (Erro 1: cor diferente no painel B)
        val roof = android.graphics.Path().apply {
            moveTo(houseL - 8f * density, houseT)
            lineTo((houseL + houseR) / 2f, houseT - 24f * density)
            lineTo(houseR + 8f * density, houseT)
            close()
        }
        canvas.drawPath(roof, if (isModifiedPanel) roofPaintB else roofPaintA)

        // Chaminé
        val chimL = houseR - 16f * density
        val chimT = houseT - 28f * density
        canvas.drawRect(chimL, chimT, chimL + 10f * density, houseT, housePaint)
        if (isModifiedPanel) {
            // Erro 5: Fumaça da chaminé
            detailPaint.color = Color.parseColor("#88CCCCCC")
            canvas.drawCircle(chimL + 5f * density, chimT - 8f * density, 6f * density, detailPaint)
            canvas.drawCircle(chimL + 8f * density, chimT - 18f * density, 8f * density, detailPaint)
        }

        // Árvore
        val trunkX = left + w * 0.22f
        val trunkT = top + h * 0.52f
        canvas.drawRect(trunkX, trunkT, trunkX + 12f * density, top + h * 0.75f, treeTrunkPaint)
        canvas.drawCircle(trunkX + 6f * density, trunkT - 14f * density, 24f * density, treeFoliagePaint)

        if (isModifiedPanel) {
            // Erro 4: Maçã vermelha na árvore
            detailPaint.color = Color.parseColor("#FF453A")
            canvas.drawCircle(trunkX + 10f * density, trunkT - 10f * density, 5f * density, detailPaint)
            canvas.drawCircle(trunkX - 4f * density, trunkT - 18f * density, 5f * density, detailPaint)
        }

        // Caminho de Pedras (Erro 7 no painel B)
        if (isModifiedPanel) {
            detailPaint.color = Color.parseColor("#55E5E5EA")
            for (p in 0..3) {
                canvas.drawOval(
                    RectF(
                        left + w * 0.32f + p * 8f * density,
                        top + h * 0.78f + p * 6f * density,
                        left + w * 0.38f + p * 8f * density,
                        top + h * 0.83f + p * 6f * density,
                    ),
                    detailPaint
                )
            }
        }

        // Flor Amarela (Erro 6 no painel B)
        if (isModifiedPanel) {
            detailPaint.color = Color.parseColor("#FFD60A")
            canvas.drawCircle(left + w * 0.78f, top + h * 0.78f, 6f * density, detailPaint)
        }

        // Renderização dos acertos e dicas no Painel Inferior
        if (isModifiedPanel) {
            val spots = scenes[activeSceneIndex].spots
            spots.forEach { spot ->
                val cx = left + w * spot.nx
                val cy = top + h * spot.ny
                val radius = 18f * density

                if (spot.isFound) {
                    canvas.drawCircle(cx, cy, radius, foundFillPaint)
                    canvas.drawCircle(cx, cy, radius, foundCirclePaint)
                    canvas.drawText("${spot.id}", cx, cy + 5f * density, textPaint)
                } else if (activeHintSpotId == spot.id) {
                    canvas.drawCircle(cx, cy, radius + 4f * density, hintPaint)
                }
            }
        }
    }
}

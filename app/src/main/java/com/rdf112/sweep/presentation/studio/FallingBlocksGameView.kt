package com.rdf112.sweep.presentation.studio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/**
 * Classic Falling Blocks puzzle view (rotate & stack falling shapes against rising speed levels).
 * Runs 100% offline with touch-swipe gestures and big button controls.
 */
class FallingBlocksGameView(context: Context) : View(context) {

    private val cols = 10
    private val rows = 18
    private val prefs = context.getSharedPreferences("midiastore_global_hub", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())

    private val board = Array(rows) { IntArray(cols) { 0 } }

    private val pieceColors = intArrayOf(
        Color.TRANSPARENT,
        Color.parseColor("#00E5FF"), // I - Cyan
        Color.parseColor("#FFD54F"), // O - Gold
        Color.parseColor("#C084FC"), // T - Purple
        Color.parseColor("#00E676"), // S - Emerald
        Color.parseColor("#FF5252"), // Z - Red
        Color.parseColor("#38BDF8"), // J - Sky Blue
        Color.parseColor("#FB923C"), // L - Orange
    )

    private val baseShapes: List<List<Pair<Int, Int>>> = listOf(
        listOf(-1 to 0, 0 to 0, 1 to 0, 2 to 0),   // 1: I
        listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1),     // 2: O
        listOf(-1 to 0, 0 to 0, 1 to 0, 0 to 1),    // 3: T
        listOf(0 to 0, 1 to 0, -1 to 1, 0 to 1),    // 4: S
        listOf(-1 to 0, 0 to 0, 0 to 1, 1 to 1),    // 5: Z
        listOf(-1 to 0, 0 to 0, 1 to 0, 1 to 1),    // 6: J
        listOf(-1 to 0, 0 to 0, 1 to 0, -1 to 1),   // 7: L
    )

    private var activeType = 1
    private var activeCells: List<Pair<Int, Int>> = baseShapes[0]
    private var pieceX = 4
    private var pieceY = 0
    private var score = 0
    private var linesCleared = 0
    private var level = 1
    private var highScore = prefs.getInt("blocks_high_score_v1", 0)
    private var isGameOver = false
    private var isPaused = false

    private var downX = 0f
    private var downY = 0f

    var isGoldVipTheme: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var onStateChanged: ((score: Int, lines: Int, level: Int, highScore: Int, gameOver: Boolean, paused: Boolean) -> Unit)? = null

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#081018") }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#132030")
        strokeWidth = 1.5f
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(180, 7, 11, 20) }
    private val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!isPaused && !isGameOver) {
                stepDown()
                val delayMs = (520L - (level - 1) * 42L).coerceAtLeast(135L)
                handler.postDelayed(this, delayMs)
            }
        }
    }

    init {
        isFocusable = true
        resetGame(startLoop = false)
    }

    fun startNewGame() = resetGame(startLoop = true)

    fun stopLoop() {
        handler.removeCallbacks(tickRunnable)
    }

    fun togglePause(): Boolean {
        if (isGameOver) {
            resetGame(startLoop = true)
            return false
        }
        isPaused = !isPaused
        handler.removeCallbacks(tickRunnable)
        if (!isPaused) handler.postDelayed(tickRunnable, 220L)
        notifyState()
        invalidate()
        return isPaused
    }

    fun moveLeft() {
        if (isPaused || isGameOver) return
        if (canPlace(activeCells, pieceX - 1, pieceY)) {
            pieceX--
            invalidate()
        }
    }

    fun moveRight() {
        if (isPaused || isGameOver) return
        if (canPlace(activeCells, pieceX + 1, pieceY)) {
            pieceX++
            invalidate()
        }
    }

    fun rotateClockwise() {
        if (isPaused || isGameOver || activeType == 2) return
        val rotated = activeCells.map { (cx, cy) -> -cy to cx }
        for (kick in intArrayOf(0, -1, 1, -2, 2)) {
            if (canPlace(rotated, pieceX + kick, pieceY)) {
                activeCells = rotated
                pieceX += kick
                invalidate()
                return
            }
        }
    }

    fun softDrop() {
        if (isPaused || isGameOver) return
        stepDown()
    }

    fun hardDrop() {
        if (isPaused || isGameOver) return
        while (canPlace(activeCells, pieceX, pieceY + 1)) {
            pieceY++
            score += 2
        }
        lockAndAdvance()
        updateHighScoreIfNeeded()
        notifyState()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        stopLoop()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec).coerceAtLeast(200)
        val desiredH = (w * rows / cols.toFloat()).toInt()
        val maxH = MeasureSpec.getSize(heightMeasureSpec)
        val finalH = if (maxH > 0) min(desiredH, maxH) else desiredH
        val finalW = (finalH * cols / rows.toFloat()).toInt()
        setMeasuredDimension(finalW.coerceAtLeast(200), finalH.coerceAtLeast(360))
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (abs(dx) < 20f && abs(dy) < 20f) {
                    rotateClockwise()
                    performClick()
                } else if (abs(dx) > abs(dy)) {
                    if (dx > 0) moveRight() else moveLeft()
                } else if (dy > 25f) {
                    softDrop()
                } else {
                    rotateClockwise()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun resetGame(startLoop: Boolean) {
        handler.removeCallbacks(tickRunnable)
        for (r in 0 until rows) board[r].fill(0)
        score = 0
        linesCleared = 0
        level = 1
        isGameOver = false
        isPaused = false
        spawnNextPiece()
        notifyState()
        invalidate()
        if (startLoop) {
            handler.postDelayed(tickRunnable, 480L)
        }
    }

    private fun spawnNextPiece() {
        val idx = Random.nextInt(baseShapes.size)
        activeType = idx + 1
        activeCells = baseShapes[idx]
        pieceX = cols / 2 - 1
        pieceY = 0
        if (!canPlace(activeCells, pieceX, pieceY)) {
            isGameOver = true
        }
    }

    private fun stepDown() {
        if (canPlace(activeCells, pieceX, pieceY + 1)) {
            pieceY++
        } else {
            lockAndAdvance()
        }
        notifyState()
        invalidate()
    }

    private fun lockAndAdvance() {
        for ((cx, cy) in activeCells) {
            val bx = pieceX + cx
            val by = pieceY + cy
            if (by in 0 until rows && bx in 0 until cols) {
                board[by][bx] = activeType
            }
        }
        var clearedNow = 0
        var r = rows - 1
        while (r >= 0) {
            if (board[r].all { it != 0 }) {
                clearedNow++
                for (moveRow in r downTo 1) {
                    board[moveRow] = board[moveRow - 1].copyOf()
                }
                board[0] = IntArray(cols) { 0 }
            } else {
                r--
            }
        }
        if (clearedNow > 0) {
            linesCleared += clearedNow
            score += when (clearedNow) {
                1 -> 100 * level
                2 -> 300 * level
                3 -> 500 * level
                else -> 800 * level
            }
            level = (linesCleared / 4) + 1
            updateHighScoreIfNeeded()
        }
        spawnNextPiece()
    }

    private fun updateHighScoreIfNeeded() {
        if (score > highScore) {
            highScore = score
            prefs.edit().putInt("blocks_high_score_v1", highScore).apply()
        }
    }

    private fun canPlace(cells: List<Pair<Int, Int>>, originX: Int, originY: Int): Boolean {
        for ((cx, cy) in cells) {
            val bx = originX + cx
            val by = originY + cy
            if (bx !in 0 until cols || by < 0 || by >= rows) return false
            if (board[by][bx] != 0) return false
        }
        return true
    }

    private fun notifyState() {
        onStateChanged?.invoke(score, linesCleared, level, highScore, isGameOver, isPaused)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cellSize = min(width / cols.toFloat(), height / rows.toFloat()).coerceAtLeast(1f)
        val totalW = cellSize * cols
        val totalH = cellSize * rows

        bgPaint.color = if (isGoldVipTheme) Color.parseColor("#140F04") else Color.parseColor("#081018")
        canvas.drawRoundRect(RectF(0f, 0f, totalW, totalH), 18f, 18f, bgPaint)

        gridPaint.color = if (isGoldVipTheme) Color.parseColor("#2A2009") else Color.parseColor("#132030")
        for (c in 1 until cols) {
            val x = c * cellSize
            canvas.drawLine(x, 0f, x, totalH, gridPaint)
        }
        for (r in 1 until rows) {
            val y = r * cellSize
            canvas.drawLine(0f, y, totalW, y, gridPaint)
        }

        fun drawBlock(gx: Int, gy: Int, colorType: Int) {
            if (colorType !in 1 until pieceColors.size) return
            cellPaint.color = pieceColors[colorType]
            val inset = cellSize * 0.08f
            val rect = RectF(
                gx * cellSize + inset,
                gy * cellSize + inset,
                (gx + 1) * cellSize - inset,
                (gy + 1) * cellSize - inset,
            )
            canvas.drawRoundRect(rect, cellSize * 0.2f, cellSize * 0.2f, cellPaint)
        }

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cell = board[r][c]
                if (cell != 0) drawBlock(c, r, cell)
            }
        }
        if (!isGameOver) {
            for ((cx, cy) in activeCells) {
                drawBlock(pieceX + cx, pieceY + cy, activeType)
            }
        }

        borderPaint.color = if (isGoldVipTheme) Color.parseColor("#FFB300") else Color.parseColor("#38BDF8")
        canvas.drawRoundRect(RectF(3f, 3f, totalW - 3f, totalH - 3f), 16f, 16f, borderPaint)

        if (isGameOver || isPaused) {
            canvas.drawRoundRect(RectF(0f, 0f, totalW, totalH), 18f, 18f, overlayPaint)
            bannerPaint.textSize = (totalW * 0.11f).coerceAtLeast(22f)
            canvas.drawText(
                if (isGameOver) "GAME OVER" else "PAUSED",
                totalW / 2f,
                totalH / 2f,
                bannerPaint,
            )
        }
    }
}

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
 * Retro Nokia-inspired Snake Canvas View running 100% offline.
 * Supports both touch-swipe controls and explicit directional buttons for easy senior accessibility.
 */
class SnakeGameView(context: Context) : View(context) {

    enum class Direction { UP, DOWN, LEFT, RIGHT }

    private data class Cell(val x: Int, val y: Int)

    private val gridSize = 16
    private val prefs = context.getSharedPreferences("midiastore_global_hub", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())

    private var snake = ArrayDeque<Cell>()
    private var currentDir = Direction.RIGHT
    private var nextDir = Direction.RIGHT
    private var food = Cell(10, 8)
    private var bonusFood: Cell? = null
    private var bonusTicksLeft = 0
    private var score = 0
    private var highScore = prefs.getInt("snake_high_score_v1", 0)
    private var isGameOver = false
    private var isPaused = false
    private var turboSpeed = false

    private var touchDownX = 0f
    private var touchDownY = 0f

    var isGoldVipTheme: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var onStateChanged: ((score: Int, highScore: Int, gameOver: Boolean, paused: Boolean) -> Unit)? = null

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#081018") }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#132030")
        strokeWidth = 1.5f
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val headPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#070B14") }
    private val foodPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF5252") }
    private val bonusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD54F") }
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(175, 7, 11, 20) }
    private val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!isPaused && !isGameOver) {
                stepSimulation()
                val baseDelay = if (turboSpeed) 105L else 175L
                val speedUp = (score * 2L).coerceAtMost(55L)
                handler.postDelayed(this, (baseDelay - speedUp).coerceAtLeast(80L))
            }
        }
    }

    init {
        isFocusable = true
        resetGame(startLoop = false)
    }

    fun startNewGame() {
        resetGame(startLoop = true)
    }

    fun togglePause(): Boolean {
        if (isGameOver) {
            resetGame(startLoop = true)
            return false
        }
        isPaused = !isPaused
        handler.removeCallbacks(tickRunnable)
        if (!isPaused) {
            handler.postDelayed(tickRunnable, 150L)
        }
        notifyListener()
        invalidate()
        return isPaused
    }

    fun setTurboSpeed(turbo: Boolean) {
        turboSpeed = turbo
    }

    fun steer(target: Direction) {
        if (isGameOver) return
        val opposite = when (currentDir) {
            Direction.UP -> Direction.DOWN
            Direction.DOWN -> Direction.UP
            Direction.LEFT -> Direction.RIGHT
            Direction.RIGHT -> Direction.LEFT
        }
        if (target != opposite) {
            nextDir = target
        }
    }

    fun stopLoop() {
        handler.removeCallbacks(tickRunnable)
    }

    override fun onDetachedFromWindow() {
        stopLoop()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val side = if (height > 0) min(width, height) else width
        val safeSide = side.coerceAtLeast(320)
        setMeasuredDimension(safeSide, safeSide)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - touchDownX
                val dy = event.y - touchDownY
                if (abs(dx) > 24f || abs(dy) > 24f) {
                    if (abs(dx) > abs(dy)) {
                        steer(if (dx > 0) Direction.RIGHT else Direction.LEFT)
                    } else {
                        steer(if (dy > 0) Direction.DOWN else Direction.UP)
                    }
                } else {
                    performClick()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun resetGame(startLoop: Boolean) {
        handler.removeCallbacks(tickRunnable)
        snake.clear()
        snake.addFirst(Cell(6, 8))
        snake.addFirst(Cell(7, 8))
        snake.addFirst(Cell(8, 8))
        currentDir = Direction.RIGHT
        nextDir = Direction.RIGHT
        score = 0
        isGameOver = false
        isPaused = false
        bonusFood = null
        bonusTicksLeft = 0
        spawnFood()
        notifyListener()
        invalidate()
        if (startLoop) {
            handler.postDelayed(tickRunnable, if (turboSpeed) 120L else 190L)
        }
    }

    private fun stepSimulation() {
        currentDir = nextDir
        val head = snake.first()
        val nextX = (head.x + when (currentDir) {
            Direction.LEFT -> -1
            Direction.RIGHT -> 1
            else -> 0
        } + gridSize) % gridSize
        val nextY = (head.y + when (currentDir) {
            Direction.UP -> -1
            Direction.DOWN -> 1
            else -> 0
        } + gridSize) % gridSize
        val nextHead = Cell(nextX, nextY)

        val ateRegular = nextHead == food
        val ateBonus = bonusFood != null && nextHead == bonusFood
        val tailSafeBody = if (ateRegular || ateBonus) snake else snake.dropLast(1)
        if (tailSafeBody.contains(nextHead)) {
            isGameOver = true
            notifyListener()
            invalidate()
            return
        }

        snake.addFirst(nextHead)
        if (ateRegular) {
            score += 1
            if (score > highScore) {
                highScore = score
                prefs.edit().putInt("snake_high_score_v1", highScore).apply()
            }
            spawnFood()
            if (score % 5 == 0 && bonusFood == null) {
                bonusFood = findFreeCell()
                bonusTicksLeft = 32
            }
        } else if (ateBonus) {
            score += 3
            if (score > highScore) {
                highScore = score
                prefs.edit().putInt("snake_high_score_v1", highScore).apply()
            }
            bonusFood = null
            bonusTicksLeft = 0
        } else {
            snake.removeLast()
        }

        if (bonusFood != null) {
            bonusTicksLeft--
            if (bonusTicksLeft <= 0) {
                bonusFood = null
            }
        }
        notifyListener()
        invalidate()
    }

    private fun spawnFood() {
        food = findFreeCell()
    }

    private fun findFreeCell(): Cell {
        val occupied = snake.toHashSet()
        for (attempt in 0 until 80) {
            val candidate = Cell(Random.nextInt(gridSize), Random.nextInt(gridSize))
            if (!occupied.contains(candidate) && candidate != food) return candidate
        }
        for (y in 0 until gridSize) {
            for (x in 0 until gridSize) {
                val c = Cell(x, y)
                if (!occupied.contains(c)) return c
            }
        }
        return Cell(0, 0)
    }

    private fun notifyListener() {
        onStateChanged?.invoke(score, highScore, isGameOver, isPaused)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val boardSize = min(width, height).toFloat().coerceAtLeast(1f)
        val cellSize = boardSize / gridSize.toFloat()

        bgPaint.color = if (isGoldVipTheme) Color.parseColor("#140F04") else Color.parseColor("#081018")
        canvas.drawRoundRect(RectF(0f, 0f, boardSize, boardSize), 24f, 24f, bgPaint)

        gridPaint.color = if (isGoldVipTheme) Color.parseColor("#2A2009") else Color.parseColor("#132030")
        for (i in 1 until gridSize) {
            val p = i * cellSize
            canvas.drawLine(p, 0f, p, boardSize, gridPaint)
            canvas.drawLine(0f, p, boardSize, p, gridPaint)
        }

        // Regular Apple
        val fx = (food.x + 0.5f) * cellSize
        val fy = (food.y + 0.5f) * cellSize
        canvas.drawCircle(fx, fy, cellSize * 0.36f, foodPaint)

        // Bonus Golden Fruit
        bonusFood?.let { bf ->
            val bx = (bf.x + 0.5f) * cellSize
            val by = (bf.y + 0.5f) * cellSize
            canvas.drawCircle(bx, by, cellSize * 0.40f, bonusPaint)
        }

        headPaint.color = if (isGoldVipTheme) Color.parseColor("#FFD54F") else Color.parseColor("#00E5B0")
        bodyPaint.color = if (isGoldVipTheme) Color.parseColor("#D97706") else Color.parseColor("#10B981")

        snake.forEachIndexed { idx, cell ->
            val inset = if (idx == 0) cellSize * 0.08f else cellSize * 0.14f
            val rect = RectF(
                cell.x * cellSize + inset,
                cell.y * cellSize + inset,
                (cell.x + 1) * cellSize - inset,
                (cell.y + 1) * cellSize - inset,
            )
            val radius = cellSize * 0.28f
            canvas.drawRoundRect(rect, radius, radius, if (idx == 0) headPaint else bodyPaint)
            if (idx == 0) {
                val r = cellSize * 0.08f
                canvas.drawCircle(rect.centerX() - cellSize * 0.14f, rect.centerY() - cellSize * 0.10f, r, eyePaint)
                canvas.drawCircle(rect.centerX() + cellSize * 0.14f, rect.centerY() - cellSize * 0.10f, r, eyePaint)
            }
        }

        borderPaint.color = if (isGoldVipTheme) Color.parseColor("#FFB300") else Color.parseColor("#00E5B0")
        canvas.drawRoundRect(RectF(3f, 3f, boardSize - 3f, boardSize - 3f), 22f, 22f, borderPaint)

        if (isGameOver || isPaused) {
            canvas.drawRoundRect(RectF(0f, 0f, boardSize, boardSize), 24f, 24f, overlayPaint)
            bannerPaint.textSize = (boardSize * 0.085f).coerceAtLeast(24f)
            canvas.drawText(
                if (isGameOver) "GAME OVER" else "PAUSED",
                boardSize / 2f,
                boardSize / 2f,
                bannerPaint,
            )
        }
    }
}

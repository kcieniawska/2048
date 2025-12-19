package com.example.a2048

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min
import kotlin.random.Random

class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    lateinit var manager: GameManager
    private var size = 4
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var tileSize = 0f
    private var gap = 16f
    private val cornerRadius = 24f

    private var animating = false
    private var animationStartTime: Long = 0
    private val animationDuration = 100L
    private val particles = mutableListOf<Confetti>()

    private data class Confetti(
        var x: Float, var y: Float, var size: Float,
        var speedY: Float, var speedX: Float,
        var rotation: Float, val color: Int
    ) {
        fun update() { y += speedY; x += speedX; rotation += 5f }
    }

    fun init(manager: GameManager) {
        this.manager = manager
        this.size = manager.size
        invalidate()
    }

    fun drawBoard() {
        animating = true
        animationStartTime = System.currentTimeMillis()
        invalidate()
    }

    fun triggerConfetti() {
        particles.clear()
        val colors = intArrayOf(Color.YELLOW, Color.RED, Color.CYAN, Color.GREEN, Color.MAGENTA)
        for (i in 0 until 100) {
            particles.add(Confetti(
                Random.nextFloat() * width, -50f, Random.nextFloat() * 20f + 10f,
                Random.nextFloat() * 15f + 5f, (Random.nextFloat() - 0.5f) * 10f,
                Random.nextFloat() * 360f, colors.random()
            ))
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!::manager.isInitialized) return

        val bSize = min(width, height).toFloat()
        tileSize = (bSize - gap * (size + 1)) / size
        val offsetX = (width - bSize) / 2
        val offsetY = (height - bSize) / 2

        val elapsed = System.currentTimeMillis() - animationStartTime
        val fraction = if (animating) min(1f, elapsed.toFloat() / animationDuration) else 1f

        // Tło
        paint.color = Color.parseColor("#1A1A1A")
        rect.set(offsetX, offsetY, offsetX + bSize, offsetY + bSize)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        // Kafelki
        for (i in 0 until size) {
            for (j in 0 until size) {
                val tile = manager.board[i][j]
                val tLeft = offsetX + gap + j * (tileSize + gap)
                val tTop = offsetY + gap + i * (tileSize + gap)

                // Tło kafelka
                paint.color = Color.parseColor("#3A3A3A")
                rect.set(tLeft, tTop, tLeft + tileSize, tTop + tileSize)
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

                if (tile.value != 0) {
                    var cLeft = tLeft
                    var cTop = tTop

                    if (tile.previousRow != -1 && tile.previousCol != -1) {
                        val pLeft = offsetX + gap + tile.previousCol * (tileSize + gap)
                        val pTop = offsetY + gap + tile.previousRow * (tileSize + gap)
                        cLeft = pLeft + (tLeft - pLeft) * fraction
                        cTop = pTop + (tTop - pTop) * fraction
                    }

                    paint.color = getTileColor(tile.value)
                    rect.set(cLeft, cTop, cLeft + tileSize, cTop + tileSize)
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

                    paint.color = Color.WHITE
                    paint.textSize = tileSize / 2.5f
                    paint.textAlign = Paint.Align.CENTER
                    val yText = cTop + tileSize / 2 - (paint.descent() + paint.ascent()) / 2
                    canvas.drawText(tile.value.toString(), cLeft + tileSize / 2, yText, paint)
                }
            }
        }

        if (animating && fraction < 1f) postInvalidateOnAnimation() else animating = false

        if (particles.isNotEmpty()) {
            val it = particles.iterator()
            while (it.hasNext()) {
                val p = it.next()
                paint.color = p.color
                canvas.save()
                canvas.rotate(p.rotation, p.x, p.y)
                canvas.drawRect(p.x, p.y, p.x + p.size, p.y + p.size/2, paint)
                canvas.restore()
                p.update()
                if (p.y > height) it.remove()
            }
            invalidate()
        }
    }

    private fun getTileColor(v: Int): Int = when (v) {
        2 -> Color.parseColor("#d597f0")
        4 -> Color.parseColor("#b547e6")
        8 -> Color.parseColor("#9f22d6")
        16 -> Color.parseColor("#6b0d94")
        32 -> Color.parseColor("#4f0f6e")
        64 -> Color.parseColor("#2e0540")
        128 -> Color.parseColor("#16021f")
        256 -> Color.parseColor("#862ef2")
        512 -> Color.parseColor("#720cf0")
        1024 -> Color.parseColor("#6811d4")
        2048 -> Color.parseColor("#fc5e03")
        else -> Color.BLACK
    }
}
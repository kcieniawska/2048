package com.example.a2048

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var size = 4
    lateinit var manager: GameManager

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val cornerRadius = 24f
    private var tileSize = 0f
    private var gap = 16f

    fun init(manager: GameManager) {
        this.manager = manager
        this.size = manager.size
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Dobieramy tileSize tak, żeby cała plansza mieściła się w mniejszym wymiarze
        tileSize = (min(width, height) - gap * (size + 1)) / size
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Obliczamy wymiary planszy w oparciu o tileSize
        val boardWidth = tileSize * size + gap * (size + 1)
        val boardHeight = tileSize * size + gap * (size + 1)
        val offsetX = (width - boardWidth) / 2
        val offsetY = (height - boardHeight) / 2

        // Tło planszy
        paint.color = Color.parseColor("#1A1A1A") // ciemniejsze tło
        rect.set(offsetX, offsetY, offsetX + boardWidth, offsetY + boardHeight)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        // Rysowanie kafelków
        for (i in 0 until size) {
            for (j in 0 until size) {
                val tile = manager.board[i][j]
                val left = offsetX + gap + j * (tileSize + gap)
                val top = offsetY + gap + i * (tileSize + gap)
                rect.set(left, top, left + tileSize, top + tileSize)

                paint.color = getTileColor(tile.value)
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

                if (tile.value != 0) {
                    paint.color = Color.WHITE
                    paint.textAlign = Paint.Align.CENTER

                    // Automatyczne skalowanie tekstu
                    paint.textSize = when {
                        tile.value < 1024 -> tileSize / 2.5f
                        tile.value < 16384 -> tileSize / 3f
                        else -> tileSize / 3.5f
                    }

                    val x = left + tileSize / 2
                    val y = top + tileSize / 2 - (paint.descent() + paint.ascent()) / 2
                    canvas.drawText(tile.value.toString(), x, y, paint)
                }
            }
        }
    }

    private fun getTileColor(value: Int): Int {
        return when (value) {
            0 -> Color.parseColor("#3A3A3A")       // puste kafelki
            2 -> Color.parseColor("#d597f0")
            4 -> Color.parseColor("#b547e6")
            8 -> Color.parseColor("#9f22d6")
            16 -> Color.parseColor("#6b0d94")
            32 -> Color.parseColor("#4f086e")
            64 -> Color.parseColor("#2e0540")
            128 -> Color.parseColor("#16021f")
            256 -> Color.parseColor("#862ef2")
            512 -> Color.parseColor("#720cf0")
            1024 -> Color.parseColor("#6811d4")
            2048 -> Color.parseColor("#3e1470")
            4096 -> Color.parseColor("#270a4a")
            8192 -> Color.parseColor("#150529")
            16384 -> Color.parseColor("#0b0314")
            else -> Color.parseColor("#000000")  // wartości większe niż 16384
        }
    }

    fun drawBoard() {
        invalidate()
    }
}

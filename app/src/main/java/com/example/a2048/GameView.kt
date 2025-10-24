package com.example.a2048

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min
import kotlin.math.max

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

    // ==== Pola dla animacji ====
    private var animating = false
    private var animationStartTime: Long = 0
    private val animationDuration = 100L // 100 ms na animację
    // ===========================

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

        // Czas animacji
        val elapsed = System.currentTimeMillis() - animationStartTime
        val fraction = if (animating) min(1f, elapsed.toFloat() / animationDuration) else 1f

        if (animating && fraction < 1.0f) {
            // Animacja trwa - wymuś kolejne przerysowanie
            postInvalidateOnAnimation()
        } else if (animating) {
            // Animacja zakończona
            animating = false
        }

        // Tło planszy
        paint.color = Color.parseColor("#1A1A1A") // ciemniejsze tło
        rect.set(offsetX, offsetY, offsetX + boardWidth, offsetY + boardHeight)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        // Rysowanie kafelków
        for (i in 0 until size) {
            for (j in 0 until size) {
                val tile = manager.board[i][j]

                // === Logika rysowania z uwzględnieniem animacji ===
                val targetLeft = offsetX + gap + j * (tileSize + gap)
                val targetTop = offsetY + gap + i * (tileSize + gap)

                var currentLeft = targetLeft
                var currentTop = targetTop

                // Tylko kafelki, które mają poprzednią pozycję, animujemy
                if (tile.previousRow != -1 && tile.previousCol != -1) {
                    val prevLeft = offsetX + gap + tile.previousCol * (tileSize + gap)
                    val prevTop = offsetY + gap + tile.previousRow * (tileSize + gap)

                    // Interpolacja pozycji
                    currentLeft = prevLeft + (targetLeft - prevLeft) * fraction
                    currentTop = prevTop + (targetTop - prevTop) * fraction
                }

                // Rysowanie tła (puste pole)
                paint.color = getTileColor(0)
                rect.set(targetLeft, targetTop, targetLeft + tileSize, targetTop + tileSize)
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

                // Rysowanie aktywnego kafelka
                if (tile.value != 0) {
                    // Animacja pojawiania się nowych kafelków (skalowanie)
                    var scale = 1f
                    if (tile.previousRow == -1 && tile.previousCol == -1 && fraction < 1f) {
                        // Kafelek jest nowy (nie miał poprzedniej pozycji)
                        scale = fraction
                    }

                    // W trakcie animacji przesunięcia
                    if (fraction < 1f) {
                        // Jeśli kafelek brał udział w połączeniu, jego wartość jest już docelowa,
                        // ale my musimy narysować 2 kafelki, które się łączą, w ich poprzednich pozycjach.
                        // Upustka: Nie implementujemy animacji "połączenia" (rysując 2 w jednym miejscu) -
                        // rysujemy tylko kafelek docelowy, a w GameView kafelek, który został połączony,
                        // po prostu zanika (jest nadpisywany przez tło). To uproszczenie jest wystarczające.

                        // Poza: Jeśli kafelek docelowy to 4096 (wynik połączenia 2048+2048), a logika zablokowała
                        // to połączenie, to kafelek się nie animuje, tylko pozostaje na miejscu.

                    }

                    // Rysowanie kafelka na aktualnej pozycji
                    val tS = tileSize * scale
                    val cL = currentLeft + (tileSize - tS) / 2
                    val cT = currentTop + (tileSize - tS) / 2

                    rect.set(cL, cT, cL + tS, cT + tS)
                    paint.color = getTileColor(tile.value)
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

                    if (tile.value != 0) {
                        // Rysowanie tekstu
                        paint.color = Color.WHITE
                        paint.textAlign = Paint.Align.CENTER

                        // Automatyczne skalowanie tekstu
                        paint.textSize = when {
                            tile.value < 1024 -> tileSize / 2.5f
                            tile.value < 16384 -> tileSize / 3f
                            else -> tileSize / 3.5f
                        }

                        val x = cL + tS / 2
                        val y = cT + tS / 2 - (paint.descent() + paint.ascent()) / 2
                        canvas.drawText(tile.value.toString(), x, y, paint)
                    }
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
            32 -> Color.parseColor("#4f0f6e")
            64 -> Color.parseColor("#2e0540")
            128 -> Color.parseColor("#16021f")
            256 -> Color.parseColor("#862ef2")
            512 -> Color.parseColor("#720cf0")
            1024 -> Color.parseColor("#6811d4")
            2048 -> Color.parseColor("#fc5e03")
            else -> Color.parseColor("#000000")  // Wartości większe niż 2048
        }
    }

    fun drawBoard() {
        animating = true
        animationStartTime = System.currentTimeMillis()
        invalidate()
    }
}
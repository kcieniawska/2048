package com.example.a2048

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.min

class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var size = 4
    lateinit var manager: GameManager
    var isAnimating = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val cornerRadius = 16f
    private var tileSize = 0f
    private var gap = 16f
    private lateinit var tileOffsets: Array<Array<PointF>>

    // Bitmapy kotów wczytane z zasobów
    private val catBitmapsOriginal = mutableMapOf<Int, Bitmap>()
    private val catBitmapsScaled = mutableMapOf<Int, Bitmap>()

    fun init(manager: GameManager) {
        this.manager = manager
        this.size = manager.size
        tileOffsets = Array(size) { Array(size) { PointF(0f, 0f) } }
        loadCatBitmaps()
        invalidate()
    }

    private fun loadCatBitmaps() {
        val ids = listOf(2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048)
        ids.forEach { value ->
            val resId = resources.getIdentifier("cat$value", "drawable", context.packageName)
            if (resId != 0) {
                val bmp = BitmapFactory.decodeResource(resources, resId)
                catBitmapsOriginal[value] = bmp
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        tileSize = (min(width, height) - gap * (size + 1)) / size

        // Skalujemy bitmapy kotów proporcjonalnie do tileSize
        catBitmapsOriginal.forEach { (value, bmp) ->
            val scale = min(tileSize / bmp.width, tileSize / bmp.height)
            val w = (bmp.width * scale).toInt()
            val h = (bmp.height * scale).toInt()
            catBitmapsScaled[value] = Bitmap.createScaledBitmap(bmp, w, h, true)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Tło planszy
        paint.color = Color.parseColor("#BBADA0")
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), cornerRadius, cornerRadius, paint)

        for (i in 0 until size) {
            for (j in 0 until size) {
                val tile = manager.board[i][j]
                val left = gap + j * (tileSize + gap) + tileOffsets[i][j].x
                val top = gap + i * (tileSize + gap) + tileOffsets[i][j].y
                rect.set(left, top, left + tileSize, top + tileSize)

                // Tło kafelka
                paint.color = Color.parseColor("#EEE4DA")
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

                // Rysowanie kota proporcjonalnie i wycentrowanego
                val bitmap = catBitmapsScaled[tile.value]
                bitmap?.let {
                    val offsetX = left + (tileSize - it.width) / 2
                    val offsetY = top + (tileSize - it.height) / 2
                    canvas.drawBitmap(it, offsetX, offsetY, paint)
                }

                // Numer kafelka
                if (tile.value != 0) {
                    paint.color = Color.BLACK
                    paint.textSize = tileSize / 3
                    paint.textAlign = Paint.Align.CENTER
                    val x = left + tileSize / 2
                    val y = top + tileSize / 2 - (paint.descent() + paint.ascent()) / 2
                    canvas.drawText(tile.value.toString(), x, y, paint)
                }
            }
        }
    }

    // Prosta animacja przesunięcia jednego kafelka
    fun animateTileMovement(fromI: Int, fromJ: Int, toI: Int, toJ: Int, onEnd: (() -> Unit)? = null) {
        isAnimating = true
        val startX = (fromJ - toJ) * (tileSize + gap)
        val startY = (fromI - toI) * (tileSize + gap)
        tileOffsets[fromI][fromJ].x = startX
        tileOffsets[fromI][fromJ].y = startY

        val animator = ValueAnimator.ofFloat(1f, 0f)
        animator.duration = 150
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { anim ->
            val fraction = anim.animatedValue as Float
            tileOffsets[fromI][fromJ].x = startX * fraction
            tileOffsets[fromI][fromJ].y = startY * fraction
            invalidate()
        }

        animator.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationEnd(animation: android.animation.Animator) {
                tileOffsets[fromI][fromJ].x = 0f
                tileOffsets[fromI][fromJ].y = 0f
                isAnimating = false
                onEnd?.invoke()
            }

            override fun onAnimationCancel(animation: android.animation.Animator) {
                tileOffsets[fromI][fromJ].x = 0f
                tileOffsets[fromI][fromJ].y = 0f
                isAnimating = false
            }

            override fun onAnimationRepeat(animation: android.animation.Animator) {}
        })
        animator.start()
    }
}

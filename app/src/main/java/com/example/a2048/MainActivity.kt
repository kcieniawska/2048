package com.example.a2048

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var manager: GameManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        manager = GameManager()
        gameView = findViewById(R.id.gameView)
        gameView.init(manager)

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if(gameView.isAnimating) return true // blokada ruchu podczas animacji
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                var moved = false
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD)
                        moved = if (diffX>0) manager.moveRight() else manager.moveLeft()
                } else {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD)
                        moved = if (diffY>0) manager.moveDown() else manager.moveUp()
                }
                if(moved){
                    gameView.invalidate()
                    if(manager.isGameOver()){
                        Toast.makeText(this@MainActivity,"Game Over!", Toast.LENGTH_SHORT).show()
                    }
                }
                return true
            }
        })

        gameView.setOnTouchListener{ _, event ->
            if(!gameView.isAnimating) gestureDetector.onTouchEvent(event)
            true
        }
    }
}

package com.example.a2048

import android.graphics.Color
import kotlin.random.Random

data class Confetti(
    var x: Float,
    var y: Float,
    var size: Float,
    var speedY: Float,
    var speedX: Float,
    var color: Int = Color.rgb(Random.nextInt(255), Random.nextInt(255), Random.nextInt(255)),
    var rotation: Float = Random.nextFloat() * 360f
) {
    fun update() {
        y += speedY
        x += speedX
        rotation += 5f
    }
}
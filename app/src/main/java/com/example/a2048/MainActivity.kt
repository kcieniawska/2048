package com.example.a2048

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var manager: GameManager
    private lateinit var btnRestart: Button
    private lateinit var tvScore: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var rvScores: RecyclerView
    private lateinit var layoutAuthor: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameView = findViewById(R.id.gameView)
        btnRestart = findViewById(R.id.btnRestart)
        tvScore = findViewById(R.id.tvScore)
        tabLayout = findViewById(R.id.tabLayout)
        rvScores = findViewById(R.id.rvScores)
        layoutAuthor = findViewById(R.id.layoutAuthor)

        manager = GameManager()
        gameView.init(manager)

        updateScoreText()
        setupTabs()
        setupGestures()

        btnRestart.setOnClickListener {
            manager.reset()
            gameView.drawBoard()
            updateScoreText()
        }
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Gra"))
        tabLayout.addTab(tabLayout.newTab().setText("Wyniki"))
        tabLayout.addTab(tabLayout.newTab().setText("Autor"))

        showTab("Gra")

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showTab("Gra")
                    1 -> showTab("Wyniki")
                    2 -> showTab("Autor")
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showTab(name: String) {
        when (name) {
            "Gra" -> {
                gameView.visibility = android.view.View.VISIBLE
                rvScores.visibility = android.view.View.GONE
                layoutAuthor.visibility = android.view.View.GONE
                btnRestart.visibility = android.view.View.VISIBLE
                tvScore.visibility = android.view.View.VISIBLE
            }
            "Wyniki" -> {
                gameView.visibility = android.view.View.GONE
                rvScores.visibility = android.view.View.VISIBLE
                layoutAuthor.visibility = android.view.View.GONE
                btnRestart.visibility = android.view.View.GONE
                tvScore.visibility = android.view.View.GONE
                showScoresTab()
            }
            "Autor" -> {
                gameView.visibility = android.view.View.GONE
                rvScores.visibility = android.view.View.GONE
                layoutAuthor.visibility = android.view.View.VISIBLE
                btnRestart.visibility = android.view.View.GONE
                tvScore.visibility = android.view.View.GONE
            }
        }
    }

    private fun showScoresTab() {
        val scores = loadScoresForToday().take(10)
            .mapIndexed { index, score -> ScoreItem(index + 1, score) }
        rvScores.layoutManager = LinearLayoutManager(this)
        rvScores.adapter = ScoreAdapter(scores)
    }

    private fun setupGestures() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                var moved = false

                if (kotlin.math.abs(diffX) > kotlin.math.abs(diffY)) {
                    if (kotlin.math.abs(diffX) > SWIPE_THRESHOLD && kotlin.math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        moved = if (diffX > 0) manager.moveRight() else manager.moveLeft()
                    }
                } else {
                    if (kotlin.math.abs(diffY) > SWIPE_THRESHOLD && kotlin.math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        moved = if (diffY > 0) manager.moveDown() else manager.moveUp()
                    }
                }

                if (moved) {
                    gameView.drawBoard()
                    updateScoreText()
                    if (manager.isGameOver()) showGameOverDialog()
                }
                return true
            }
        })

        gameView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun updateScoreText() {
        tvScore.text = "Wynik: ${manager.score}"
    }

    private fun showGameOverDialog() {
        saveScore()
        AlertDialog.Builder(this)
            .setTitle("Koniec gry 😿")
            .setMessage("Nie ma już możliwych ruchów!")
            .setPositiveButton("Zacznij od nowa") { _, _ ->
                manager.reset()
                gameView.drawBoard()
                updateScoreText()
            }
            .setNegativeButton("Wyjdź z gry") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun loadScoresForToday(): List<Int> {
        val prefs = getSharedPreferences("scores", MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val allScores = prefs.getStringSet(today, emptySet()) ?: emptySet()
        return allScores.mapNotNull { it.toIntOrNull() }.sortedDescending()
    }

    private fun saveScore() {
        val prefs = getSharedPreferences("scores", MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val scoresSet = prefs.getStringSet(today, emptySet())?.toMutableSet() ?: mutableSetOf()
        scoresSet.add(manager.score.toString())
        prefs.edit().putStringSet(today, scoresSet).apply()
    }
}

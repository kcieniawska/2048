package com.example.a2048

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
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

    // Easter Egg
    private lateinit var easterEggContainer: LinearLayout
    private lateinit var hiddenImage: ImageView
    private lateinit var hiddenText: TextView
    private var titleClickCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicjalizacja widoków
        gameView = findViewById(R.id.gameView)
        easterEggContainer = findViewById(R.id.easterEggContainer)
        hiddenImage = findViewById(R.id.hiddenImage)
        hiddenText = findViewById(R.id.hiddenText)
        btnRestart = findViewById(R.id.btnRestart)
        tvScore = findViewById(R.id.tvScore)
        tabLayout = findViewById(R.id.tabLayout)
        rvScores = findViewById(R.id.rvScores)
        layoutAuthor = findViewById(R.id.layoutAuthor)
        val tvTitle: TextView = findViewById(R.id.tvTitle)

        manager = GameManager()
        gameView.init(manager)

        updateScoreText()
        setupTabs()
        setupGestures()

        // Restart gry
        btnRestart.setOnClickListener {
            manager.reset()
            gameView.drawBoard()
            updateScoreText()
        }

        // Easter Egg – kliknięcie w tytuł
        tvTitle.setOnClickListener {
            titleClickCount++
            if (titleClickCount >= 2) { // dwukrotne kliknięcie
                titleClickCount = 0
                toggleEasterEgg()
            }
        }
    }

    // Funkcja toggleEasterEgg – pokazuje/ukrywa kontener easter egga z animacją
    private fun toggleEasterEgg() {
        if (easterEggContainer.visibility == View.GONE) {
            // Pokaż kontener
            hiddenImage.visibility = View.VISIBLE
            hiddenText.visibility = View.VISIBLE

            easterEggContainer.alpha = 0f
            easterEggContainer.visibility = View.VISIBLE
            easterEggContainer.animate()
                .alpha(1f)
                .setDuration(500)
                .start()

            // Animacja skalowania obrazka
            hiddenImage.scaleX = 0.5f
            hiddenImage.scaleY = 0.5f
            hiddenImage.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .start()

            // Ukryj planszę i przyciski
            gameView.animate().alpha(0f).setDuration(500).withEndAction {
                gameView.visibility = View.GONE
                gameView.alpha = 1f
            }.start()

            btnRestart.animate().alpha(0f).setDuration(500).withEndAction {
                btnRestart.visibility = View.GONE
                btnRestart.alpha = 1f
            }.start()

            tvScore.animate().alpha(0f).setDuration(500).withEndAction {
                tvScore.visibility = View.GONE
                tvScore.alpha = 1f
            }.start()

        } else {
            // Ukryj kontener easter egga
            easterEggContainer.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction {
                    easterEggContainer.visibility = View.GONE
                    hiddenImage.visibility = View.GONE
                    hiddenText.visibility = View.GONE
                    easterEggContainer.alpha = 1f
                }.start()

            // Przywróć planszę i przyciski
            gameView.alpha = 0f
            gameView.visibility = View.VISIBLE
            gameView.animate().alpha(1f).setDuration(500).start()

            btnRestart.alpha = 0f
            btnRestart.visibility = View.VISIBLE
            btnRestart.animate().alpha(1f).setDuration(500).start()

            tvScore.alpha = 0f
            tvScore.visibility = View.VISIBLE
            tvScore.animate().alpha(1f).setDuration(500).start()
        }
    }

    // Zakładki
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
                gameView.visibility = View.VISIBLE
                easterEggContainer.visibility = View.GONE
                rvScores.visibility = View.GONE
                layoutAuthor.visibility = View.GONE
                btnRestart.visibility = View.VISIBLE
                tvScore.visibility = View.VISIBLE
            }
            "Wyniki" -> {
                gameView.visibility = View.GONE
                easterEggContainer.visibility = View.GONE
                rvScores.visibility = View.VISIBLE
                layoutAuthor.visibility = View.GONE
                btnRestart.visibility = View.GONE
                tvScore.visibility = View.GONE
                showScoresTab()
            }
            "Autor" -> {
                gameView.visibility = View.GONE
                easterEggContainer.visibility = View.GONE
                rvScores.visibility = View.GONE
                layoutAuthor.visibility = View.VISIBLE
                btnRestart.visibility = View.GONE
                tvScore.visibility = View.GONE
            }
        }
    }

    private fun showScoresTab() {
        val scores = loadScoresForToday().take(10)
            .mapIndexed { index, score -> ScoreItem(index + 1, score) }
        rvScores.layoutManager = LinearLayoutManager(this)
        rvScores.adapter = ScoreAdapter(scores)
    }

    // Gesty do przesuwania
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

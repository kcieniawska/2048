package com.example.a2048

import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var manager: GameManager
    private lateinit var btnRestart: Button
    private lateinit var tvScore: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var rvScores: RecyclerView
    private lateinit var layoutScores: LinearLayout
    private lateinit var layoutAuthor: LinearLayout
    private lateinit var tvNoScores: TextView

    private lateinit var easterEggContainer: LinearLayout
    private lateinit var hiddenImage: ImageView
    private lateinit var hiddenText: TextView
    private var titleClickCount = 0

    private val PREFS_NAME = "scores_prefs"
    private val KEY_SCORES = "scores_json"

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
        layoutScores = findViewById(R.id.layoutScores)
        layoutAuthor = findViewById(R.id.layoutAuthor)
        tvNoScores = findViewById(R.id.tvNoScores)
        val tvTitle: TextView = findViewById(R.id.tvTitle)

        manager = GameManager()
        gameView.init(manager)

        updateScoreText()
        setupTabs()
        setupGestures()

        btnRestart.setOnClickListener {
            if (manager.score > 0) saveScore()
            manager.reset()
            gameView.drawBoard()
            updateScoreText()
        }

        tvTitle.setOnClickListener {
            titleClickCount++
            if (titleClickCount >= 2) {
                titleClickCount = 0
                toggleEasterEgg()
            }
        }
    }

    private fun toggleEasterEgg() {
        if (easterEggContainer.visibility == View.GONE) {
            easterEggContainer.visibility = View.VISIBLE
            hiddenImage.visibility = View.VISIBLE
            hiddenText.visibility = View.VISIBLE
            easterEggContainer.alpha = 0f
            easterEggContainer.animate().alpha(1f).setDuration(500).start()
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
            easterEggContainer.animate().alpha(0f).setDuration(500).withEndAction {
                easterEggContainer.visibility = View.GONE
                hiddenImage.visibility = View.GONE
                hiddenText.visibility = View.GONE
                easterEggContainer.alpha = 1f
            }.start()
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
                layoutScores.visibility = View.GONE
                tvNoScores.visibility = View.GONE
                layoutAuthor.visibility = View.GONE
                btnRestart.visibility = View.VISIBLE
                tvScore.visibility = View.VISIBLE
            }

            "Wyniki" -> {
                gameView.visibility = View.GONE
                easterEggContainer.visibility = View.GONE
                layoutAuthor.visibility = View.GONE
                btnRestart.visibility = View.GONE
                tvScore.visibility = View.GONE
                showScoresTab()
            }

            "Autor" -> {
                gameView.visibility = View.GONE
                easterEggContainer.visibility = View.GONE
                layoutScores.visibility = View.GONE
                tvNoScores.visibility = View.GONE
                layoutAuthor.visibility = View.VISIBLE
                btnRestart.visibility = View.GONE
                tvScore.visibility = View.GONE
            }
        }
    }

    private fun showScoresTab() {
        val scores = loadScores()
            .sortedByDescending { it.score }
            .mapIndexed { index, stored -> ScoreItem(index + 1, stored.score) }

        if (scores.isEmpty()) {
            layoutScores.visibility = View.GONE
            tvNoScores.visibility = View.VISIBLE
        } else {
            layoutScores.visibility = View.VISIBLE
            tvNoScores.visibility = View.GONE
            rvScores.layoutManager = LinearLayoutManager(this)
            rvScores.adapter = ScoreAdapter(scores)
        }
    }

    private fun setupGestures() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
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
                showScoresTab() // odświeżamy wyniki
            }
            .setNegativeButton("Wyjdź z gry") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun saveScore() {
        try {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val json = prefs.getString(KEY_SCORES, "[]")
            val type = object : TypeToken<MutableList<StoredScore>>() {}.type
            val list: MutableList<StoredScore> = Gson().fromJson(json, type) ?: mutableListOf()

            list.add(StoredScore(manager.score))

            prefs.edit().putString(KEY_SCORES, Gson().toJson(list)).apply()
        } catch (e: Exception) {
            Log.e("SCORE_DEBUG", "Błąd zapisu wyniku: ${e.message}", e)
        }
    }

    private fun loadScores(): List<StoredScore> {
        return try {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val json = prefs.getString(KEY_SCORES, "[]")
            val type = object : TypeToken<List<StoredScore>>() {}.type
            Gson().fromJson<List<StoredScore>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("SCORE_DEBUG", "Błąd odczytu wyników: ${e.message}", e)
            emptyList()
        }
    }
}

// Model wyników
data class StoredScore(val score: Int)

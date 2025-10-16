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
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private lateinit var gameView: GameView
    private lateinit var manager: GameManager
    private lateinit var btnRestart: Button
    private lateinit var tvScore: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var rvScores: RecyclerView
    private lateinit var layoutAuthor: LinearLayout
    private lateinit var tvNoScores: TextView

    private lateinit var easterEggContainer: LinearLayout
    private lateinit var hiddenImage: ImageView
    private lateinit var hiddenText: TextView
    private var titleClickCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // findViewById - zabezpieczenie: jeśli któryś view nie istnieje, logujemy i kończymy init
        try {
            gameView = findViewById(R.id.gameView)
            easterEggContainer = findViewById(R.id.easterEggContainer)
            hiddenImage = findViewById(R.id.hiddenImage)
            hiddenText = findViewById(R.id.hiddenText)
            btnRestart = findViewById(R.id.btnRestart)
            tvScore = findViewById(R.id.tvScore)
            tabLayout = findViewById(R.id.tabLayout)
            rvScores = findViewById(R.id.rvScores)
            layoutAuthor = findViewById(R.id.layoutAuthor)
            tvNoScores = findViewById(R.id.tvNoScores)
        } catch (e: Exception) {
            Log.e(TAG, "Brak niektórych widoków w layout: ${e.message}", e)
            Toast.makeText(this, "Błąd layoutu: brak widoku. Sprawdź activity_main.xml", Toast.LENGTH_LONG).show()
            return
        }

        manager = GameManager()
        gameView.init(manager)

        // przygotuj RecyclerView raz (bez adaptera na start)
        rvScores.layoutManager = LinearLayoutManager(this)
        rvScores.setHasFixedSize(true)

        updateScoreText()
        setupTabs()
        setupGestures()

        btnRestart.setOnClickListener {
            manager.reset()
            gameView.drawBoard()
            updateScoreText()
        }

        val tvTitle: TextView = findViewById(R.id.tvTitle)
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
            hiddenImage.visibility = View.VISIBLE
            hiddenText.visibility = View.VISIBLE
            easterEggContainer.alpha = 0f
            easterEggContainer.visibility = View.VISIBLE
            easterEggContainer.animate().alpha(1f).setDuration(350).start()
            hiddenImage.scaleX = 0.6f
            hiddenImage.scaleY = 0.6f
            hiddenImage.animate().scaleX(1f).scaleY(1f).setDuration(350).start()
            // ukryj planszę i kontrolki
            gameView.visibility = View.GONE
            btnRestart.visibility = View.GONE
            tvScore.visibility = View.GONE
        } else {
            easterEggContainer.visibility = View.GONE
            hiddenImage.visibility = View.GONE
            hiddenText.visibility = View.GONE
            gameView.visibility = View.VISIBLE
            btnRestart.visibility = View.VISIBLE
            tvScore.visibility = View.VISIBLE
        }
    }

    private fun setupTabs() {
        tabLayout.removeAllTabs()
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
                showScoresTabSafe()
            }
            "Autor" -> {
                gameView.visibility = View.GONE
                easterEggContainer.visibility = View.GONE
                rvScores.visibility = View.GONE
                tvNoScores.visibility = View.GONE
                layoutAuthor.visibility = View.VISIBLE
                btnRestart.visibility = View.GONE
                tvScore.visibility = View.GONE
            }
        }
    }

    // Bezpieczna wersja showScoresTab - złapie wyjątki i zaloguje
    private fun showScoresTabSafe() {
        try {
            val raw = loadScoresForToday()
            val scores = raw.take(10).mapIndexed { index, score -> ScoreItem(index + 1, score) }
            // zawsze ustaw adapter (nawet pusty) — brak NPE
            rvScores.adapter = ScoreAdapter(scores)
            if (scores.isEmpty()) {
                rvScores.visibility = View.GONE
                tvNoScores.visibility = View.VISIBLE
            } else {
                rvScores.visibility = View.VISIBLE
                tvNoScores.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas ładowania wyników: ${e.message}", e)
            Toast.makeText(this, "Nie udało się załadować wyników (sprawdź logcat).", Toast.LENGTH_LONG).show()
            // pokaż pustą listę zamiast crasha
            rvScores.adapter = ScoreAdapter(emptyList())
            rvScores.visibility = View.GONE
            tvNoScores.visibility = View.VISIBLE
        }
    }

    private fun setupGestures() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                var moved = false
                try {
                    if (kotlin.math.abs(diffX) > kotlin.math.abs(diffY)) {
                        if (kotlin.math.abs(diffX) > SWIPE_THRESHOLD && kotlin.math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            moved = if (diffX > 0) manager.moveRight() else manager.moveLeft()
                        }
                    } else {
                        if (kotlin.math.abs(diffY) > SWIPE_THRESHOLD && kotlin.math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                            moved = if (diffY > 0) manager.moveDown() else manager.moveUp()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Błąd gestu: ${e.message}", e)
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
        try {
            tvScore.text = "Wynik: ${manager.score}"
        } catch (e: Exception) {
            Log.e(TAG, "Błąd updateScoreText: ${e.message}", e)
        }
    }

    private fun showGameOverDialog() {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Błąd dialogu końca gry: ${e.message}", e)
        }
    }

    private fun saveScore() {
        try {
            val prefs = getSharedPreferences("scores", MODE_PRIVATE)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val allScoresString = prefs.getString(today, "") ?: ""
            val scores = allScoresString.split(",").filter { it.isNotEmpty() }.toMutableList()
            scores.add(manager.score.toString())
            prefs.edit().putString(today, scores.joinToString(",")).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Błąd saveScore: ${e.message}", e)
        }
    }

    private fun loadScoresForToday(): List<Int> {
        return try {
            val prefs = getSharedPreferences("scores", MODE_PRIVATE)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val allScoresString = prefs.getString(today, "") ?: ""
            allScoresString.split(",")
                .mapNotNull { it.toIntOrNull() }
                .sortedDescending()
        } catch (e: Exception) {
            Log.e(TAG, "Błąd loadScoresForToday: ${e.message}", e)
            emptyList()
        }
    }
}

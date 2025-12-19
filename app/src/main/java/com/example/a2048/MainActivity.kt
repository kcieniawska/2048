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
import android.text.Html
import android.text.method.LinkMovementMethod
import kotlin.math.abs
import android.os.Build

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
    private lateinit var btnChangelog: Button
    private lateinit var scrollChangelog: ScrollView
    private lateinit var tvChangelog: TextView
    private lateinit var btnUndo: Button
    private lateinit var easterEggContainer: LinearLayout
    private lateinit var hiddenImage: ImageView
    private lateinit var hiddenText: TextView
    private lateinit var tvTitle: TextView
    private var titleClickCount = 0

    private val PREFS_NAME = "scores_prefs"
    private val KEY_SCORES = "scores_json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()

        manager = GameManager()
        gameView.init(manager)

        updateScoreText()
        setupTabs()
        setupGestures()
        setupChangelog()
        setupTitleInteractions()

        btnRestart.setOnClickListener {
            if (manager.score > 0) saveScore()
            manager.reset()
            gameView.drawBoard()
            updateScoreText()
            btnUndo.isEnabled = false
            showTab("Gra")
        }

        btnUndo.setOnClickListener {
            if (manager.undo()) {
                gameView.drawBoard()
                updateScoreText()
                btnUndo.isEnabled = false
                Toast.makeText(this, "Ruch cofnięty!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initViews() {
        gameView = findViewById(R.id.gameView)
        tvScore = findViewById(R.id.tvScore)
        btnRestart = findViewById(R.id.btnRestart)
        btnUndo = findViewById(R.id.btnUndo)
        tabLayout = findViewById(R.id.tabLayout)
        rvScores = findViewById(R.id.rvScores)
        layoutScores = findViewById(R.id.layoutScores)
        layoutAuthor = findViewById(R.id.layoutAuthor)
        tvNoScores = findViewById(R.id.tvNoScores)
        btnChangelog = findViewById(R.id.btnChangelog)
        scrollChangelog = findViewById(R.id.scrollChangelog)
        tvChangelog = findViewById(R.id.tvChangelog)
        easterEggContainer = findViewById(R.id.easterEggContainer)
        hiddenImage = findViewById(R.id.hiddenImage)
        hiddenText = findViewById(R.id.hiddenText)
        tvTitle = findViewById(R.id.tvTitle)
        btnUndo.isEnabled = false
    }

    private fun setupTitleInteractions() {
        // 1. CHEAT: Długie przytrzymanie tytułu
        tvTitle.setOnLongClickListener {
            manager.setCheatTiles()
            gameView.drawBoard()
            updateScoreText()
            Toast.makeText(this, "Cheat activated! Połącz 1024.", Toast.LENGTH_SHORT).show()
            true
        }

        // 2. EASTER EGG: Kliknięcie 2 razy
        tvTitle.setOnClickListener {
            titleClickCount++
            if (titleClickCount >= 2) {
                titleClickCount = 0
                toggleEasterEgg()
            }
        }
    }

    private fun toggleEasterEgg() {
        val isEggHidden = easterEggContainer.visibility == View.GONE
        if (isEggHidden) {
            hiddenImage.setImageResource(R.drawable.easter_egg_image)
            hiddenText.text = "Pozdrowienia z Podhala! 🏔️"
            easterEggContainer.bringToFront()
            easterEggContainer.visibility = View.VISIBLE
            easterEggContainer.alpha = 0f
            easterEggContainer.animate().alpha(1f).setDuration(500).start()

            gameView.visibility = View.GONE
            btnRestart.visibility = View.GONE
            tvScore.visibility = View.GONE
            btnUndo.visibility = View.GONE
            tabLayout.visibility = View.GONE
        } else {
            easterEggContainer.visibility = View.GONE
            tabLayout.visibility = View.VISIBLE
            showTab("Gra")
        }
    }

    private fun setupGestures() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 == null) return false
                val dx = e2.x - e1.x
                val dy = e2.y - e1.y
                var moved = false
                if (abs(dx) > abs(dy)) {
                    if (abs(dx) > 100 && abs(vx) > 100) moved = if (dx > 0) manager.moveRight() else manager.moveLeft()
                } else {
                    if (abs(dy) > 100 && abs(vy) > 100) moved = if (dy > 0) manager.moveDown() else manager.moveUp()
                }
                if (moved) {
                    gameView.drawBoard()
                    updateScoreText()
                    btnUndo.isEnabled = true
                    if (manager.reached2048) show2048ReachedDialog()
                    if (manager.isGameOver()) showGameOverDialog()
                }
                return true
            }
        })
        gameView.setOnTouchListener { v, ev ->
            gestureDetector.onTouchEvent(ev)
            v.performClick()
            true
        }
    }

    private fun show2048ReachedDialog() {
        gameView.triggerConfetti()
        manager.resetReached2048()
        val view = layoutInflater.inflate(R.layout.dialog_win_2048, null)
        val dialog = AlertDialog.Builder(this).setView(view).setCancelable(false).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setDimAmount(0.8f)

        view.findViewById<Button>(R.id.btnContinue).setOnClickListener { dialog.dismiss() }
        view.findViewById<Button>(R.id.btnNewGame).setOnClickListener {
            manager.reset(); gameView.drawBoard(); updateScoreText(); dialog.dismiss()
        }
        dialog.show()
    }

    // NAPRAWIONE: Nowy popup Końca Gry
    private fun showGameOverDialog() {
        if (manager.score > 0) saveScore()

        val dialogView = layoutInflater.inflate(R.layout.dialog_game_over, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setDimAmount(0.85f)

        // Ustawienie wyniku
        dialogView.findViewById<TextView>(R.id.tvFinalScore).text = "Twój wynik: ${manager.score}"

        // Przycisk RESTART
        dialogView.findViewById<Button>(R.id.btnTryAgain).setOnClickListener {
            manager.reset()
            gameView.drawBoard()
            updateScoreText()
            btnUndo.isEnabled = false
            showTab("Gra")
            dialog.dismiss()
        }

        // Przycisk WYJŚCIE
        dialogView.findViewById<Button>(R.id.btnExit).setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun updateScoreText() { tvScore.text = "Wynik: ${manager.score}" }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Gra"))
        tabLayout.addTab(tabLayout.newTab().setText("Wyniki"))
        tabLayout.addTab(tabLayout.newTab().setText("Autor"))
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
        val isGra = name == "Gra"
        gameView.visibility = if (isGra) View.VISIBLE else View.GONE
        layoutScores.visibility = if (name == "Wyniki") View.VISIBLE else View.GONE
        layoutAuthor.visibility = if (name == "Autor") View.VISIBLE else View.GONE
        btnRestart.visibility = if (isGra) View.VISIBLE else View.GONE
        tvScore.visibility = if (isGra) View.VISIBLE else View.GONE
        btnUndo.visibility = if (isGra) View.VISIBLE else View.GONE
        tabLayout.visibility = View.VISIBLE
        if (name == "Wyniki") showScoresTab()
        if (!isGra) { easterEggContainer.visibility = View.GONE; scrollChangelog.visibility = View.GONE }
    }

    private fun showScoresTab() {
        val scores = loadScores().sortedByDescending { it.score }.mapIndexed { i, s -> ScoreItem(i + 1, s.score) }
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

    private fun setupChangelog() {
        val txt = "<b>Wersja 1.4</b>: Nowe animacje i konfetti!"
        tvChangelog.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(txt, Html.FROM_HTML_MODE_LEGACY) else Html.fromHtml(txt)
        btnChangelog.setOnClickListener {
            scrollChangelog.visibility = if (scrollChangelog.visibility == View.GONE) View.VISIBLE else View.GONE
        }
    }

    private fun saveScore() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val json = prefs.getString(KEY_SCORES, "[]")
        val list: MutableList<StoredScore> = Gson().fromJson(json, object : TypeToken<MutableList<StoredScore>>() {}.type) ?: mutableListOf()
        list.add(StoredScore(manager.score))
        prefs.edit().putString(KEY_SCORES, Gson().toJson(list)).apply()
    }

    private fun loadScores(): List<StoredScore> =
        Gson().fromJson(getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_SCORES, "[]"),
            object : TypeToken<List<StoredScore>>() {}.type) ?: emptyList()
}

data class StoredScore(val score: Int)
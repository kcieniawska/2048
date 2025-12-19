package com.example.a2048

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import android.os.Build
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    // Widoki gry
    private lateinit var gameView: GameView
    private lateinit var manager: GameManager
    private lateinit var tvScore: TextView
    private lateinit var tvTitle: TextView

    // Przyciski i kontenery
    private lateinit var btnRestart: Button
    private lateinit var btnUndo: Button
    private lateinit var controlsContainer: LinearLayout

    // Layouty zakładek
    private lateinit var tabLayout: TabLayout
    private lateinit var layoutScores: LinearLayout
    private lateinit var layoutInfo: LinearLayout
    private lateinit var layoutAuthor: LinearLayout
    private lateinit var easterEggContainer: LinearLayout

    // Elementy treści
    private lateinit var rvScores: RecyclerView
    private lateinit var tvNoScores: TextView
    private lateinit var tvChangelog: TextView
    private lateinit var hiddenImage: ImageView
    private lateinit var hiddenText: TextView

    // Nowe elementy sekcji Autor
    private lateinit var btnUniversity: Button
    private lateinit var ivAuthorPhoto: ImageView

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

        // Obsługa restartu
        btnRestart.setOnClickListener {
            if (manager.score > 0) saveScore()
            manager.reset()
            gameView.drawBoard()
            updateScoreText()
            btnUndo.isEnabled = false
            showTab("Gra")
        }

        // Obsługa cofania
        btnUndo.setOnClickListener {
            if (manager.undo()) {
                gameView.drawBoard()
                updateScoreText()
                btnUndo.isEnabled = false
                Toast.makeText(this, "Ruch cofnięty! ↩️", Toast.LENGTH_SHORT).show()
            }
        }

        // OBSŁUGA PRZYCISKU GITHUB W AUTORZE
        btnUniversity.setOnClickListener {
            val url = "https://github.com/kcieniawska/"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
    }

    private fun initViews() {
        // Podstawowe
        gameView = findViewById(R.id.gameView)
        tvScore = findViewById(R.id.tvScore)
        tvTitle = findViewById(R.id.tvTitle)
        btnRestart = findViewById(R.id.btnRestart)
        btnUndo = findViewById(R.id.btnUndo)
        controlsContainer = findViewById(R.id.controlsContainer)

        // Zakładki i kontenery
        tabLayout = findViewById(R.id.tabLayout)
        layoutScores = findViewById(R.id.layoutScores)
        layoutInfo = findViewById(R.id.layoutInfo)
        layoutAuthor = findViewById(R.id.layoutAuthor)
        easterEggContainer = findViewById(R.id.easterEggContainer)

        // Treść
        rvScores = findViewById(R.id.rvScores)
        tvNoScores = findViewById(R.id.tvNoScores)
        tvChangelog = findViewById(R.id.tvChangelog)
        hiddenImage = findViewById(R.id.hiddenImage)
        hiddenText = findViewById(R.id.hiddenText)

        // Autor
        btnUniversity = findViewById(R.id.btnUniversity)
        ivAuthorPhoto = findViewById(R.id.ivAuthorPhoto)

        btnUndo.isEnabled = false
    }

    private fun setupTitleInteractions() {
        tvTitle.setOnClickListener {
            titleClickCount++
            if (titleClickCount >= 2) {
                titleClickCount = 0
                toggleEasterEgg()
            }
        }

        tvTitle.setOnLongClickListener {
            manager.setCheatTiles()
            gameView.drawBoard()
            updateScoreText()
            Toast.makeText(this, "Cheat aktywowany! 🐏", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Gra"))
        tabLayout.addTab(tabLayout.newTab().setText("Wyniki"))
        tabLayout.addTab(tabLayout.newTab().setText("Nowości"))
        tabLayout.addTab(tabLayout.newTab().setText("Autor"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showTab("Gra")
                    1 -> showTab("Wyniki")
                    2 -> showTab("Nowości")
                    3 -> showTab("Autor")
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
        layoutInfo.visibility = if (name == "Nowości") View.VISIBLE else View.GONE
        layoutAuthor.visibility = if (name == "Autor") View.VISIBLE else View.GONE

        controlsContainer.visibility = if (isGra) View.VISIBLE else View.GONE
        tvScore.visibility = if (isGra) View.VISIBLE else View.GONE
        tabLayout.visibility = View.VISIBLE

        if (name == "Wyniki") showScoresTab()
        if (name != "Gra") easterEggContainer.visibility = View.GONE
    }

    private fun toggleEasterEgg() {
        val isEggHidden = easterEggContainer.visibility == View.GONE
        if (isEggHidden) {
            easterEggContainer.bringToFront()
            easterEggContainer.visibility = View.VISIBLE
            gameView.visibility = View.GONE
            controlsContainer.visibility = View.GONE
        } else {
            showTab("Gra")
        }
    }

    private fun setupChangelog() {
        val txt = """
        <b>WIELKA AKTUALIZACJA | Wersja 1.5 - 19.12.2025</b><br/>
        • NOWY layout wszystkich zakładek<br/>
        • NOWY wyglad easter egga (2x klik na tytuł gry)<br/>
        • NOWA animacja i NOWY wygląd dla komunikatów<br/>
        • NOWE logo aplikacji<br/>
        • Optymalizacja płynności gestów i zakładek<br/>
        
        <br/>
        
        <b>Wersja 1.4 - 29.10.2025</b><br/>
        • Dodano możliwość cofania ruchów (Undo) ↩️<br/>
        • Usprawniono rozgrywkę i logikę gry<br/>
        • Dodano animację dla kafelka 2048 🎊<br/>
        • Nowa kolorystyka komunikatów<br/><br/>

        <b>Wersja 1.3 - 24.10.2025</b><br/>
        • Zlikwidowano błędy i zoptymalizowano animacje<br/>
        • Zmiana zasady gry: Maksymalna liczba to 2048<br/><br/>

        <b>Wersja 1.2 - 17.10.2025</b><br/>
        • Naprawiono zapis wyników<br/>
        • Dodano sekcję "Co nowego w aplikacji?"<br/>
        • Dodano prosty Easter Egg 🏔️<br/><br/>

        <b>Wersja 1.1 - 16.10.2025</b><br/>
        • Zlikwidowano błędy i usprawniono rozgrywkę<br/>
        • Odświeżenie wyglądu interfejsu<br/><br/>

        <b>Wersja 1.0 - 10.10.2025</b><br/>
        • Pierwsza wersja gry 2048
    """.trimIndent()

        tvChangelog.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(txt, Html.FROM_HTML_MODE_LEGACY) else Html.fromHtml(txt)
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
        gameView.setOnTouchListener { v, ev -> gestureDetector.onTouchEvent(ev); v.performClick(); true }
    }

    private fun show2048ReachedDialog() {
        gameView.triggerConfetti()
        manager.resetReached2048()
        val view = layoutInflater.inflate(R.layout.dialog_win_2048, null)
        val dialog = AlertDialog.Builder(this).setView(view).setCancelable(false).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        view.findViewById<Button>(R.id.btnContinue).setOnClickListener { dialog.dismiss() }
        view.findViewById<Button>(R.id.btnNewGame).setOnClickListener {
            manager.reset(); gameView.drawBoard(); updateScoreText(); dialog.dismiss()
        }
        dialog.show()
    }

    private fun showGameOverDialog() {
        if (manager.score > 0) saveScore()
        val view = layoutInflater.inflate(R.layout.dialog_game_over, null)
        val dialog = AlertDialog.Builder(this).setView(view).setCancelable(false).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        view.findViewById<TextView>(R.id.tvFinalScore).text = "Twój wynik: ${manager.score}"
        view.findViewById<Button>(R.id.btnTryAgain).setOnClickListener {
            manager.reset(); gameView.drawBoard(); updateScoreText(); dialog.dismiss()
        }
        view.findViewById<Button>(R.id.btnExit).setOnClickListener { finish() }
        dialog.show()
    }

    private fun updateScoreText() { tvScore.text = "Wynik: ${manager.score}" }

    private fun showScoresTab() {
        val scores = loadScores().sortedByDescending { it.score }.mapIndexed { i, s -> ScoreItem(i + 1, s.score) }
        if (scores.isEmpty()) {
            layoutScores.visibility = View.VISIBLE
            tvNoScores.visibility = View.VISIBLE
            rvScores.visibility = View.GONE
        } else {
            layoutScores.visibility = View.VISIBLE
            tvNoScores.visibility = View.GONE
            rvScores.visibility = View.VISIBLE
            rvScores.layoutManager = LinearLayoutManager(this)
            rvScores.adapter = ScoreAdapter(scores)
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

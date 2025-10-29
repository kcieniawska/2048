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
import android.os.Build // Dodany import dla Build.VERSION.SDK_INT

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
    private var titleClickCount = 0

    private val PREFS_NAME = "scores_prefs"
    private val KEY_SCORES = "scores_json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicjalizacja widoków
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
        val tvTitle: TextView = findViewById(R.id.tvTitle)

        btnUndo.isEnabled = false

        manager = GameManager()
        gameView.init(manager)

        updateScoreText()
        setupTabs()
        setupGestures()
        setupChangelog()
        setupTitleClick(tvTitle)

        // --- OBSŁUGA PRZYCISKÓW ---
        btnRestart.setOnClickListener {
            if (manager.score > 0) saveScore()
            manager.reset()
            gameView.drawBoard()
            updateScoreText()
            btnUndo.isEnabled = false
            showTab("Gra")
        }

        // === OBSŁUGA COFANIA RUCHU ===
        btnUndo.setOnClickListener {
            if (manager.undo()) {
                gameView.drawBoard()
                updateScoreText()
                btnUndo.isEnabled = false
                Toast.makeText(this, "Ruch cofnięty!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Nie ma już ruchów do cofnięcia!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Funkcja aktywująca Easter Egg po dwukrotnym kliknięciu w tytuł
    private fun setupTitleClick(tvTitle: TextView) {
        tvTitle.setOnClickListener {
            titleClickCount++
            if (titleClickCount >= 2) {
                titleClickCount = 0
                toggleEasterEgg()
            }
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

                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        moved = if (diffX > 0) manager.moveRight() else manager.moveLeft()
                    }
                } else {
                    if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        moved = if (diffY > 0) manager.moveDown() else manager.moveUp()
                    }
                }

                if (moved) {
                    gameView.drawBoard()
                    updateScoreText()
                    btnUndo.isEnabled = true

                    if (manager.reached2048) {
                        show2048ReachedDialog()
                    }

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

    // Używamy pełnej ścieżki do stylu: androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert
    private fun show2048ReachedDialog() {
        // Reset flag 2048, używamy refleksji, bo pole jest private set
        // Dzięki temu dialog pojawi się tylko raz
        manager.javaClass.getDeclaredField("reached2048").apply {
            isAccessible = true
            setBoolean(manager, false)
        }

        AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
            .setTitle("🥳 GRATULACJE! 🥳")
            .setMessage("Dotarłeś do 2048! Lecimy dalej?")
            .setPositiveButton("KONTUNUUJ") { _, _ ->
                // Kontynuacja gry
            }
            .setNegativeButton("Zakończ grę") { _, _ ->
                showGameOverDialog()
            }
            .setCancelable(false)
            .show()
    }

    // Używamy pełnej ścieżki do stylu: androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert
    private fun showGameOverDialog() {
        if (manager.score > 0) saveScore()

        AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
            .setTitle("Koniec gry 😿")
            .setMessage("Nie ma już możliwych ruchów! Twój wynik: ${manager.score}")
            .setPositiveButton("Zacznij od nowa") { _, _ ->
                manager.reset()
                gameView.drawBoard()
                updateScoreText()
                btnUndo.isEnabled = false
                showTab("Gra")
            }
            .setNegativeButton("Wyjdź z gry") { _, _ -> finish() }
            .setCancelable(false)
            .show()
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
                btnUndo.visibility = View.VISIBLE
            }
            "Wyniki" -> {
                gameView.visibility = View.GONE
                easterEggContainer.visibility = View.GONE
                layoutAuthor.visibility = View.GONE
                btnRestart.visibility = View.GONE
                tvScore.visibility = View.GONE
                btnUndo.visibility = View.GONE
                showScoresTab()
            }
            "Autor" -> {
                gameView.visibility = View.GONE
                easterEggContainer.visibility = View.GONE
                layoutScores.visibility = View.GONE
                tvNoScores.visibility = View.GONE
                layoutAuthor.visibility = View.VISIBLE
                scrollChangelog.visibility = View.GONE
                btnRestart.visibility = View.GONE
                tvScore.visibility = View.GONE
                btnUndo.visibility = View.GONE
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

    // START POPRAWIONEJ FUNKCJI DLA KOMPATYBILNOŚCI WSTECZNEJ (API < 24)
    private fun setupChangelog() {
        val changelogText = """
<h1>CO NOWEGO?</h1>
        <p>GitHub: <a href="https://github.com/kcieniawska/2048"> kcieniawska</a></p>
        <p>Discord: <a href="https://discord.gg/PF3wAaVhEP"> 2048</a></p>
        <br>------------------------------------------------------------------<br>
        <h3>Wersja 1.4 - 29.10.2025</h3>
        <ul>
            <li>Dodano możliwość cofania ruchów</li>
            <li>Usprawniono rozgrywke</li>
            <li>Dodano animację dla zdobytego kafelka 2048</li>
            <li>Komunikaty uzyskały nowy kolor</li>
        </ul>
        <br>------------------------------------------------------------------<br>
        <h3>Wersja 1.3 - 24.10.2025</h3>
        <ul>
            <li> Zlikwidowano błędy</li>
            <li> Zoptymalizowano i naprawiono animacje gry</li>
            <li> Zmieniono zasade gry: Maksymalna liczba to 2048, a nie jak wczesniej</li>
        </ul>
<br>------------------------------------------------------------------<br>
        <h3>Wersja 1.2 - 17.10.2025</h3>
        <ul>
            <li> Naprawiono zapis wyników</li>
            <li> Zlikwidowano błędy</li>
            <li> Dodano przycisk "Co nowego w aplikacji?"</li>
            <li> Dodano prosty Easter Egg</li>
        </ul>
<br>------------------------------------------------------------------<br>
        <h3>Wersja 1.1 - 16.10.2025</h3>
        <ul>
            <li> Zlikwidowano błędy</li>
            <li> Usprawniono rozgrywkę</li>
            <li> Zmiana wyglądu gry</li>
        </ul>
<br>------------------------------------------------------------------<br>
        <h3>Wersja 1.0</h3>
        <ul>
            <li> Pierwsza wersja gry 2048</li>
        </ul>
""".trimIndent()

        // POPRAWKA BŁĘDU: Warunkowe użycie metody fromHtml dla API >= 24 i API < 24
        val styledText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // API 24 (Nougat) i nowsze: używamy nowej, bezpiecznej metody
            Html.fromHtml(changelogText, Html.FROM_HTML_MODE_LEGACY)
        } else {
            // API 23 i starsze: używamy starej, przestarzałej, ale koniecznej metody
            @Suppress("DEPRECATION")
            Html.fromHtml(changelogText)
        }

        tvChangelog.text = styledText
        tvChangelog.movementMethod = LinkMovementMethod.getInstance()
        scrollChangelog.isVerticalScrollBarEnabled = true
        scrollChangelog.isScrollbarFadingEnabled = false
        btnChangelog.setOnClickListener {
            scrollChangelog.visibility =
                if (scrollChangelog.visibility == View.GONE) View.VISIBLE else View.GONE
        }
    }
    // KONIEC POPRAWIONEJ FUNKCJI

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

data class StoredScore(val score: Int)
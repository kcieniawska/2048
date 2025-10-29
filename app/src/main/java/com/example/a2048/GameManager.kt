package com.example.a2048

import kotlin.random.Random

// Klasa do przechowywania stanu kafelka przed ruchem
data class GridState(val value: Int, val row: Int, val col: Int)

class GameManager(val size: Int = 4) {

    val board: Array<Array<Tile>> = Array(size) { Array(size) { Tile() } }
    var score: Int = 0
        private set

    // === UNDO I 2048 ===
    private var previousBoard: Array<Array<Tile>>? = null
    private var previousScore: Int = 0
    var reached2048: Boolean = false
        private set
    // ====================

    init {
        reset()
    }

    fun reset() {
        for (i in 0 until size) {
            for (j in 0 until size) {
                board[i][j].clear()
            }
        }
        score = 0
        previousBoard = null
        previousScore = 0
        reached2048 = false
        addRandomTile()
    }

    // USUNIĘTO: === FUNKCJA DLA TRYBU TESTOWEGO 1024 + 1024 ===
    /*
    /**
     * Ustawia planszę w stan 1024 + 1024, aby umożliwić szybkie wbicie 2048 jednym ruchem.
     */
    fun setCheatTiles() {
        for (i in 0 until size) {
            for (j in 0 until size) {
                board[i][j].clear()
            }
        }
        board[0][0].value = 1024
        board[0][1].value = 1024
        score = 0
    }
    // =========================================================
    */

    // Zapisuje stan kafelków (wartość, pozycja) przed ruchem
    private fun saveTilesState() {
        previousScore = score
        previousBoard = Array(size) { i ->
            Array(size) { j ->
                board[i][j].copy() // Kopiowanie kafelka (jego wartości i stanu)
            }
        }

        // Zapisuje pozycje dla animacji
        for (i in 0 until size) {
            for (j in 0 until size) {
                board[i][j].savePosition(i, j)
            }
        }
    }

    /**
     * Cofa ostatni wykonany ruch.
     * @return true, jeśli cofnięto ruch; false, jeśli nie ma stanu do cofnięcia.
     */
    fun undo(): Boolean {
        val prev = previousBoard
        if (prev != null) {
            for (i in 0 until size) {
                for (j in 0 until size) {
                    board[i][j] = prev[i][j].copy() // Przywrócenie kafelków
                }
            }
            score = previousScore
            previousBoard = null // Po cofnięciu stanu, usuwamy go
            return true
        }
        return false
    }

    // ====================================================
    //  RUCHY: moveLeft, moveRight, moveUp, moveDown
    // ====================================================
    fun moveLeft(): Boolean {
        saveTilesState()
        var moved = false
        var pointsAdded = 0
        for (i in 0 until size) {
            val (newRow, points, rowMoved, mergedIndices) = calculateRowMove(board[i].map { it.value })

            if (rowMoved) {
                moved = true
                pointsAdded += points
                // Kopiowanie nowych wartości z powrotem do planszy
                for (j in 0 until size) {
                    board[i][j].value = newRow[j]

                    // Obsługa animacji (mergedFrom)
                    if (mergedIndices.contains(j)) {
                        // Kafelek w [i][j] powstał przez połączenie
                        // Ponieważ ruch jest w lewo, kafelki łączyły się z prawym sąsiadem
                        val sourceCol = j + 1
                        board[i][j].mergedFrom = i to sourceCol
                    }
                }
            }
        }

        if (moved) {
            score += pointsAdded
            addRandomTile()
        }
        return moved
    }

    fun moveRight(): Boolean {
        saveTilesState()
        var moved = false
        var pointsAdded = 0
        for (i in 0 until size) {
            // Odwracamy wiersz, obliczamy ruch (jak dla lewego), i odwracamy z powrotem
            val reversedRowValues = board[i].map { it.value }.reversed()
            val (newReversedRow, points, rowMoved, mergedIndices) = calculateRowMove(reversedRowValues)

            if (rowMoved) {
                moved = true
                pointsAdded += points
                val newRow = newReversedRow.reversed() // Odwracamy z powrotem

                for (j in 0 until size) {
                    board[i][j].value = newRow[j]

                    // Obsługa animacji (mergedFrom)
                    if (mergedIndices.contains(size - 1 - j)) { // Indeks w odwróconym rzędzie
                        // Kafelek w [i][j] powstał przez połączenie
                        // Ponieważ ruch jest w prawo, kafelki łączyły się z lewym sąsiadem
                        val sourceCol = j - 1
                        board[i][j].mergedFrom = i to sourceCol
                    }
                }
            }
        }

        if (moved) {
            score += pointsAdded
            addRandomTile()
        }
        return moved
    }

    fun moveUp(): Boolean {
        saveTilesState()
        var moved = false
        var pointsAdded = 0

        for (j in 0 until size) { // Iteracja po kolumnach
            // Wyciągamy kolumnę
            val colValues = (0 until size).map { i -> board[i][j].value }
            val (newCol, points, colMoved, mergedIndices) = calculateRowMove(colValues)

            if (colMoved) {
                moved = true
                pointsAdded += points

                for (i in 0 until size) {
                    board[i][j].value = newCol[i]

                    // Obsługa animacji (mergedFrom)
                    if (mergedIndices.contains(i)) {
                        // Kafelek w [i][j] powstał przez połączenie
                        // Ponieważ ruch jest w górę, łączył się z dolnym sąsiadem
                        val sourceRow = i + 1
                        board[i][j].mergedFrom = sourceRow to j
                    }
                }
            }
        }

        if (moved) {
            score += pointsAdded
            addRandomTile()
        }
        return moved
    }

    fun moveDown(): Boolean {
        saveTilesState()
        var moved = false
        var pointsAdded = 0

        for (j in 0 until size) { // Iteracja po kolumnach
            // Wyciągamy kolumnę i odwracamy
            val reversedColValues = (0 until size).map { i -> board[i][j].value }.reversed()
            val (newReversedCol, points, colMoved, mergedIndices) = calculateRowMove(reversedColValues)

            if (colMoved) {
                moved = true
                pointsAdded += points
                val newCol = newReversedCol.reversed() // Odwracamy z powrotem

                for (i in 0 until size) {
                    board[i][j].value = newCol[i]

                    // Obsługa animacji (mergedFrom)
                    if (mergedIndices.contains(size - 1 - i)) { // Indeks w odwróconej kolumnie
                        // Kafelek w [i][j] powstał przez połączenie
                        // Ponieważ ruch jest w dół, łączył się z górnym sąsiadem
                        val sourceRow = i - 1
                        board[i][j].mergedFrom = sourceRow to j
                    }
                }
            }
        }

        if (moved) {
            score += pointsAdded
            addRandomTile()
        }
        return moved
    }


    /**
     * Właściwa logika ruchu dla rzędu/kolumny.
     * Działa na liście wartości (np. [2, 2, 4, 0]) i zwraca nowy stan.
     * @return Quad(nowa lista wartości, dodane punkty, czy nastąpiła zmiana, indeksy połączonych kafelków)
     */
    private fun calculateRowMove(originalRow: List<Int>): Quad<List<Int>, Int, Boolean, List<Int>> {
        val original = originalRow.toList()

        // 1. Kompresja (usuwanie zer)
        val filtered = original.filter { it != 0 }.toMutableList()
        var points = 0
        val mergedIndices = mutableListOf<Int>()
        var i = 0
        var filteredIndex = 0

        // 2. Łączenie
        while (i < filtered.size - 1) {
            if (filtered[i] == filtered[i + 1]) {
                filtered[i] *= 2
                points += filtered[i] // Dodaj punkty
                mergedIndices.add(filteredIndex) // Zapisz indeks połączonego kafelka

                // Osiągnięto 2048
                if (filtered[i] == 2048) {
                    reached2048 = true
                }

                filtered.removeAt(i + 1)
            } else {
                i++
            }
            filteredIndex++
        }

        // 3. Wypełnianie zerami
        while (filtered.size < size) filtered.add(0)

        val changed = original != filtered
        return Quad(filtered, points, changed, mergedIndices.toList())
    }


    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)


    private fun addRandomTile() {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until size)
            for (j in 0 until size)
                if (board[i][j].isEmpty()) emptyCells.add(i to j)

        if (emptyCells.isNotEmpty()) {
            val (i, j) = emptyCells.random()
            board[i][j].value = if (Random.nextFloat() < 0.9f) 2 else 4
            // Nowe kafelki nie mają poprzedniej pozycji, co zasygnalizuje, że mają się "pojawiś"
            board[i][j].previousRow = -1
            board[i][j].previousCol = -1
        }
    }

    fun isGameOver(): Boolean {
        // jeśli jest puste pole -> nie ma końca
        if (board.any { row -> row.any { it.isEmpty() } }) return false

        // sprawdź, czy jest jakieś sąsiednie równe pole (pion/poziom)
        for (i in 0 until size) {
            for (j in 0 until size) {
                val currentValue = board[i][j].value

                // Sprawdzenie sąsiada z prawej
                if (j < size - 1 && board[i][j + 1].value == currentValue) return false

                // Sprawdzenie sąsiada z dołu
                if (i < size - 1 && board[i + 1][j].value == currentValue) return false
            }
        }

        return true // Brak wolnych pól i brak możliwych połączeń
    }
}

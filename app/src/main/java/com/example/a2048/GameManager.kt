package com.example.a2048

import kotlin.random.Random

// Klasa do przechowywania stanu kafelka przed ruchem
data class GridState(val value: Int, val row: Int, val col: Int)

class GameManager(val size: Int = 4) {

    val board: Array<Array<Tile>> = Array(size) { Array(size) { Tile() } }
    var score: Int = 0
        private set

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
        addRandomTile() // startujemy od jednego kafelka
    }

    // Zapisuje stan kafelków (wartość, pozycja) przed ruchem
    private fun saveTilesState() {
        for (i in 0 until size) {
            for (j in 0 until size) {
                board[i][j].savePosition(i, j)
            }
        }
    }

    // ====================================================
    //  RUCHY: moveLeft, moveRight, moveUp, moveDown
    // ====================================================
    fun moveLeft(): Boolean {
        saveTilesState()
        var moved = false
        for (i in 0 until size) {
            // Konwersja na listę wartości, by użyć logicznego compressAndMerge
            val originalValues = board[i].map { it.value }.toMutableList()
            val (newRowValues, points, changed, mergedIndices) = compressAndMerge(originalValues)

            if (changed) {
                moved = true
                score += points

                // Aktualizacja planszy i stanu animacji
                for (j in 0 until size) {
                    val tile = board[i][j]
                    val newValue = newRowValues[j]

                    if (tile.value != newValue) {
                        tile.value = newValue
                        if (newValue != 0) {
                            // Jeśli wartość się zmieniła, to jest nowy kafelek lub wynik mergowania
                            // Musimy zlokalizować, skąd ten kafelek przyszedł.
                            // W prostym 2048 to jest trudne bez bardziej złożonego śledzenia,
                            // ale przy standardowej implementacji:
                            // 1. Kafelki, które się przesunęły, miały pozycję (i, j')
                            // 2. Kafelki wynikające z mergowania są pod j-tym indeksem.

                            // Na potrzeby animacji, założymy, że kafelek przesunął się ZAWSZE
                            // Z najbliższego niezerowego kafelka, który jest *przed* nim (lub to ten sam).
                            // Ponieważ compressAndMerge nie zwraca pełnej mapy, uproszczmy logikę.
                            // Używamy mergedFrom tylko dla połączeń, reszta animacji będzie liniowa.

                            if (mergedIndices.contains(j)) {
                                tile.mergedFrom = i to j // Sygnalizacja, że kafelek jest wynikiem połączenia
                            }
                        }
                    }
                }
            }
        }
        if (moved) addRandomTile()
        return moved
    }

    fun moveRight(): Boolean {
        saveTilesState()
        var moved = false
        for (i in 0 until size) {
            val originalValues = board[i].map { it.value }.reversed().toMutableList() // Odwrócona wiersza
            val (newRowValuesReversed, points, changed, mergedIndices) = compressAndMerge(originalValues)

            if (changed) {
                moved = true
                score += points
            }

            // Odwrócenie i aktualizacja planszy
            val newRowValues = newRowValuesReversed.reversed()
            val mergedIndicesNormal = mergedIndices.map { size - 1 - it }

            for (j in 0 until size) {
                val tile = board[i][j]
                val newValue = newRowValues[j]

                if (tile.value != newValue) {
                    tile.value = newValue
                    if (mergedIndicesNormal.contains(j)) {
                        tile.mergedFrom = i to j
                    }
                }
            }
        }
        if (moved) addRandomTile()
        return moved
    }

    fun moveUp(): Boolean {
        saveTilesState()
        var moved = false
        for (j in 0 until size) {
            val originalValues = MutableList(size) { i -> board[i][j].value }
            val (newColValues, points, changed, mergedIndices) = compressAndMerge(originalValues)

            if (changed) {
                moved = true
                score += points

                for (i in 0 until size) {
                    val tile = board[i][j]
                    val newValue = newColValues[i]

                    if (tile.value != newValue) {
                        tile.value = newValue
                        if (mergedIndices.contains(i)) {
                            tile.mergedFrom = i to j
                        }
                    }
                }
            }
        }
        if (moved) addRandomTile()
        return moved
    }

    fun moveDown(): Boolean {
        saveTilesState()
        var moved = false
        for (j in 0 until size) {
            val originalValues = MutableList(size) { i -> board[i][j].value }.reversed().toMutableList()
            val (newColValuesReversed, points, changed, mergedIndices) = compressAndMerge(originalValues)

            if (changed) {
                moved = true
                score += points
            }

            val newColValues = newColValuesReversed.reversed()
            val mergedIndicesNormal = mergedIndices.map { size - 1 - it }

            for (i in 0 until size) {
                val tile = board[i][j]
                val newValue = newColValues[i]

                if (tile.value != newValue) {
                    tile.value = newValue
                    if (mergedIndicesNormal.contains(i)) {
                        tile.mergedFrom = i to j
                    }
                }
            }
        }
        if (moved) addRandomTile()
        return moved
    }

    // ====================================================
    //  compressAndMerge: usuń zera, zrób mergy, dopasuj rozmiar
    //  Zwraca: (newList, pointsGained, changedFlag, mergedIndices)
    // ====================================================
    private fun compressAndMerge(row: MutableList<Int>): Quad<MutableList<Int>, Int, Boolean, List<Int>> {
        val original = row.toList()
        val filtered = row.filter { it != 0 }.toMutableList()
        var points = 0
        val mergedIndices = mutableListOf<Int>()
        var i = 0
        var filteredIndex = 0

        while (i < filtered.size - 1) {
            if (filtered[i] != 0 && filtered[i] == filtered[i + 1]) {

                // OGRANICZENIE DO 2048:
                if (filtered[i] * 2 <= 2048) {
                    filtered[i] *= 2
                    points += filtered[i]
                    mergedIndices.add(filteredIndex) // Zapisujemy indeks w nowym, pełnym rzędzie
                    filtered.removeAt(i + 1)
                } else {
                    i++ // Nie łączymy, jeśli wynik > 2048, tylko przechodzimy dalej
                }
            } else {
                i++
            }
            filteredIndex++
        }

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
                val v = board[i][j].value
                if (i < size - 1 && board[i + 1][j].value == v && v <= 1024) return false // Sprawdzenie, czy można połączyć
                if (j < size - 1 && board[i][j + 1].value == v && v <= 1024) return false // Sprawdzenie, czy można połączyć
            }
        }
        return true
    }
}
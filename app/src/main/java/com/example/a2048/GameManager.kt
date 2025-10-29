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
    var reached2048: Boolean = false // Wymagane, aby MainActivity mogła sprawdzać warunek 2048
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
        addRandomTile() // Start standardowej gry
    }

    // === FUNKCJA UNDO ===
    fun undo(): Boolean {
        if (previousBoard != null) {
            for (i in 0 until size) {
                for (j in 0 until size) {
                    board[i][j] = previousBoard!![i][j].copy()
                }
            }
            score = previousScore

            previousBoard = null
            previousScore = 0

            return true
        }
        return false
    }
    // ====================

    // Zapisuje stan kafelków przed ruchem
    private fun saveTilesState() {
        // Zapis stanu do undo
        previousBoard = Array(size) { i ->
            Array(size) { j ->
                board[i][j].copy().apply {
                    this.previousRow = i
                    this.previousCol = j
                    this.mergedFrom = null
                }
            }
        }
        previousScore = score

        // Zapis pozycji dla animacji
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
            val originalValues = board[i].map { it.value }.toMutableList()
            val (newRowValues, points, changed, mergedIndices) = compressAndMerge(originalValues)

            if (changed) {
                moved = true
                score += points
                for (j in 0 until size) {
                    val tile = board[i][j]
                    tile.value = newRowValues[j]
                    if (mergedIndices.contains(j)) {
                        tile.mergedFrom = i to j
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
            val originalValues = board[i].map { it.value }.reversed().toMutableList()
            val (newRowValuesReversed, points, changed, mergedIndices) = compressAndMerge(originalValues)

            if (changed) {
                moved = true
                score += points
            }

            val newRowValues = newRowValuesReversed.reversed()
            val mergedIndicesNormal = mergedIndices.map { size - 1 - it }

            for (j in 0 until size) {
                val tile = board[i][j]
                tile.value = newRowValues[j]
                if (mergedIndicesNormal.contains(j)) {
                    tile.mergedFrom = i to j
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
                    tile.value = newColValues[i]
                    if (mergedIndices.contains(i)) {
                        tile.mergedFrom = i to j
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
                tile.value = newColValues[i]
                if (mergedIndicesNormal.contains(i)) {
                    tile.mergedFrom = i to j
                }
            }
        }
        if (moved) addRandomTile()
        return moved
    }

    // Zmodyfikowana funkcja do śledzenia 2048
    private fun compressAndMerge(row: MutableList<Int>): Quad<MutableList<Int>, Int, Boolean, List<Int>> {
        val original = row.toList()
        val filtered = row.filter { it != 0 }.toMutableList()
        var points = 0
        val mergedIndices = mutableListOf<Int>()
        var i = 0
        var filteredIndex = 0

        while (i < filtered.size - 1) {
            if (filtered[i] != 0 && filtered[i] == filtered[i + 1]) {

                if (filtered[i] * 2 <= 2048) {
                    val newTileValue = filtered[i] * 2
                    filtered[i] = newTileValue
                    points += newTileValue
                    mergedIndices.add(filteredIndex)

                    if (newTileValue == 2048) {
                        reached2048 = true
                    }

                    filtered.removeAt(i + 1)
                } else {
                    i++
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
            board[i][j].previousRow = -1
            board[i][j].previousCol = -1
        }
    }

    fun isGameOver(): Boolean {
        if (board.any { row -> row.any { it.isEmpty() } }) return false

        for (i in 0 until size) {
            for (j in 0 until size) {
                val v = board[i][j].value
                if (i < size - 1 && board[i + 1][j].value == v) return false
                if (j < size - 1 && board[i][j + 1].value == v) return false
            }
        }
        return true
    }
}
package com.example.a2048

import kotlin.random.Random

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

    // ====================================================
    //  RUCHY: moveLeft, moveRight, moveUp, moveDown
    // ====================================================
    fun moveLeft(): Boolean {
        var moved = false
        for (i in 0 until size) {
            val original = board[i].map { it.value }
            val (newRow, points, changed) = compressAndMerge(original.toMutableList())
            if (changed) {
                moved = true
                score += points
            }
            for (j in 0 until size) board[i][j].value = newRow[j]
        }
        if (moved) addRandomTile()
        return moved
    }

    fun moveRight(): Boolean {
        var moved = false
        for (i in 0 until size) {
            val original = board[i].map { it.value }.reversed().toMutableList()
            val (newRow, points, changed) = compressAndMerge(original)
            if (changed) {
                moved = true
                score += points
            }
            for (j in 0 until size) board[i][size - 1 - j].value = newRow[j]
        }
        if (moved) addRandomTile()
        return moved
    }

    fun moveUp(): Boolean {
        var moved = false
        for (j in 0 until size) {
            val original = MutableList(size) { i -> board[i][j].value }
            val (newCol, points, changed) = compressAndMerge(original.toMutableList())
            if (changed) {
                moved = true
                score += points
            }
            for (i in 0 until size) board[i][j].value = newCol[i]
        }
        if (moved) addRandomTile()
        return moved
    }

    fun moveDown(): Boolean {
        var moved = false
        for (j in 0 until size) {
            val original = MutableList(size) { i -> board[i][j].value }.reversed().toMutableList()
            val (newCol, points, changed) = compressAndMerge(original)
            if (changed) {
                moved = true
                score += points
            }
            for (i in 0 until size) board[size - 1 - i][j].value = newCol[i]
        }
        if (moved) addRandomTile()
        return moved
    }

    // ====================================================
    //  compressAndMerge: usuń zera, zrób mergy, dopasuj rozmiar
    //  Zwraca: (newList, pointsGained, changedFlag)
    // ====================================================
    private fun compressAndMerge(row: MutableList<Int>): Triple<MutableList<Int>, Int, Boolean> {
        val original = row.toList()
        val filtered = row.filter { it != 0 }.toMutableList()
        var points = 0

        var i = 0
        while (i < filtered.size - 1) {
            if (filtered[i] == filtered[i + 1]) {
                filtered[i] *= 2
                points += filtered[i]
                filtered.removeAt(i + 1)
                i++ // kontynuujemy dalsze sprawdzanie
            } else {
                i++
            }
        }

        while (filtered.size < size) filtered.add(0)

        val changed = original != filtered
        return Triple(filtered, points, changed)
    }

    private fun addRandomTile() {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until size)
            for (j in 0 until size)
                if (board[i][j].isEmpty()) emptyCells.add(i to j)

        if (emptyCells.isNotEmpty()) {
            val (i, j) = emptyCells.random()
            board[i][j].value = if (Random.nextFloat() < 0.9f) 2 else 4
        }
    }

    fun isGameOver(): Boolean {
        // jeśli jest puste pole -> nie ma końca
        if (board.any { row -> row.any { it.isEmpty() } }) return false

        // sprawdź, czy jest jakieś sąsiednie równe pole (pion/poziom)
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

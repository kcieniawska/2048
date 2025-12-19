package com.example.a2048

import kotlin.random.Random

class GameManager(val size: Int = 4) {
    val board: Array<Array<Tile>> = Array(size) { Array(size) { Tile() } }
    var score: Int = 0
        private set

    private var previousBoard: Array<Array<Tile>>? = null
    private var previousScore: Int = 0
    var reached2048: Boolean = false

    init {
        reset()
    }

    /**
     * Ustawia planszę w stan 1024 + 1024, aby umożliwić szybkie zdobycie 2048.
     * Wywołaj tę metodę np. po kliknięciu w ukryty przycisk lub specjalną kombinację.
     */
    fun setCheatTiles() {
        for (i in 0 until size) {
            for (j in 0 until size) {
                board[i][j].clear()
            }
        }
        // Ustawiamy dwa kafelki gotowe do połączenia
        board[0][0].value = 1024
        board[0][1].value = 1024
        score = 0
        reached2048 = false
        // Resetujemy pozycje, aby nie było błędów w animacji startowej
        board[0][0].savePosition(0, 0)
        board[0][1].savePosition(0, 1)
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
        addRandomTile() // Start z jednym kafelkiem
    }

    fun resetReached2048() {
        reached2048 = false
    }

    private fun saveTilesState() {
        previousScore = score
        previousBoard = Array(size) { i ->
            Array(size) { j -> board[i][j].copy() }
        }
        for (i in 0 until size) {
            for (j in 0 until size) {
                board[i][j].savePosition(i, j)
            }
        }
    }

    fun undo(): Boolean {
        val prev = previousBoard ?: return false
        for (i in 0 until size) {
            for (j in 0 until size) {
                board[i][j] = prev[i][j].copy()
            }
        }
        score = previousScore
        previousBoard = null
        return true
    }

    fun moveLeft(): Boolean = move("LEFT")
    fun moveRight(): Boolean = move("RIGHT")
    fun moveUp(): Boolean = move("UP")
    fun moveDown(): Boolean = move("DOWN")

    private fun move(direction: String): Boolean {
        saveTilesState()
        var movedTotal = false
        var pointsTotal = 0

        for (i in 0 until size) {
            val original = when(direction) {
                "LEFT" -> board[i].map { it.value }
                "RIGHT" -> board[i].map { it.value }.reversed()
                "UP" -> (0 until size).map { r -> board[r][i].value }
                "DOWN" -> (0 until size).map { r -> board[r][i].value }.reversed()
                else -> listOf()
            }

            val (newValues, points, moved) = calculateRowMove(original)
            if (moved) {
                movedTotal = true
                pointsTotal += points

                val finalValues = if (direction == "RIGHT" || direction == "DOWN") {
                    newValues.reversed()
                } else {
                    newValues
                }

                for (k in 0 until size) {
                    when(direction) {
                        "LEFT", "RIGHT" -> board[i][k].value = finalValues[k]
                        "UP", "DOWN" -> board[k][i].value = finalValues[k]
                    }
                }
            }
        }

        if (movedTotal) {
            score += pointsTotal
            addRandomTile()
        }
        return movedTotal
    }

    private fun calculateRowMove(original: List<Int>): Triple<List<Int>, Int, Boolean> {
        val filtered = original.filter { it != 0 }.toMutableList()
        var pts = 0
        var i = 0
        while (i < filtered.size - 1) {
            if (filtered[i] == filtered[i+1]) {
                filtered[i] *= 2
                pts += filtered[i]
                if (filtered[i] == 2048) reached2048 = true // Wykrycie 2048
                filtered.removeAt(i+1)
            }
            i++
        }
        while (filtered.size < size) filtered.add(0)
        return Triple(filtered, pts, original != filtered)
    }

    private fun addRandomTile() {
        val empty = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until size) {
            for (j in 0 until size) {
                if (board[i][j].isEmpty()) empty.add(i to j)
            }
        }

        if (empty.isNotEmpty()) {
            val (r, c) = empty.random()
            board[r][c].value = if (Random.nextFloat() < 0.9f) 2 else 4
            board[r][c].previousRow = -1
            board[r][c].previousCol = -1
        }
    }

    fun isGameOver(): Boolean {
        if (board.any { row -> row.any { it.isEmpty() } }) return false
        for (i in 0 until size) {
            for (j in 0 until size) {
                val v = board[i][j].value
                if (j < size - 1 && board[i][j+1].value == v) return false
                if (i < size - 1 && board[i+1][j].value == v) return false
            }
        }
        return true
    }
}
package com.example.a2048

import kotlin.random.Random

class GameManager(val size: Int = 4) {
    val board = Array(size) { Array(size) { Tile() } }
    var score = 0

    init {
        addRandomTile()
    }
    class Tile(var value: Int = 0) {
        var merged = false
        fun isEmpty() = value == 0
        fun clear() { value = 0; merged = false }
    }

    fun reset() {
        for (i in 0 until size) {
            for (j in 0 until size) {
                board[i][j].clear()
            }
        }
        score = 0
        addRandomTile()
    }

    private fun emptyTiles(): List<Pair<Int, Int>> {
        val empty = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until size)
            for (j in 0 until size)
                if (board[i][j].isEmpty()) empty.add(Pair(i, j))
        return empty
    }

    fun addRandomTile() {
        val empty = emptyTiles()
        if (empty.isNotEmpty()) {
            val (i, j) = empty.random()
            board[i][j].value = if (Random.nextInt(10) < 9) 2 else 4
        }
    }

    private fun resetMergedFlags() {
        for (row in board)
            for (tile in row) tile.merged = false
    }

    fun moveLeft(): Boolean {
        var moved = false
        resetMergedFlags()
        for (i in 0 until size) {
            val row = board[i]
            for (j in 1 until size) {
                if (row[j].isEmpty()) continue
                var k = j
                while (k > 0 && row[k - 1].isEmpty()) {
                    row[k - 1].value = row[k].value
                    row[k].clear()
                    k--
                    moved = true
                }
                if (k > 0 && row[k - 1].value == row[k].value && !row[k - 1].merged && !row[k].merged) {
                    row[k - 1].value *= 2
                    row[k - 1].merged = true
                    row[k].clear()
                    score += row[k - 1].value
                    moved = true
                }
            }
        }
        if (moved) addRandomTile()
        return moved
    }

    fun moveRight(): Boolean { rotate180(); val moved = moveLeft(); rotate180(); return moved }
    fun moveUp(): Boolean { rotateLeft(); val moved = moveLeft(); rotateRight(); return moved }
    fun moveDown(): Boolean { rotateRight(); val moved = moveLeft(); rotateLeft(); return moved }

    private fun rotateLeft() {
        val newBoard = Array(size) { Array(size) { Tile() } }
        for (i in 0 until size)
            for (j in 0 until size)
                newBoard[size - j - 1][i].value = board[i][j].value
        for (i in 0 until size)
            for (j in 0 until size)
                board[i][j].value = newBoard[i][j].value
    }

    private fun rotateRight() {
        val newBoard = Array(size) { Array(size) { Tile() } }
        for (i in 0 until size)
            for (j in 0 until size)
                newBoard[j][size - i - 1].value = board[i][j].value
        for (i in 0 until size)
            for (j in 0 until size)
                board[i][j].value = newBoard[i][j].value
    }

    private fun rotate180() { rotateLeft(); rotateLeft() }

    fun isGameOver(): Boolean {
        if (emptyTiles().isNotEmpty()) return false
        for (i in 0 until size)
            for (j in 0 until size) {
                val v = board[i][j].value
                if ((j < size - 1 && board[i][j + 1].value == v) || (i < size - 1 && board[i + 1][j].value == v))
                    return false
            }
        return true
    }
}
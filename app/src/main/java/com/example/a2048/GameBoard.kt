package com.example.a2048

class GameBoard(val size: Int = 4) {
    var board = Array(size) { Array(size) { 0 } }

    init {
        addRandomTile()
        addRandomTile()
    }

    fun addRandomTile() {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until size)
            for (j in 0 until size)
                if (board[i][j] == 0) emptyCells.add(Pair(i, j))
        if (emptyCells.isNotEmpty()) {
            val (row, col) = emptyCells.random()
            board[row][col] = if ((0..9).random() < 9) 2 else 4
        }
    }

    private fun compress(row: List<Int>): List<Int> {
        val newRow = row.filter { it != 0 }.toMutableList()
        while (newRow.size < size) newRow.add(0)
        return newRow
    }

    private fun merge(row: MutableList<Int>): Boolean {
        var moved = false
        for (i in 0 until size - 1) {
            if (row[i] != 0 && row[i] == row[i + 1]) {
                row[i] *= 2
                row[i + 1] = 0
                moved = true
            }
        }
        return moved
    }

    fun moveLeft(): Boolean {
        var moved = false
        for (i in 0 until size) {
            val row = board[i].toMutableList()
            moved = merge(row) || moved
            board[i] = compress(row).toTypedArray()
        }
        if (moved) addRandomTile()
        return moved
    }

    fun moveRight(): Boolean {
        board = board.map { it.reversedArray() }.toTypedArray()
        val moved = moveLeft()
        board = board.map { it.reversedArray() }.toTypedArray()
        return moved
    }

    fun moveUp(): Boolean {
        transpose()
        val moved = moveLeft()
        transpose()
        return moved
    }

    fun moveDown(): Boolean {
        transpose()
        val moved = moveRight()
        transpose()
        return moved
    }

    private fun transpose() {
        val newBoard = Array(size) { Array(size) { 0 } }
        for (i in 0 until size)
            for (j in 0 until size)
                newBoard[i][j] = board[j][i]
        board = newBoard
    }
}

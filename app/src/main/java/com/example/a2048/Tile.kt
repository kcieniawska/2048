package com.example.a2048

data class Tile(var value: Int = 0) {
    // Stan dla animacji
    var previousRow: Int = -1
    var previousCol: Int = -1
    var mergedFrom: Pair<Int, Int>? = null

    fun isEmpty() = value == 0
    fun clear() {
        value = 0
        previousRow = -1
        previousCol = -1
        mergedFrom = null
    }

    fun savePosition(row: Int, col: Int) {
        previousRow = row
        previousCol = col
        mergedFrom = null
    }
}
package com.example.a2048

data class Tile(
    var value: Int = 0,
    var merged: Boolean = false
) {
    fun isEmpty(): Boolean = value == 0
    fun clear() {
        value = 0
        merged = false
    }
}

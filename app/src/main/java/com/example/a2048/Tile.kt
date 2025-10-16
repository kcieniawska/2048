package com.example.a2048

data class Tile(var value: Int = 0) {
    fun isEmpty() = value == 0
    fun clear() { value = 0 }
}

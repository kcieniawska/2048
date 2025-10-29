package com.example.a2048

data class Tile(var value: Int = 0) {
    // Stan dla animacji, używany przez GameView i GameManager
    var previousRow: Int = -1
    var previousCol: Int = -1
    var mergedFrom: Pair<Int, Int>? = null // Przechowuje pozycję kafelka, z którym się połączył

    fun isEmpty() = value == 0

    fun clear() {
        value = 0
        previousRow = -1
        previousCol = -1
        mergedFrom = null
    }

    /**
     * Zapisuje aktualną pozycję kafelka jako jego "poprzednią" pozycję.
     * Używane przez GameManager przed wykonaniem ruchu, aby umożliwić animację przesunięcia.
     */
    fun savePosition(row: Int, col: Int) {
        previousRow = row
        previousCol = col
        mergedFrom = null // Resetujemy stan połączenia
    }
}
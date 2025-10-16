package com.example.a2048

// Klasa danych reprezentująca pojedynczy wynik w tabeli wyników
data class ScoreItem(
    val place: Int,  // miejsce w rankingu
    val score: Int   // wynik gracza
)
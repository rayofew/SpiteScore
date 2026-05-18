package com.reznick.spitescore.data.model

enum class GameType(val displayName: String, val supportsSpiteCards: Boolean = false) {
    CASSINO("Cassino", supportsSpiteCards = true),
    CRIBBAGE("Cribbage", supportsSpiteCards = true),
    GARBAGE("Garbage", supportsSpiteCards = true),
    HEARTS("Hearts"),
    SPADES("Spades"),
    YAHTZEE("Yahtzee"),
    FARKLE("Farkle"),
    DICE10K("Dice 10,000"),
    FREEFORM("Free-form", supportsSpiteCards = true);

    companion object {
        fun fromString(value: String): GameType? =
            entries.firstOrNull { it.name.lowercase() == value.lowercase() }
    }
}

enum class ScoringMode(val displayName: String) {
    MANUAL("Manual entry"),
    CASSINO_STRUCTURED("Cassino scoring"),
    CRIBBAGE_BOARD("Cribbage board"),
    YAHTZEE_CARD("Yahtzee scorecard"),
    RUNNING_TOTAL("Running total")
}

enum class WinCondition(val displayName: String) {
    HIGHEST_SCORE("Highest score wins"),
    LOWEST_SCORE("Lowest score wins"),
    TARGET_SCORE("First to target score"),
    MANUAL("Manual / no condition")
}

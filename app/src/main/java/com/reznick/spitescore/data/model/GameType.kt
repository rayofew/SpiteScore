package com.reznick.spitescore.data.model

enum class GameType(val displayName: String) {
    CASSINO("Cassino"),
    CRIBBAGE("Cribbage"),
    GARBAGE("Garbage"),
    HEARTS("Hearts"),
    SPADES("Spades"),
    YATZY("Yatzy"),
    FARKLE("Farkle"),
    DICE10K("Dice 10,000"),
    FREEFORM("Free-form");

    companion object {
        fun fromString(value: String): GameType? =
            entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                    it.displayName.equals(value, ignoreCase = true)
            }
    }
}

enum class ScoringMode(val displayName: String) {
    MANUAL("Manual entry"),
    CASSINO_STRUCTURED("Cassino scoring"),
    CRIBBAGE_BOARD("Cribbage board"),
    RUNNING_TOTAL("Running total")
}

enum class WinCondition(val displayName: String) {
    HIGHEST_SCORE("Highest score wins"),
    LOWEST_SCORE("Lowest score wins"),
    TARGET_SCORE("First to target score"),
    MANUAL("Manual / no condition")
}

package com.reznick.spitescore.ui.game.cribbage

/** Pure cribbage bookkeeping — no Android or Compose dependencies so it can be unit tested. */
object CribbageRules {
    const val DEFAULT_TARGET = 121

    /** Main pad: every pegging score and the common hand totals. Laid out 4 per row. */
    val MAIN_PAD: List<Int> = (1..12).toList()

    /** Secondary pad for the rare big hands. Laid out 6 per row. */
    val MORE_PAD: List<Int> = (13..29).toList()

    /** A loser who has not reached this score is skunked. Null when the target is too small. */
    fun skunkLine(target: Int): Int? = (target - 30).takeIf { it > 0 }

    /** A loser who has not reached this score is double-skunked. Only full-length games have one. */
    fun doubleSkunkLine(target: Int): Int? = (target - 60).takeIf { target >= DEFAULT_TARGET }

    fun hasWon(score: Int, target: Int): Boolean = score >= target

    /** Label for the finish, or null when the loser cleared every skunk line. */
    fun skunkLabel(target: Int, loserScore: Int): String? = when {
        doubleSkunkLine(target)?.let { loserScore < it } == true -> "Double skunk!"
        skunkLine(target)?.let { loserScore < it } == true       -> "Skunk!"
        else                                                      -> null
    }

    /**
     * Position of the trailing peg: the score before the most recent change.
     * A take-back makes [lastDelta] negative, so the back peg never leads the front one.
     */
    fun backPeg(current: Int, lastDelta: Int?): Int =
        if (lastDelta == null) 0 else (current - lastDelta).coerceIn(0, current.coerceAtLeast(0))
}

package com.reznick.spitescore.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GameConfig(
    val gameType: GameType,
    val variant: String = "",
    val scoringMode: ScoringMode = ScoringMode.MANUAL,
    val winCondition: WinCondition = WinCondition.HIGHEST_SCORE,
    val targetScore: Int = 121,
    val muggins: Boolean = false,
    val skunks: Boolean = false,
    val shortGame: Boolean = false,
    val savedSetupId: String? = null
)

data class ActiveGame(
    val id: String,
    val config: GameConfig,
    val players: List<Player>,
    val rounds: List<RoundScores>,
    val entries: List<ScoreEntry>,
    val startedAt: Long,
    val isOver: Boolean = false,
    val winnerSeats: List<Int> = emptyList()
) {
    fun totalScore(seat: Int): Int = entries.filter { it.playerSeat == seat }.sumOf { it.points }

    fun runningTotals(): Map<Int, Int> = players.associate { it.seat to totalScore(it.seat) }

    fun leader(): Player? {
        val totals = runningTotals()
        return players.maxByOrNull { totals[it.seat] ?: 0 }
    }
}

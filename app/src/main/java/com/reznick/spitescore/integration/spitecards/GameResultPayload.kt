package com.reznick.spitescore.integration.spitecards

import kotlinx.serialization.Serializable

// Received from SpiteCards at game end
@Serializable
data class GameResultPayload(
    val schemaVersion: Int,
    val sourceApp: String,
    val gameType: String,
    val variant: String = "",
    val ruleOptions: Map<String, Boolean> = emptyMap(),
    val players: List<ResultPlayer>,
    val scores: List<ResultScore>,
    val winnerSeats: List<Int>,
    val playedAt: String,
    val durationSeconds: Long,
    /** Optional full per-player breakdown (e.g. the Yatzy scorecard). Empty from apps that don't send it. */
    val scorecards: List<PlayerScorecard> = emptyList()
)

@Serializable
data class ResultPlayer(val name: String, val seat: Int, val id: String = "")

@Serializable
data class ResultScore(val seat: Int, val total: Int)

/** One player's full card: a value per category key, plus computed bonuses and total. */
@Serializable
data class PlayerScorecard(
    val seat: Int,
    val categories: Map<String, Int> = emptyMap(),
    val upperBonus: Int = 0,
    /** Count of EXTRA yatzies beyond the first (each typically worth bonus points). */
    val yatzyBonus: Int = 0,
    val total: Int = 0
)

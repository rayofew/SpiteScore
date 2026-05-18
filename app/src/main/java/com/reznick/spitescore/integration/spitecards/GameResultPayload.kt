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
    val durationSeconds: Long
)

@Serializable
data class ResultPlayer(val name: String, val seat: Int)

@Serializable
data class ResultScore(val seat: Int, val total: Int)

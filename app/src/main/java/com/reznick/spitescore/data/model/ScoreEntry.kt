package com.reznick.spitescore.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ScoreEntry(
    val round: Int,
    val playerSeat: Int,
    val points: Int,
    val label: String = "",       // e.g. "Sweep", "His heels", "Pegging"
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class RoundScores(
    val round: Int,
    val scores: Map<Int, Int>     // seat -> points this round
)

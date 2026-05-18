package com.reznick.spitescore.data.model

data class SavedSetup(
    val id: String,
    val name: String,           // e.g. "Friday Cribbage"
    val config: GameConfig,
    val defaultPlayers: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

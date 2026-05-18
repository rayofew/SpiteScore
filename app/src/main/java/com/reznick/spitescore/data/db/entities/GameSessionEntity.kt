package com.reznick.spitescore.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_sessions")
data class GameSessionEntity(
    @PrimaryKey val id: String,
    val gameType: String,
    val variant: String,
    val configJson: String,
    val playersJson: String,
    val entriesJson: String,
    val winnerSeats: String,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Long
)

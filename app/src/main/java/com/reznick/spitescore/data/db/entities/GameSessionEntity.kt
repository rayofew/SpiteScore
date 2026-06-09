package com.reznick.spitescore.data.db.entities

import androidx.room.ColumnInfo
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
    /** Stores winner names (not seat numbers). Column kept as "winnerSeats" for DB compatibility. */
    @ColumnInfo(name = "winnerSeats") val winnerNames: String,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Long
)

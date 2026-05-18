package com.reznick.spitescore.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_setups")
data class SavedSetupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val configJson: String,
    val defaultPlayersJson: String,
    val createdAt: Long
)

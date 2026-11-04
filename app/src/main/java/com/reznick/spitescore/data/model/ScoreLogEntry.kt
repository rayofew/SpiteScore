package com.reznick.spitescore.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ScoreLogEntry(
    val id: Long = System.nanoTime(),       // unique key for LazyList
    val seat: Int,
    val playerName: String,
    val delta: Int,
    val scoreAfter: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isUndo: Boolean = false,
    val reversed: Boolean = false           // this entry was later taken back from the log
)

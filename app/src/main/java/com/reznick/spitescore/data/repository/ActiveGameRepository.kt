package com.reznick.spitescore.data.repository

import android.content.Context
import com.reznick.spitescore.data.model.Player
import com.reznick.spitescore.data.model.ScoreLogEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ActiveGameState(
    val gameId: String,
    val players: List<Player>,
    val scores: Map<String, Int>,
    val nextSeat: Int,
    val playUntil: Int? = null,
    val gameType: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val partnerships: Map<String, String> = emptyMap(),  // "seat" → "partnerSeat", bidirectional
    val scoreLog: List<ScoreLogEntry> = emptyList()
)

class ActiveGameRepository(context: Context) {
    private val prefs = context.getSharedPreferences("active_game", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun save(state: ActiveGameState) {
        prefs.edit().putString("state", json.encodeToString(state)).apply()
    }

    fun load(): ActiveGameState? {
        val raw = prefs.getString("state", null) ?: return null
        return try { json.decodeFromString(raw) } catch (_: Exception) { null }
    }

    fun hasGame(): Boolean = load() != null

    fun clear() {
        prefs.edit().remove("state").apply()
    }
}
package com.reznick.spitescore.data.repository

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PlayerRosterRepository(context: Context) {
    private val prefs = context.getSharedPreferences("player_roster", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun getPlayers(): List<String> {
        val raw = prefs.getString("players", null) ?: return emptyList()
        return try { json.decodeFromString(raw) } catch (_: Exception) { emptyList() }
    }

    fun addPlayer(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val current = getPlayers().toMutableList()
        if (trimmed !in current) {
            current.add(trimmed)
            prefs.edit().putString("players", json.encodeToString(current)).apply()
        }
    }

    fun removePlayer(name: String) {
        val updated = getPlayers().filter { it != name }
        prefs.edit().putString("players", json.encodeToString(updated)).apply()
    }

    fun save(players: List<String>) {
        prefs.edit().putString("players", json.encodeToString(players)).apply()
    }
}
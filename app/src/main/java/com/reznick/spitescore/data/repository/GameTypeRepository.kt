package com.reznick.spitescore.data.repository

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GameTypeRepository(context: Context) {
    private val prefs = context.getSharedPreferences("game_types", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val defaults = listOf(
        "Cribbage", "Rummy", "Gin Rummy", "Go Fish",
        "Spades", "Hearts", "Canasta"
    )

    fun getTypes(): List<String> {
        if (!prefs.contains("types")) {
            prefs.edit().putString("types", json.encodeToString(defaults)).apply()
            return defaults
        }
        val raw = prefs.getString("types", null) ?: return emptyList()
        return try { json.decodeFromString(raw) } catch (_: Exception) { emptyList() }
    }

    fun addType(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val current = getTypes().toMutableList()
        if (trimmed !in current) {
            current.add(trimmed)
            prefs.edit().putString("types", json.encodeToString(current)).apply()
        }
    }

    fun removeType(name: String) {
        val updated = getTypes().filter { it != name }
        prefs.edit().putString("types", json.encodeToString(updated)).apply()
    }
}

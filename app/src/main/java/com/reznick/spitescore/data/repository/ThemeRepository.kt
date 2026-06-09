package com.reznick.spitescore.data.repository

import android.content.Context

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class ThemeRepository(context: Context) {
    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    fun getTheme(): ThemeMode {
        val name = prefs.getString("mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try { ThemeMode.valueOf(name) } catch (_: Exception) { ThemeMode.SYSTEM }
    }

    fun setTheme(mode: ThemeMode) {
        prefs.edit().putString("mode", mode.name).apply()
    }
}

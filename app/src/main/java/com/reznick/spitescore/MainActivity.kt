package com.reznick.spitescore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import com.reznick.spitescore.data.repository.ThemeMode
import com.reznick.spitescore.integration.spitecards.GameResultPayload
import com.reznick.spitescore.navigation.SpiteScoreNavHost
import com.reznick.spitescore.ui.theme.SpiteScoreTheme
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_GAME_RESULT = "extra_game_result"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val incomingResult: GameResultPayload? = intent.getStringExtra(EXTRA_GAME_RESULT)?.let {
            runCatching { Json.decodeFromString<GameResultPayload>(it) }.getOrNull()
        }
        val themeRepo = (applicationContext as SpiteScoreApp).themeRepository

        setContent {
            var themeMode by remember { mutableStateOf(themeRepo.getTheme()) }
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT  -> false
                ThemeMode.DARK   -> true
            }

            SpiteScoreTheme(darkTheme = darkTheme) {
                SpiteScoreNavHost(
                    incomingResult = incomingResult,
                    currentTheme = themeMode,
                    onThemeChange = { mode ->
                        themeRepo.setTheme(mode)
                        themeMode = mode
                    }
                )
            }
        }
    }
}

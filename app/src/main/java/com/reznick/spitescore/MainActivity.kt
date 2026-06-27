package com.reznick.spitescore

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        // Always-light, brand-locked (like SpiteYatzy). Light status bar style = DARK icons, so the
        // clock/battery stay visible on the cream background.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        val incomingResult: GameResultPayload? = intent.getStringExtra(EXTRA_GAME_RESULT)?.let {
            runCatching { Json.decodeFromString<GameResultPayload>(it) }.getOrNull()
        }
        setContent {
            SpiteScoreTheme {
                SpiteScoreNavHost(incomingResult = incomingResult)
            }
        }
    }
}

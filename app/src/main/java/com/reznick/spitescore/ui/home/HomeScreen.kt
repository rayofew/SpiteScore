package com.reznick.spitescore.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reznick.spitescore.integration.spitecards.GameResultPayload

@Composable
fun HomeScreen(
    incomingResult: GameResultPayload?,
    onNewGame: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "SpiteScore",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Track scores. No ads. No accounts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // SpiteCards handoff banner
            if (incomingResult != null) {
                SpiteCardsResultBanner(
                    result = incomingResult,
                    onNewGame = onNewGame,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            Button(
                onClick = onNewGame,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("New Game", style = MaterialTheme.typography.titleLarge)
            }

            Spacer(Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onHistory) { Text("History") }
                TextButton(onClick = onSettings) { Text("Settings") }
                TextButton(onClick = onAbout) { Text("About") }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "Part of the Spite line · Tap to see other apps",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable { onAbout() }
            )
        }
    }
}

@Composable
private fun SpiteCardsResultBanner(
    result: GameResultPayload,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Game received from SpiteCards", style = MaterialTheme.typography.titleLarge)
            Text(
                "${result.gameType.replaceFirstChar { it.uppercase() }} · ${result.players.size} players",
                style = MaterialTheme.typography.bodyMedium
            )
            result.scores.forEach { score ->
                val name = result.players.find { it.seat == score.seat }?.name ?: "Seat ${score.seat}"
                val isWinner = score.seat in result.winnerSeats
                Text(
                    "$name: ${score.total} pts${if (isWinner) " 🏆" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isWinner) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(4.dp))
            Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth()) {
                Text("Save to history")
            }
        }
    }
}

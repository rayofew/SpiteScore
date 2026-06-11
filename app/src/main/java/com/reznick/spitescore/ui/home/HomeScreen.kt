package com.reznick.spitescore.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reznick.spitescore.R
import com.reznick.spitescore.integration.spitecards.GameResultPayload

@Composable
fun HomeScreen(
    incomingResult: GameResultPayload?,
    hasSavedGame: Boolean,
    onNewGame: () -> Unit,
    onContinueGame: () -> Unit,
    onSaveResult: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit
) {
    var resultSaved by remember { mutableStateOf(false) }
    // Auto-save a score handed back from a companion app (e.g. SpiteYatzy) exactly once,
    // so it lands in history without a manual tap. saveReceivedResult is idempotent
    // (REPLACE on a stable id), so a re-delivered result can't duplicate the entry.
    LaunchedEffect(incomingResult) {
        if (incomingResult != null && !resultSaved) {
            onSaveResult()
            resultSaved = true
        }
    }
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.spite_logo),
                contentDescription = "SpiteScore",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .padding(bottom = 8.dp)
            )
            Text(
                "Score tracker · No ads · No accounts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 48.dp)
            )

            if (incomingResult != null) {
                SpiteCardsResultBanner(
                    result = incomingResult,
                    onViewStats = onStats,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            if (hasSavedGame) {
                Button(
                    onClick = onContinueGame,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Continue Game", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onNewGame,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("New Game", style = MaterialTheme.typography.titleLarge)
                }
            } else {
                Button(
                    onClick = onNewGame,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("New Game", style = MaterialTheme.typography.titleLarge)
                }
            }

            Spacer(Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onStats) { Text("Stats") }
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
    onViewStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val source = when (result.sourceApp) {
                "spiteyatzy" -> "SpiteYatzy"
                "spitecards" -> "SpiteCards"
                else         -> result.sourceApp.ifBlank { "a companion app" }
            }
            Text("Score saved from $source", style = MaterialTheme.typography.titleLarge)
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
            Button(onClick = onViewStats, modifier = Modifier.fillMaxWidth()) {
                Text("View in Stats")
            }
        }
    }
}
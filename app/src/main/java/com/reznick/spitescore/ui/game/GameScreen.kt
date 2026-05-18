package com.reznick.spitescore.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reznick.spitescore.data.model.Player
import com.reznick.spitescore.data.model.ScoreEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    gameId: String,
    onGameEnd: () -> Unit
) {
    // In production, these flow from GameViewModel
    val players = remember {
        listOf(
            Player("p0", "Player 1", 0),
            Player("p1", "Player 2", 1)
        )
    }
    val entries = remember { mutableStateListOf<ScoreEntry>() }
    var round by remember { mutableIntStateOf(1) }
    var showAddRound by remember { mutableStateOf(false) }
    var showEndGame by remember { mutableStateOf(false) }

    val totals = players.associate { p -> p.seat to entries.filter { it.playerSeat == p.seat }.sumOf { it.points } }
    val leader = totals.maxByOrNull { it.value }?.key

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Round $round") },
                actions = {
                    TextButton(onClick = { showEndGame = true }) { Text("End Game") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddRound = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add scores")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Running scoreboard
            ScoreboardHeader(players, totals, leader)
            HorizontalDivider()

            // Score history
            LazyColumn(modifier = Modifier.weight(1f)) {
                val grouped = entries.groupBy { it.round }
                grouped.keys.sortedDescending().forEach { r ->
                    item {
                        Text(
                            "Round $r",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(grouped[r] ?: emptyList()) { entry ->
                        val playerName = players.find { it.seat == entry.playerSeat }?.name ?: "?"
                        ListItem(
                            headlineContent = { Text(playerName) },
                            supportingContent = { if (entry.label.isNotEmpty()) Text(entry.label) },
                            trailingContent = {
                                Text(
                                    "${if (entry.points >= 0) "+" else ""}${entry.points}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (entry.points >= 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddRound) {
        AddRoundDialog(
            players = players,
            round = round,
            onConfirm = { newEntries ->
                entries.addAll(newEntries)
                round++
                showAddRound = false
            },
            onDismiss = { showAddRound = false }
        )
    }

    if (showEndGame) {
        EndGameDialog(
            players = players,
            totals = totals,
            onConfirm = onGameEnd,
            onDismiss = { showEndGame = false }
        )
    }
}

@Composable
private fun ScoreboardHeader(
    players: List<Player>,
    totals: Map<Int, Int>,
    leaderSeat: Int?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        players.forEach { player ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    player.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (player.seat == leaderSeat) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${totals[player.seat] ?: 0}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (player.seat == leaderSeat) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun AddRoundDialog(
    players: List<Player>,
    round: Int,
    onConfirm: (List<ScoreEntry>) -> Unit,
    onDismiss: () -> Unit
) {
    val scores = remember { mutableStateMapOf<Int, String>().also { m -> players.forEach { m[it.seat] = "" } } }
    val labels = remember { mutableStateMapOf<Int, String>().also { m -> players.forEach { m[it.seat] = "" } } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Round $round scores") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                players.forEach { player ->
                    Text(player.name, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = scores[player.seat] ?: "",
                            onValueChange = { scores[player.seat] = it },
                            label = { Text("Points") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = labels[player.seat] ?: "",
                            onValueChange = { labels[player.seat] = it },
                            label = { Text("Note") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val entries = players.mapNotNull { player ->
                    val pts = scores[player.seat]?.toIntOrNull() ?: return@mapNotNull null
                    ScoreEntry(
                        round = round,
                        playerSeat = player.seat,
                        points = pts,
                        label = labels[player.seat] ?: ""
                    )
                }
                onConfirm(entries)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EndGameDialog(
    players: List<Player>,
    totals: Map<Int, Int>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sorted = players.sortedByDescending { totals[it.seat] ?: 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("End game?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                sorted.forEachIndexed { i, player ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${i + 1}. ${player.name}")
                        Text("${totals[player.seat] ?: 0} pts")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("End Game") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep playing") } }
    )
}

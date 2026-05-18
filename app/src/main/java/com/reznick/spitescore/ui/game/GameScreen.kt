package com.reznick.spitescore.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.reznick.spitescore.data.model.Player
import com.reznick.spitescore.data.model.ScoreEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    gameId: String,
    playerNames: List<String>,
    onGameEnd: () -> Unit
) {
    val players = remember {
        playerNames.mapIndexed { i, name ->
            Player("p$i", name.ifBlank { "Player ${i + 1}" }, i)
        }
    }
    val scores = remember { mutableStateMapOf<Int, Int>().also { m -> players.forEach { m[it.seat] = 0 } } }
    val history = remember { mutableStateListOf<ScoreEntry>() }
    var entryCounter by remember { mutableIntStateOf(1) }
    var selectedPlayer by remember { mutableStateOf<Player?>(null) }
    var showEndGame by remember { mutableStateOf(false) }

    val leader = scores.entries.maxByOrNull { it.value }?.key?.takeIf { (scores[it] ?: 0) > 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SpiteScore") },
                actions = {
                    TextButton(onClick = { showEndGame = true }) { Text("End Game") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            players.forEach { player ->
                PlayerCard(
                    player = player,
                    score = scores[player.seat] ?: 0,
                    isLeader = player.seat == leader,
                    onClick = { selectedPlayer = player }
                )
            }

            if (history.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    "History",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(history.reversed()) { entry ->
                        val playerName = players.find { it.seat == entry.playerSeat }?.name ?: "?"
                        ListItem(
                            headlineContent = { Text(playerName) },
                            supportingContent = { if (entry.label.isNotEmpty()) Text(entry.label) },
                            trailingContent = {
                                Text(
                                    "${if (entry.points >= 0) "+" else ""}${entry.points}",
                                    style = MaterialTheme.typography.titleMedium,
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

    selectedPlayer?.let { player ->
        PlayerScoreDialog(
            player = player,
            currentScore = scores[player.seat] ?: 0,
            onAdd = { amount ->
                scores[player.seat] = (scores[player.seat] ?: 0) + amount
                history.add(ScoreEntry(entryCounter++, player.seat, amount, ""))
                selectedPlayer = null
            },
            onSubtract = { amount ->
                scores[player.seat] = (scores[player.seat] ?: 0) - amount
                history.add(ScoreEntry(entryCounter++, player.seat, -amount, ""))
                selectedPlayer = null
            },
            onSetTo = { value ->
                val diff = value - (scores[player.seat] ?: 0)
                scores[player.seat] = value
                history.add(ScoreEntry(entryCounter++, player.seat, diff, "→ $value"))
                selectedPlayer = null
            },
            onDismiss = { selectedPlayer = null }
        )
    }

    if (showEndGame) {
        EndGameDialog(
            players = players,
            scores = scores,
            onConfirm = onGameEnd,
            onDismiss = { showEndGame = false }
        )
    }
}

@Composable
private fun PlayerCard(
    player: Player,
    score: Int,
    isLeader: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLeader) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(player.name, style = MaterialTheme.typography.headlineSmall)
            Text("$score", style = MaterialTheme.typography.displaySmall)
        }
    }
}

@Composable
private fun PlayerScoreDialog(
    player: Player,
    currentScore: Int,
    onAdd: (Int) -> Unit,
    onSubtract: (Int) -> Unit,
    onSetTo: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var setToValue by remember { mutableStateOf("") }
    val amountInt = amount.toIntOrNull()
    val setToInt = setToValue.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(player.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Score: $currentScore",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { amountInt?.let { onSubtract(it) } },
                        enabled = amountInt != null && amountInt > 0
                    ) { Text("−") }

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { c -> c.isDigit() } },
                        label = { Text("Points") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Button(
                        onClick = { amountInt?.let { onAdd(it) } },
                        enabled = amountInt != null && amountInt > 0
                    ) { Text("+") }
                }

                HorizontalDivider()

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = setToValue,
                        onValueChange = { v ->
                            if (v.isEmpty() || v == "-" || v.toIntOrNull() != null) setToValue = v
                        },
                        label = { Text("Change score to") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Button(
                        onClick = { setToInt?.let { onSetTo(it) } },
                        enabled = setToInt != null
                    ) { Text("Set") }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EndGameDialog(
    players: List<Player>,
    scores: Map<Int, Int>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sorted = players.sortedByDescending { scores[it.seat] ?: 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("End game?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                sorted.forEachIndexed { i, player ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${i + 1}. ${player.name}")
                        Text("${scores[player.seat] ?: 0} pts")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("End Game") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep playing") } }
    )
}

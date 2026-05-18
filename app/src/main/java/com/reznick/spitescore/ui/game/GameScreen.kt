package com.reznick.spitescore.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardCapitalization
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
        mutableStateListOf(*playerNames.mapIndexed { i, name ->
            Player("p$i", name.ifBlank { "Player ${i + 1}" }, i)
        }.toTypedArray())
    }
    var nextSeat by remember { mutableIntStateOf(playerNames.size) }
    val scores = remember { mutableStateMapOf<Int, Int>().also { m -> players.forEach { m[it.seat] = 0 } } }
    val history = remember { mutableStateListOf<ScoreEntry>() }
    var entryCounter by remember { mutableIntStateOf(1) }
    var showEndGame by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var renamingPlayer by remember { mutableStateOf<Player?>(null) }

    val leader = scores.entries
        .filter { e -> players.any { it.seat == e.key } }
        .maxByOrNull { it.value }?.key
        ?.takeIf { (scores[it] ?: 0) > 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SpiteScore") },
                actions = {
                    if (history.isNotEmpty()) {
                        TextButton(onClick = { showHistory = true }) { Text("History") }
                    }
                    TextButton(onClick = { showEndGame = true }) { Text("End Game") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            players.forEach { player ->
                PlayerCard(
                    player = player,
                    score = scores[player.seat] ?: 0,
                    isLeader = player.seat == leader,
                    canRemove = players.size > 1,
                    onNameTap = { renamingPlayer = player },
                    onRemove = {
                        players.remove(player)
                    },
                    onAdd = { amount ->
                        scores[player.seat] = (scores[player.seat] ?: 0) + amount
                        history.add(ScoreEntry(entryCounter++, player.seat, amount, ""))
                    },
                    onSubtract = { amount ->
                        scores[player.seat] = (scores[player.seat] ?: 0) - amount
                        history.add(ScoreEntry(entryCounter++, player.seat, -amount, ""))
                    },
                    onSetTo = { value ->
                        val diff = value - (scores[player.seat] ?: 0)
                        scores[player.seat] = value
                        history.add(ScoreEntry(entryCounter++, player.seat, diff, "→ $value"))
                    }
                )
            }

            OutlinedButton(
                onClick = {
                    val seat = nextSeat++
                    val name = "Player ${players.size + 1}"
                    players.add(Player("p$seat", name, seat))
                    scores[seat] = 0
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add Player") }
        }
    }

    renamingPlayer?.let { player ->
        RenameDialog(
            current = player.name,
            onConfirm = { newName ->
                val idx = players.indexOfFirst { it.id == player.id }
                if (idx >= 0) players[idx] = players[idx].copy(name = newName)
                renamingPlayer = null
            },
            onDismiss = { renamingPlayer = null }
        )
    }

    if (showHistory) {
        HistorySheet(
            players = players,
            history = history,
            onDismiss = { showHistory = false }
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
    canRemove: Boolean,
    onNameTap: () -> Unit,
    onRemove: () -> Unit,
    onAdd: (Int) -> Unit,
    onSubtract: (Int) -> Unit,
    onSetTo: (Int) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var setToValue by remember { mutableStateOf("") }
    val amountInt = amount.toIntOrNull()
    val setToInt = setToValue.toIntOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLeader) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onNameTap,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(player.name, style = MaterialTheme.typography.headlineSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$score", style = MaterialTheme.typography.displaySmall)
                    if (canRemove) {
                        IconButton(onClick = onRemove) {
                            Icon(
                                Icons.Default.PersonRemove,
                                contentDescription = "Remove ${player.name}",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { amountInt?.let { onSubtract(it); amount = "" } },
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
                    onClick = { amountInt?.let { onAdd(it); amount = "" } },
                    enabled = amountInt != null && amountInt > 0
                ) { Text("+") }
            }

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
                    onClick = { setToInt?.let { onSetTo(it); setToValue = "" } },
                    enabled = setToInt != null
                ) { Text("Set") }
            }
        }
    }
}

@Composable
private fun RenameDialog(
    current: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(current) }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename player") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onConfirm(text.trim()) },
                enabled = text.isNotBlank()
            ) { Text("Rename") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorySheet(
    players: List<Player>,
    history: List<ScoreEntry>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
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

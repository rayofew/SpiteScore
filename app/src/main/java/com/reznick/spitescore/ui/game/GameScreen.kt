package com.reznick.spitescore.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.key
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
    var selectedSeat by remember { mutableIntStateOf(players.firstOrNull()?.seat ?: 0) }
    var showEndGame by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var renamingPlayer by remember { mutableStateOf<Player?>(null) }

    val leader = scores.entries
        .filter { e -> players.any { it.seat == e.key } }
        .maxByOrNull { it.value }?.key
        ?.takeIf { (scores[it] ?: 0) > 0 }

    val selectedPlayer = players.find { it.seat == selectedSeat } ?: players.firstOrNull()

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
                .imePadding()
        ) {
            // Scoreboard — fills remaining space
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(players, key = { it.id }) { player ->
                    val isSelected = player.seat == selectedSeat
                    val isLeader = player.seat == leader
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSeat = player.seat }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            player.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isLeader) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${scores[player.seat] ?: 0}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (isLeader) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    HorizontalDivider()
                }

                item {
                    TextButton(
                        onClick = {
                            val seat = nextSeat++
                            players.add(Player("p$seat", "Player ${players.size + 1}", seat))
                            scores[seat] = 0
                            selectedSeat = seat
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Player")
                    }
                }
            }

            // Input panel — pinned to bottom
            if (selectedPlayer != null) {
                HorizontalDivider()
                ScoreInputPanel(
                    player = selectedPlayer,
                    currentScore = scores[selectedPlayer.seat] ?: 0,
                    canRemove = players.size > 1,
                    onRename = { renamingPlayer = selectedPlayer },
                    onRemove = {
                        val fallback = players.firstOrNull { it.seat != selectedPlayer.seat }?.seat ?: -1
                        players.remove(selectedPlayer)
                        selectedSeat = fallback
                    },
                    onAdd = { n ->
                        scores[selectedPlayer.seat] = (scores[selectedPlayer.seat] ?: 0) + n
                        history.add(ScoreEntry(entryCounter++, selectedPlayer.seat, n, ""))
                    },
                    onSubtract = { n ->
                        scores[selectedPlayer.seat] = (scores[selectedPlayer.seat] ?: 0) - n
                        history.add(ScoreEntry(entryCounter++, selectedPlayer.seat, -n, ""))
                    },
                    onSetTo = { value ->
                        val diff = value - (scores[selectedPlayer.seat] ?: 0)
                        scores[selectedPlayer.seat] = value
                        history.add(ScoreEntry(entryCounter++, selectedPlayer.seat, diff, "→ $value"))
                    }
                )
            }
        }
    }

    renamingPlayer?.let { player ->
        RenameDialog(
            onConfirm = { newName ->
                val idx = players.indexOfFirst { it.id == player.id }
                if (idx >= 0) players[idx] = players[idx].copy(name = newName)
                renamingPlayer = null
            },
            onDismiss = { renamingPlayer = null }
        )
    }

    if (showHistory) {
        HistorySheet(players = players, history = history, onDismiss = { showHistory = false })
    }

    if (showEndGame) {
        EndGameDialog(players = players, scores = scores, onConfirm = onGameEnd, onDismiss = { showEndGame = false })
    }
}

@Composable
private fun ScoreInputPanel(
    player: Player,
    currentScore: Int,
    canRemove: Boolean,
    onRename: () -> Unit,
    onRemove: () -> Unit,
    onAdd: (Int) -> Unit,
    onSubtract: (Int) -> Unit,
    onSetTo: (Int) -> Unit
) {
    val amountState = remember(player.id) { mutableStateOf("") }
    val setToState = remember(player.id) { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Player name + rename/remove
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onRename,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(player.name, style = MaterialTheme.typography.titleMedium)
            }
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

        // +/- row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(onClick = {
                val n = amountState.value.toIntOrNull() ?: 1
                onSubtract(n); amountState.value = ""
            }) { Text("−") }

            OutlinedTextField(
                value = amountState.value,
                onValueChange = { amountState.value = it.filter { c -> c.isDigit() } },
                placeholder = { Text("1") },
                label = { Text("Points") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Button(onClick = {
                val n = amountState.value.toIntOrNull() ?: 1
                onAdd(n); amountState.value = ""
            }) { Text("+") }
        }

        // Set to row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = setToState.value,
                onValueChange = { v ->
                    if (v.isEmpty() || v == "-" || v.toIntOrNull() != null) setToState.value = v
                },
                label = { Text("Change score to") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Button(onClick = {
                val n = setToState.value.toIntOrNull()
                if (n != null) { onSetTo(n); setToState.value = "" }
            }) { Text("Set") }
        }
    }
}

@Composable
private fun RenameDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
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
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }, enabled = text.isNotBlank()) {
                Text("Rename")
            }
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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

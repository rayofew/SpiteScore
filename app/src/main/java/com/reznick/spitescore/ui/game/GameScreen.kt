package com.reznick.spitescore.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
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
        },
        bottomBar = {
            if (selectedPlayer != null) {
                Surface(shadowElevation = 8.dp) {
                    HorizontalDivider()
                    ScoreInputPanel(
                        player = selectedPlayer,
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
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                        .padding(horizontal = 20.dp, vertical = 16.dp),
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
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Player")
                }
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
    canRemove: Boolean,
    onRename: () -> Unit,
    onRemove: () -> Unit,
    onAdd: (Int) -> Unit,
    onSubtract: (Int) -> Unit,
    onSetTo: (Int) -> Unit
) {
    var input by remember(player.id) { mutableStateOf("") }

    fun appendDigit(d: String) {
        input = if (input.isEmpty() || input == "0") d
                else if (input.length < 6) input + d
                else input
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Player name + input display + rename/remove
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onRename, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                Text(player.name, style = MaterialTheme.typography.labelLarge)
            }
            Text(
                text = if (input.isEmpty()) "—" else input,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.PersonRemove, contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
        }

        // Keypad + action buttons side by side
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Digit grid
            Column(modifier = Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(listOf("7","8","9"), listOf("4","5","6"), listOf("1","2","3")).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { digit ->
                            KeypadDigitButton(digit, modifier = Modifier.weight(1f)) { appendDigit(digit) }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    KeypadDigitButton("0", modifier = Modifier.weight(2f)) { appendDigit("0") }
                    KeypadDigitButton("⌫", modifier = Modifier.weight(1f)) { input = input.dropLast(1) }
                }
            }

            // Action buttons
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { onAdd(input.toIntOrNull() ?: 1); input = "" },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp)
                ) { Text("+", style = MaterialTheme.typography.titleMedium) }

                OutlinedButton(
                    onClick = { onSubtract(input.toIntOrNull() ?: 1); input = "" },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp)
                ) { Text("−", style = MaterialTheme.typography.titleMedium) }

                Button(
                    onClick = { val n = input.toIntOrNull(); if (n != null) { onSetTo(n); input = "" } },
                    enabled = input.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("Set", style = MaterialTheme.typography.labelMedium) }

            }
        }
    }
}

@Composable
private fun KeypadDigitButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge)
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

package com.reznick.spitescore.ui.wizard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.reznick.spitescore.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGameWizardScreen(
    onGameStarted: (String, List<String>) -> Unit,
    onBack: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var selectedGame by remember { mutableStateOf(GameType.CASSINO) }
    var players by remember { mutableStateOf(listOf("", "")) }
    var winCondition by remember { mutableStateOf(WinCondition.HIGHEST_SCORE) }
    var targetScore by remember { mutableIntStateOf(121) }
    var muggins by remember { mutableStateOf(false) }
    var setupName by remember { mutableStateOf("") }
    var saveSetup by remember { mutableStateOf(false) }

    val totalSteps = 4

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Game — Step ${step + 1} of $totalSteps") },
                navigationIcon = {
                    IconButton(onClick = { if (step == 0) onBack() else step-- }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (step < totalSteps - 1) {
                    Button(
                        onClick = { step++ },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = step != 1 || players.any { it.isNotBlank() }
                    ) { Text("Next") }
                } else {
                    Button(
                        onClick = {
                            val names = players.filter { it.isNotBlank() }
                                .ifEmpty { listOf("Player 1", "Player 2") }
                            onGameStarted("game_${System.currentTimeMillis()}", names)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Start Game") }
                }
                TextButton(
                    onClick = { onGameStarted("game_${System.currentTimeMillis()}", listOf("Player 1", "Player 2")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Skip to free-form") }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (step) {
                0 -> GamePickerStep(selectedGame) { selectedGame = it }
                1 -> PlayersStep(players) { players = it }
                2 -> RulesStep(selectedGame, winCondition, targetScore, muggins,
                    onWin = { winCondition = it },
                    onTarget = { targetScore = it },
                    onMuggins = { muggins = it })
                3 -> SaveSetupStep(saveSetup, setupName, { saveSetup = it }, { setupName = it })
            }
        }
    }
}

@Composable
private fun GamePickerStep(selected: GameType, onSelect: (GameType) -> Unit) {
    Text("Choose a game", style = MaterialTheme.typography.headlineMedium)
    GameType.entries.forEach { game ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            RadioButton(selected = selected == game, onClick = { onSelect(game) })
            Spacer(Modifier.width(8.dp))
            Text(game.displayName)
        }
    }
}

@Composable
private fun PlayersStep(players: List<String>, onUpdate: (List<String>) -> Unit) {
    Text("Players", style = MaterialTheme.typography.headlineMedium)
    players.forEachIndexed { i, name ->
        OutlinedTextField(
            value = name,
            onValueChange = { new -> onUpdate(players.toMutableList().also { it[i] = new }) },
            label = { Text("Player ${i + 1}") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (players.size < 6) {
            OutlinedButton(onClick = { onUpdate(players + "") }) { Text("Add player") }
        }
        if (players.size > 2) {
            OutlinedButton(onClick = { onUpdate(players.dropLast(1)) }) { Text("Remove") }
        }
    }
}

@Composable
private fun RulesStep(
    game: GameType,
    winCondition: WinCondition,
    targetScore: Int,
    muggins: Boolean,
    onWin: (WinCondition) -> Unit,
    onTarget: (Int) -> Unit,
    onMuggins: (Boolean) -> Unit
) {
    Text("Rules", style = MaterialTheme.typography.headlineMedium)
    Text("Win condition", style = MaterialTheme.typography.titleLarge)
    WinCondition.entries.forEach { w ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = winCondition == w, onClick = { onWin(w) })
            Spacer(Modifier.width(8.dp))
            Text(w.displayName)
        }
    }
    if (winCondition == WinCondition.TARGET_SCORE) {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = targetScore.toString(),
            onValueChange = { it.toIntOrNull()?.let(onTarget) },
            label = { Text("Target score") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
    if (game == GameType.CRIBBAGE) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = muggins, onCheckedChange = onMuggins)
            Spacer(Modifier.width(8.dp))
            Text("Muggins")
        }
    }
}

@Composable
private fun SaveSetupStep(
    save: Boolean,
    name: String,
    onSave: (Boolean) -> Unit,
    onName: (String) -> Unit
) {
    Text("Save setup?", style = MaterialTheme.typography.headlineMedium)
    Text(
        "Save this game config as a named setup so you can start it again with one tap.",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = save, onCheckedChange = onSave)
        Spacer(Modifier.width(12.dp))
        Text("Save as a setup")
    }
    if (save) {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onName,
            label = { Text("Setup name (e.g. Friday Cribbage)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

package com.reznick.spitescore.ui.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.Splitscreen
import com.reznick.spitescore.ui.game.cribbage.CribbageScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reznick.spitescore.SpiteScoreApp
import com.reznick.spitescore.data.model.Player
import com.reznick.spitescore.data.model.ScoreLogEntry
import com.reznick.spitescore.ui.theme.DeltaNegative
import com.reznick.spitescore.ui.theme.DeltaPositive
import com.reznick.spitescore.ui.wizard.PlayerTeamChip
import com.reznick.spitescore.ui.wizard.teamColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    onGameEnd: () -> Unit,
    onReturnHome: () -> Unit = onGameEnd
) {
    val context = LocalContext.current
    val viewModel: GameViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T = GameViewModel(
                savedStateHandle = extras.createSavedStateHandle(),
                app = context.applicationContext as SpiteScoreApp
            ) as T
        }
    )

    val snackbarHostState = remember { SnackbarHostState() }

    // UI-only state — dialogs are fine to reset on rotation
    var showResetScores  by remember { mutableStateOf(false) }
    var showEndGame      by remember { mutableStateOf(false) }
    var showGameSettings by remember { mutableStateOf(false) }
    var showScoreLog     by remember { mutableStateOf(true) }
    var showPartners     by remember { mutableStateOf(false) }
    var renamingPlayer   by remember { mutableStateOf<Player?>(null) }

    // Collect one-shot navigation / error events from the ViewModel
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GameEvent.GameEnded  -> onGameEnd()
                is GameEvent.ReturnHome -> onReturnHome()
                is GameEvent.Error      -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // Two-sided Cribbage gets the split-screen board; the standard list stays one tap away.
    var useStandardLayout by rememberSaveable { mutableStateOf(false) }
    if (viewModel.isCribbageLayout && !useStandardLayout) {
        CribbageScreen(
            viewModel            = viewModel,
            snackbarHostState    = snackbarHostState,
            onShowStandardLayout = { useStandardLayout = true }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { viewModel.returnHome() }) {
                        Icon(Icons.Default.Home, contentDescription = "Save and go home")
                    }
                },
                title = {
                    Column {
                        Text(
                            viewModel.currentGameType ?: "SpiteScore",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        if (viewModel.playUntil != null) {
                            Text(
                                "Goal: ${viewModel.playUntil}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                actions = {
                    if (viewModel.isCribbageLayout) {
                        IconButton(onClick = { useStandardLayout = false }) {
                            Icon(Icons.Default.Splitscreen, contentDescription = "Cribbage board layout")
                        }
                    }
                    IconButton(onClick = { showGameSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Game Settings")
                    }
                    if (viewModel.currentGameType != null) {
                        Text(
                            "SpiteScore",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column {
                    viewModel.selectedPlayer?.let { player ->
                        HorizontalDivider()
                        ScoreInputPanel(
                            player       = player,
                            teamName     = viewModel.selectedTeamName,
                            currentScore = viewModel.scores[viewModel.primarySeat(player.seat)] ?: 0,
                            canRemove    = viewModel.players.size > 1,
                            canUndo      = viewModel.canUndo(player.seat),
                            onRename     = { renamingPlayer = player },
                            onRemove     = { viewModel.removePlayer(player) },
                            onAdd        = { n -> viewModel.recordScore(player.seat, n) },
                            onSubtract   = { n -> viewModel.recordScore(player.seat, -n) },
                            onSetTo      = { v -> viewModel.setScore(player.seat, v) },
                            onUndo       = { viewModel.undoLastScore(player.seat) }
                        )
                    }
                    Spacer(Modifier.navigationBarsPadding())
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Only show one row per team (secondary partner is folded into the primary row)
            items(
                viewModel.players.filter { viewModel.primarySeat(it.seat) == it.seat },
                key = { it.id }
            ) { player ->
                val teamName   = viewModel.teamDisplayName(player.seat)
                val isSelected = player.seat == viewModel.selectedSeat
                val isLeader   = player.seat == viewModel.leader
                val nameColor  = if (isLeader) MaterialTheme.colorScheme.primary
                                 else MaterialTheme.colorScheme.onSurface
                val delta      = viewModel.flashDelta[player.seat]

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectPlayer(player.seat) }
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        teamName,
                        style = MaterialTheme.typography.titleLarge,
                        color = nameColor
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        val score = viewModel.scores[player.seat] ?: 0
                        AnimatedContent(
                            targetState = score,
                            transitionSpec = {
                                val goingUp = targetState > initialState
                                (slideInVertically(tween(180)) { if (goingUp) -it / 2 else it / 2 } +
                                    fadeIn(tween(180))) togetherWith
                                (slideOutVertically(tween(180)) { if (goingUp) it / 2 else -it / 2 } +
                                    fadeOut(tween(180)))
                            },
                            label = "score-${player.seat}"
                        ) { s ->
                            Text(
                                "$s",
                                style = MaterialTheme.typography.headlineMedium,
                                color = nameColor
                            )
                        }
                        if (delta != null) {
                            val sign = if (delta >= 0) "+" else ""
                            Text(
                                "$sign$delta",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (delta >= 0) DeltaPositive else DeltaNegative
                            )
                        }
                    }
                }
                HorizontalDivider()
            }

            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = { viewModel.addPlayer() },
                        modifier = Modifier.weight(1f).padding(8.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Player")
                    }
                    TextButton(
                        onClick = { showPartners = true },
                        modifier = Modifier.weight(1f).padding(8.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Teams")
                    }
                }
            }

            // Score log — running history of changes this game
            if (viewModel.scoreLog.isNotEmpty()) {
                item {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Score Log",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        TextButton(onClick = { showScoreLog = !showScoreLog }) {
                            Text(if (showScoreLog) "Hide" else "Show")
                        }
                    }
                }
                if (showScoreLog) {
                    items(viewModel.scoreLog, key = { it.id }) { entry ->
                        ScoreLogRow(entry)
                    }
                }
            }

            // Goal reached banner — animates in/out when any player hits the target score
            item {
                AnimatedVisibility(
                    visible = viewModel.goalMessage != null,
                    enter   = expandVertically(tween(300)) + fadeIn(tween(300)),
                    exit    = shrinkVertically(tween(200)) + fadeOut(tween(200))
                ) {
                    viewModel.goalMessage?.let { msg ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    msg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = { showEndGame = true }) { Text("End Game") }
                            }
                        }
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showResetScores = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("Reset Scores") }

                    OutlinedButton(
                        onClick = { showEndGame = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) { Text("End Game") }
                }
            }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────

    renamingPlayer?.let { player ->
        RenameDialog(
            currentName = player.name,
            onConfirm   = { newName ->
                viewModel.renamePlayer(player, newName)
                renamingPlayer = null
            },
            onDismiss = { renamingPlayer = null }
        )
    }

    if (showResetScores) {
        ResetScoresDialog(
            onConfirm = { viewModel.resetScores(); showResetScores = false },
            onDismiss = { showResetScores = false }
        )
    }

    if (showEndGame) {
        EndGameDialog(
            players     = viewModel.players,
            scores      = viewModel.scores,
            teamNameFor = { seat -> viewModel.teamDisplayName(seat) },
            primarySeat = { seat -> viewModel.primarySeat(seat) },
            onResult    = { result -> viewModel.endGame(result) },
            onAbandon   = { viewModel.abandonGame() },
            onDismiss   = { showEndGame = false }
        )
    }

    if (showPartners) {
        ManageTeamsDialog(
            players     = viewModel.players,
            teamIdOf    = { seat -> viewModel.teams[seat] },
            onLink      = { seat, anchor -> viewModel.linkToTeam(seat, anchor) },
            onLeave     = { seat -> viewModel.leaveTeam(seat) },
            onDismiss   = { showPartners = false }
        )
    }

    if (showGameSettings) {
        GameSettingsDialog(
            currentGameType  = viewModel.currentGameType,
            currentPlayUntil = viewModel.playUntil,
            onSave = { newType, newLimit ->
                viewModel.updateGameSettings(newType, newLimit)
                showGameSettings = false
            },
            onDismiss = { showGameSettings = false }
        )
    }
}

// ── Score log row ──────────────────────────────────────────────────────────────

private val logTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

@Composable
private fun ScoreLogRow(entry: ScoreLogEntry) {
    val timeStr = remember(entry.timestamp) { logTimeFormat.format(Date(entry.timestamp)) }
    val sign    = if (entry.delta > 0) "+" else ""
    val color   = when {
        entry.isUndo   -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        entry.delta > 0 -> DeltaPositive
        else            -> DeltaNegative
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            entry.playerName,
            style    = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        if (entry.isUndo) {
            Text(
                "↩ ${sign}${entry.delta}",
                style = MaterialTheme.typography.labelLarge,
                color = color
            )
        } else {
            Text(
                "$sign${entry.delta}  →  ${entry.scoreAfter}",
                style = MaterialTheme.typography.labelLarge,
                color = color
            )
        }
        Text(
            timeStr,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            modifier = Modifier.padding(start = 12.dp)
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
}

// ── Score input panel ──────────────────────────────────────────────────────────

@Composable
private fun ScoreInputPanel(
    player: Player,
    teamName: String,
    currentScore: Int,
    canRemove: Boolean,
    canUndo: Boolean,
    onRename: () -> Unit,
    onRemove: () -> Unit,
    onAdd: (Int) -> Unit,
    onSubtract: (Int) -> Unit,
    onSetTo: (Int) -> Unit,
    onUndo: () -> Unit
) {
    val haptic      = LocalHapticFeedback.current
    val quickValues = listOf(1, 5, 10, 25)
    var selectedAmount by remember(player.id) { mutableStateOf(1) }
    var showAddDialog  by remember(player.id) { mutableStateOf(false) }
    var showSetDialog  by remember(player.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onRename,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(teamName, style = MaterialTheme.typography.titleMedium)
            }
            Row {
                IconButton(onClick = onUndo, enabled = canUndo) {
                    Icon(
                        Icons.Default.Undo,
                        contentDescription = "Undo last score",
                        tint = if (canUndo)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                    )
                }
                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Default.PersonRemove,
                            contentDescription = "Remove player",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickValues.forEach { v ->
                FilterChip(
                    selected = selectedAmount == v,
                    onClick  = { selectedAmount = v },
                    label    = {
                        Text(
                            "$v",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick  = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSubtract(selectedAmount)
                },
                modifier = Modifier.weight(1f)
            ) { Text("−", style = MaterialTheme.typography.headlineSmall) }

            Button(
                onClick  = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAdd(selectedAmount)
                },
                modifier = Modifier.weight(1f)
            ) { Text("+", style = MaterialTheme.typography.headlineSmall) }
        }

        OutlinedButton(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Add to score")
        }
        OutlinedButton(onClick = { showSetDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Change score to")
        }
    }

    if (showAddDialog) {
        AddToScoreDialog(
            playerName = player.name,
            onAdd      = { n -> onAdd(n); showAddDialog = false },
            onDismiss  = { showAddDialog = false }
        )
    }
    if (showSetDialog) {
        SetScoreDialog(
            playerName   = player.name,
            currentScore = currentScore,
            onSet        = { v -> onSetTo(v); showSetDialog = false },
            onDismiss    = { showSetDialog = false }
        )
    }
}

// ── Dialogs ────────────────────────────────────────────────────────────────────

@Composable
private fun AddToScoreDialog(
    playerName: String,
    onAdd: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to score — $playerName") },
        text  = {
            OutlinedTextField(
                value         = amount,
                onValueChange = { v ->
                    if (v.isEmpty() || v == "-" || v.toIntOrNull() != null) amount = v
                },
                placeholder   = { Text("Enter points") },
                textStyle     = MaterialTheme.typography.headlineMedium,
                modifier      = Modifier.fillMaxWidth().focusRequester(focusRequester),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction    = ImeAction.Done
                )
            )
        },
        confirmButton = {
            val n = amount.toIntOrNull() ?: 0
            Button(onClick = { if (n != 0) onAdd(n) else onDismiss() }, enabled = n != 0) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun SetScoreDialog(
    playerName: String,
    currentScore: Int,
    onSet: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Score — $playerName") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Current score: $currentScore",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                OutlinedTextField(
                    value         = value,
                    onValueChange = { v ->
                        if (v.isEmpty() || v == "-" || v.toIntOrNull() != null) value = v
                    },
                    placeholder   = { Text("New score") },
                    textStyle     = MaterialTheme.typography.headlineMedium,
                    modifier      = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction    = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { value.toIntOrNull()?.let { onSet(it) } ?: onDismiss() },
                enabled  = value.toIntOrNull() != null
            ) { Text("Set") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun RenameDialog(currentName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Rename player") },
        text    = {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                label = { Text("Name") }, singleLine = true
            )
        },
        confirmButton  = {
            TextButton(onClick = { onConfirm(text.trim().ifBlank { currentName }) }) { Text("OK") }
        },
        dismissButton  = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ResetScoresDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Reset Scores?") },
        text    = { Text("All scores will be set back to 0. Player list stays the same.") },
        confirmButton  = { Button(onClick = onConfirm) { Text("Reset") } },
        dismissButton  = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun EndGameDialog(
    players: List<Player>,
    scores: Map<Int, Int>,
    teamNameFor: (Int) -> String,
    primarySeat: (Int) -> Int,
    onResult: (GameEndResult) -> Unit,
    onAbandon: () -> Unit,
    onDismiss: () -> Unit
) {
    // Only show one row per team — filter to primary seats only
    val teams    = players
        .filter { primarySeat(it.seat) == it.seat }
        .sortedByDescending { scores[it.seat] ?: 0 }
    val topScore = teams.firstOrNull()?.let { scores[it.seat] ?: 0 } ?: 0
    val leaders  = teams.filter { (scores[it.seat] ?: 0) == topScore && topScore > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("End Game") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                teams.forEachIndexed { idx, player ->
                    val isLeader = player in leaders
                    val name     = teamNameFor(player.seat)
                    Text(
                        "${idx + 1}. $name: ${scores[player.seat] ?: 0}${if (isLeader) "  🏆" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isLeader) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                    )
                }
                HorizontalDivider()
                Text(
                    "Record this game as:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick  = { onResult(GameEndResult.NO_WIN) },
                        modifier = Modifier.weight(1f)
                    ) { Text("No Win") }
                    OutlinedButton(
                        onClick  = { onResult(GameEndResult.TIE) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Tie") }
                    Button(
                        onClick  = { onResult(GameEndResult.WIN) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Win") }
                }
                HorizontalDivider()
                OutlinedButton(
                    onClick  = onAbandon,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) { Text("Abandon (don't record)") }
            }
        },
        confirmButton  = {},
        dismissButton  = { TextButton(onClick = onDismiss) { Text("Keep Playing") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManageTeamsDialog(
    players: List<Player>,
    teamIdOf: (Int) -> Int?,
    onLink: (seat: Int, anchor: Int) -> Unit,
    onLeave: (seat: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSeat by remember { mutableStateOf<Int?>(null) }

    // Stable 1..n numbering of the live team ids
    val orderedTeamIds = players.mapNotNull { teamIdOf(it.seat) }.distinct().sorted()
    fun teamNumber(tid: Int) = orderedTeamIds.indexOf(tid) + 1

    fun tap(seat: Int) {
        val sel = selectedSeat
        when {
            sel == null -> selectedSeat = seat
            sel == seat -> selectedSeat = null
            else -> {
                // Link the solo of the pair onto the other. If both are solo this
                // forms a new team; if one is teamed, the solo joins that team.
                val selSolo  = teamIdOf(sel) == null
                val seatSolo = teamIdOf(seat) == null
                when {
                    selSolo            -> onLink(sel, seat)
                    seatSolo           -> onLink(seat, sel)
                    else               -> { /* both already teamed — remove one first */ }
                }
                selectedSeat = null
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Teams") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (selectedSeat != null)
                        "Tap another player to team them up. ✕ removes a player from their team."
                    else
                        "Tap two players to make a team. Tap a third to add them in. ✕ removes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    players.forEach { player ->
                        val tid = teamIdOf(player.seat)
                        PlayerTeamChip(
                            name      = player.name,
                            teamLabel = tid?.let { teamNumber(it).toString() },
                            teamColor = tid?.let { teamColor(teamNumber(it)) },
                            selected  = selectedSeat == player.seat,
                            onClick   = { tap(player.seat) },
                            onRemove  = if (tid != null) ({ onLeave(player.seat); selectedSeat = null }) else null
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = null
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameSettingsDialog(
    currentGameType: String?,
    currentPlayUntil: Int?,
    onSave: (gameType: String?, playUntil: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val app       = LocalContext.current.applicationContext as SpiteScoreApp
    val gameTypes = remember { app.gameTypeRepository.getTypes() }

    var gameTypeText     by remember { mutableStateOf(currentGameType ?: "") }
    var hasLimit         by remember { mutableStateOf(currentPlayUntil != null) }
    var limitText        by remember { mutableStateOf(currentPlayUntil?.toString() ?: "") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Game Settings") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value         = gameTypeText,
                        onValueChange = { gameTypeText = it },
                        label         = { Text("Game type") },
                        trailingIcon  = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                        },
                        modifier  = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        gameTypes.forEach { type ->
                            DropdownMenuItem(
                                text    = { Text(type) },
                                onClick = { gameTypeText = type; dropdownExpanded = false }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Play to score", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = hasLimit,
                        onCheckedChange = { hasLimit = it; if (!it) limitText = "" }
                    )
                }

                if (hasLimit) {
                    OutlinedTextField(
                        value         = limitText,
                        onValueChange = { limitText = it.filter { c -> c.isDigit() } },
                        label         = { Text("Target score") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val gameType = gameTypeText.trim().ifBlank { null }
                val limit    = if (hasLimit) limitText.toIntOrNull() else null
                onSave(gameType, limit)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

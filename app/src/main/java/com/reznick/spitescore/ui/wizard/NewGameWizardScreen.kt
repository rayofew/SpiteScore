package com.reznick.spitescore.ui.wizard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.reznick.spitescore.SpiteScoreApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGameWizardScreen(
    onGameStarted: (
        gameId: String,
        players: List<String>,
        playUntil: Int?,
        gameType: String?,
        partnerships: List<Pair<Int, Int>>
    ) -> Unit,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as SpiteScoreApp
    val roster = remember { app.playerRosterRepository.getPlayers() }
    val gameTypes = remember { app.gameTypeRepository.getTypes() }

    var players by remember { mutableStateOf(listOf("", "")) }
    var hasScoreLimit by remember { mutableStateOf(false) }
    var playUntil by remember { mutableStateOf("") }
    var gameType by remember { mutableStateOf("") }
    var teamsEnabled by remember { mutableStateOf(false) }
    // List of (playerIndexA, playerIndexB) pairs — indices into the `players` list
    var teamPairs by remember { mutableStateOf(listOf<Pair<Int, Int>>()) }

    // Computed at composable scope so both bottomBar and content can read it
    val hasDuplicates = players.filter { it.isNotBlank() }
        .map { it.trim().lowercase() }
        .let { names -> names.size != names.distinct().size }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Game") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        val names = players.filter { it.isNotBlank() }
                            .ifEmpty { listOf("Player 1", "Player 2") }
                        // Map wizard player indices to final seat indices (skipping blanks)
                        val validIndices = players.indices.filter { players[it].isNotBlank() }
                        val seatPairs = if (teamsEnabled) {
                            teamPairs.mapNotNull { (a, b) ->
                                val sa = validIndices.indexOf(a)
                                val sb = validIndices.indexOf(b)
                                if (sa >= 0 && sb >= 0) sa to sb else null
                            }
                        } else emptyList()
                        onGameStarted(
                            "game_${System.currentTimeMillis()}",
                            names,
                            if (hasScoreLimit) playUntil.toIntOrNull() else null,
                            gameType.trim().ifBlank { null },
                            seatPairs
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = players.any { it.isNotBlank() } && !hasDuplicates
                ) { Text("Start Game") }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("What game?", style = MaterialTheme.typography.headlineMedium)

            GameTypePickerField(
                value = gameType,
                gameTypes = gameTypes,
                onValueChange = { gameType = it }
            )

            HorizontalDivider()

            Text("Who's playing?", style = MaterialTheme.typography.titleMedium)

            players.forEachIndexed { i, name ->
                PlayerPickerField(
                    value         = name,
                    label         = "Player ${i + 1}",
                    roster        = roster,
                    excludedNames = players.filterIndexed { idx, _ -> idx != i }
                                           .filter { it.isNotBlank() },
                    onValueChange = { new -> players = players.toMutableList().also { it[i] = new } }
                )
            }

            // Duplicate-name warning
            if (hasDuplicates) {
                Text(
                    "Each player must have a unique name.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (players.size < 6) {
                    OutlinedButton(onClick = { players = players + "" }) { Text("Add player") }
                }
                if (players.size > 2) {
                    OutlinedButton(onClick = { players = players.dropLast(1) }) { Text("Remove") }
                }
            }

            if (players.filter { it.isNotBlank() }.size >= 4) {
                HorizontalDivider()
                Text("Teams (optional)", style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Play as partners", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (teamsEnabled) "Scores are shared within each team"
                            else "All players score individually",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = teamsEnabled,
                        onCheckedChange = { enabled ->
                            teamsEnabled = enabled
                            if (enabled && teamPairs.isEmpty()) {
                                // Default: pair up players in order (0&1, 2&3, …)
                                val validIndices = players.indices.filter { players[it].isNotBlank() }
                                teamPairs = (0 until validIndices.size / 2)
                                    .map { i -> validIndices[i * 2] to validIndices[i * 2 + 1] }
                            }
                            if (!enabled) teamPairs = emptyList()
                        }
                    )
                }

                if (teamsEnabled) {
                    val validIndices = players.indices.filter { players[it].isNotBlank() }

                    // Returns the partner index for [idx], if one exists
                    fun partnerOf(idx: Int): Int? {
                        val pair = teamPairs.find { (a, b) -> a == idx || b == idx }
                        return pair?.let { (a, b) -> if (a == idx) b else a }
                    }

                    // Link [idx] ↔ [partnerIdx], cascading any existing pairings for both.
                    // Pass null to clear [idx]'s partner.
                    fun setPartner(idx: Int, partnerIdx: Int?) {
                        var updated = teamPairs.filter { (a, b) -> a != idx && b != idx }
                        if (partnerIdx != null) {
                            updated = updated.filter { (a, b) -> a != partnerIdx && b != partnerIdx }
                            updated = updated + (idx to partnerIdx)
                        }
                        teamPairs = updated
                    }

                    validIndices.forEach { idx ->
                        val playerName  = players[idx].ifBlank { "Player ${idx + 1}" }
                        val partnerIdx  = partnerOf(idx)
                        val partnerName = partnerIdx?.let {
                            players.getOrNull(it)?.ifBlank { "Player ${it + 1}" }
                        }
                        val otherValid  = validIndices.filter { it != idx }
                        var expanded    by remember(idx) { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded        = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value         = partnerName ?: "No partner",
                                onValueChange = {},
                                readOnly      = true,
                                label         = { Text(playerName) },
                                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier      = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                colors = if (partnerName != null)
                                    OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor       = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        unfocusedLabelColor        = MaterialTheme.colorScheme.primary,
                                        unfocusedTrailingIconColor = MaterialTheme.colorScheme.primary
                                    )
                                else OutlinedTextFieldDefaults.colors()
                            )
                            ExposedDropdownMenu(
                                expanded         = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "No partner",
                                            color = if (partnerIdx == null)
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    },
                                    onClick = { setPartner(idx, null); expanded = false }
                                )
                                HorizontalDivider()
                                otherValid.forEach { candidateIdx ->
                                    val candidateName = players.getOrNull(candidateIdx)
                                        ?.ifBlank { "Player ${candidateIdx + 1}" } ?: "?"
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                candidateName,
                                                color = if (partnerIdx == candidateIdx)
                                                    MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            if (partnerIdx != candidateIdx) setPartner(idx, candidateIdx)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            Text("Game options", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column {
                    Text("Play to a score", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (hasScoreLimit) "Game ends at target" else "No limit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = hasScoreLimit,
                    onCheckedChange = {
                        hasScoreLimit = it
                        if (!it) playUntil = ""
                    }
                )
            }

            if (hasScoreLimit) {
                OutlinedTextField(
                    value = playUntil,
                    onValueChange = { playUntil = it.filter { c -> c.isDigit() } },
                    label = { Text("Target score") },
                    placeholder = { Text("e.g. 121") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameTypePickerField(
    value: String,
    gameTypes: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it) },
            label = { Text("Game type") },
            placeholder = { Text("e.g. Cribbage, Poker…") },
            trailingIcon = {
                if (gameTypes.isNotEmpty()) ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )
        if (gameTypes.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                gameTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = { onValueChange(type); expanded = false }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerPickerField(
    value: String,
    label: String,
    roster: List<String>,
    excludedNames: List<String> = emptyList(),
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val availableRoster = remember(roster, excludedNames) {
        val excluded = excludedNames.map { it.trim().lowercase() }.toSet()
        roster.filter { it.trim().lowercase() !in excluded }
    }
    val isDuplicate = value.isNotBlank() &&
        excludedNames.any { it.trim().lowercase() == value.trim().lowercase() }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it) },
            label = { Text(label) },
            isError = isDuplicate,
            supportingText = if (isDuplicate) {
                { Text("Name already used", color = MaterialTheme.colorScheme.error) }
            } else null,
            trailingIcon = {
                if (availableRoster.isNotEmpty()) ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )
        if (availableRoster.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Add new player") },
                    onClick = { onValueChange(""); expanded = false }
                )
                HorizontalDivider()
                availableRoster.forEach { playerName ->
                    DropdownMenuItem(
                        text = { Text(playerName) },
                        onClick = { onValueChange(playerName); expanded = false }
                    )
                }
            }
        }
    }
}

package com.reznick.spitescore.ui.wizard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.reznick.spitescore.SpiteScoreApp
import com.reznick.spitescore.data.model.Player
import com.reznick.spitescore.integration.CompanionGames

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewGameWizardScreen(
    onGameStarted: (
        gameId: String,
        players: List<String>,
        playUntil: Int?,
        gameType: String?,
        teams: List<List<Int>>
    ) -> Unit,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as SpiteScoreApp
    val roster = remember { app.playerRosterRepository.getPlayers() }
    // Companion games (played in another app) are always offered up top
    val gameTypes = remember {
        (CompanionGames.displayNames + app.gameTypeRepository.getTypes()).distinct()
    }

    var players by remember { mutableStateOf(listOf("", "")) }
    var hasScoreLimit by remember { mutableStateOf(false) }
    var playUntil by remember { mutableStateOf("") }
    var gameType by remember { mutableStateOf("") }
    var teamsEnabled by remember { mutableStateOf(false) }
    // player-row index → teamId. Indices absent from the map play solo.
    var teamAssign by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var selectedIdx by remember { mutableStateOf<Int?>(null) }
    var nextTeamId by remember { mutableStateOf(1) }
    var showInstallDialog by remember { mutableStateOf(false) }

    // Computed at composable scope so both bottomBar and content can read it
    val hasDuplicates = players.filter { it.isNotBlank() }
        .map { it.trim().lowercase() }
        .let { names -> names.size != names.distinct().size }

    // A companion game is played in a separate app, not scored here
    val companionPackage = CompanionGames.packageFor(gameType)
    val isCompanion = companionPackage != null

    // Picks player names, defaulting to two placeholders when all blank
    fun chosenNames() = players.filter { it.isNotBlank() }
        .ifEmpty { listOf("Player 1", "Player 2") }

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
                        val names = chosenNames()
                        if (isCompanion) {
                            // Hand off to the companion app (or prompt to install it)
                            val pkg = companionPackage!!
                            if (app.spiteBridge.isInstalled(pkg)) {
                                names.forEach { app.playerRosterRepository.addPlayer(it) }
                                app.gameTypeRepository.addType(gameType.trim())
                                val playerObjs = names.mapIndexed { i, n -> Player("p$i", n, i) }
                                app.spiteBridge.sendGameSetup(
                                    targetPackage = pkg,
                                    gameType = CompanionGames.tokenFor(gameType),
                                    players = playerObjs
                                )
                                onBack()
                            } else {
                                showInstallDialog = true
                            }
                            return@Button
                        }
                        // Map wizard player indices to final seat indices (skipping blanks)
                        val validIndices = players.indices.filter { players[it].isNotBlank() }
                        val seatTeams = if (teamsEnabled) {
                            teamAssign.entries
                                .filter { it.key in validIndices }
                                .groupBy({ it.value }, { it.key })
                                .values
                                .map { group -> group.mapNotNull { idx ->
                                    validIndices.indexOf(idx).takeIf { it >= 0 } } }
                                .filter { it.size >= 2 }
                        } else emptyList()
                        onGameStarted(
                            "game_${System.currentTimeMillis()}",
                            names,
                            if (hasScoreLimit) playUntil.toIntOrNull() else null,
                            gameType.trim().ifBlank { null },
                            seatTeams
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = players.any { it.isNotBlank() } && !hasDuplicates
                ) { Text(if (isCompanion) "Play in ${gameType.trim()}" else "Start Game") }
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

            if (isCompanion) {
                HorizontalDivider()
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "🎲 ${gameType.trim()} is played in the ${gameType.trim()} app. " +
                            "Tap below to launch it with these players.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (!isCompanion && players.filter { it.isNotBlank() }.size >= 4) {
                HorizontalDivider()
                Text("Teams (optional)", style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Play as teams", style = MaterialTheme.typography.bodyLarge)
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
                            selectedIdx = null
                            if (enabled && teamAssign.isEmpty()) {
                                // Sensible default: pair players up in order (1&2, 3&4, …)
                                val valid = players.indices.filter { players[it].isNotBlank() }
                                val m = mutableMapOf<Int, Int>()
                                var t = 1
                                var i = 0
                                while (i + 1 < valid.size) {
                                    m[valid[i]] = t; m[valid[i + 1]] = t; t++; i += 2
                                }
                                teamAssign = m
                                nextTeamId = t
                            }
                            if (!enabled) teamAssign = emptyMap()
                        }
                    )
                }

                if (teamsEnabled) {
                    val validIndices = players.indices.filter { players[it].isNotBlank() }
                    fun nameOf(idx: Int) =
                        players.getOrNull(idx)?.ifBlank { "Player ${idx + 1}" } ?: "Player ${idx + 1}"

                    // Stable team numbering: sort the live team ids and number them 1..n
                    val orderedTeamIds = teamAssign.entries
                        .filter { it.key in validIndices }
                        .groupBy({ it.value }, { it.key })
                        .filter { it.value.size >= 2 }
                        .keys.sorted()
                    fun teamNumber(tid: Int) = orderedTeamIds.indexOf(tid) + 1

                    fun tapPlayer(idx: Int) {
                        val sel = selectedIdx
                        when {
                            sel == null  -> { selectedIdx = idx; return }
                            sel == idx   -> { selectedIdx = null; return }
                        }
                        val a = teamAssign[sel!!]
                        val b = teamAssign[idx]
                        val m = teamAssign.toMutableMap()
                        when {
                            a == null && b == null -> {
                                val t = nextTeamId; nextTeamId += 1
                                m[sel] = t; m[idx] = t
                            }
                            a != null && b == null -> m[idx] = a
                            a == null && b != null -> m[sel] = b
                            a != null && b != null && a != b ->
                                // Merge the selected player's team into the tapped one
                                m.filter { it.value == a }.keys.forEach { m[it] = b }
                        }
                        teamAssign = m
                        selectedIdx = null
                    }

                    fun removeFromTeam(idx: Int) {
                        val t = teamAssign[idx] ?: return
                        val m = teamAssign.toMutableMap()
                        m.remove(idx)
                        val rest = m.filter { it.value == t }.keys
                        if (rest.size <= 1) rest.forEach { m.remove(it) }   // dissolve lone member
                        teamAssign = m
                        if (selectedIdx == idx) selectedIdx = null
                    }

                    Text(
                        if (selectedIdx != null)
                            "Tap another player to team up with ${nameOf(selectedIdx!!)}."
                        else
                            "Tap two players to make a team. Tap a third to add them in. ✕ removes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        validIndices.forEach { idx ->
                            val tid = teamAssign[idx]?.takeIf { it in orderedTeamIds }
                            PlayerTeamChip(
                                name      = nameOf(idx),
                                teamLabel = tid?.let { teamNumber(it).toString() },
                                teamColor = tid?.let { teamColor(teamNumber(it)) },
                                selected  = selectedIdx == idx,
                                onClick   = { tapPlayer(idx) },
                                onRemove  = if (tid != null) ({ removeFromTeam(idx) }) else null
                            )
                        }
                    }

                    // Live summary of the resulting teams
                    val groups = orderedTeamIds.map { tid ->
                        tid to validIndices.filter { teamAssign[it] == tid }.sorted()
                    }
                    val solo = validIndices.filter { teamAssign[it] !in orderedTeamIds }

                    if (groups.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            groups.forEach { (tid, members) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(teamColor(teamNumber(tid)))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Team ${teamNumber(tid)}: " +
                                            members.joinToString(" & ") { nameOf(it) },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            if (solo.isNotEmpty()) {
                                Text(
                                    "Solo: " + solo.joinToString(", ") { nameOf(it) },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        TextButton(
                            onClick = { teamAssign = emptyMap(); selectedIdx = null },
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("Clear teams") }
                    }
                }
            }

            if (!isCompanion) {
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
            } // end !isCompanion game options
        }
    }

    if (showInstallDialog && companionPackage != null) {
        val appName = gameType.trim()
        AlertDialog(
            onDismissRequest = { showInstallDialog = false },
            title = { Text("Get $appName") },
            text = {
                Text(
                    "$appName is played in its own app. Install it to play — your " +
                        "results come back to SpiteScore automatically."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showInstallDialog = false
                    app.spiteBridge.openInPlayStore(companionPackage)
                }) { Text("Install") }
            },
            dismissButton = {
                TextButton(onClick = { showInstallDialog = false }) { Text("Not now") }
            }
        )
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

// Distinct, reused across the wizard summary, the chips, and the in-game editor.
private val TeamColors = listOf(
    Color(0xFF1E88E5), // blue
    Color(0xFFE53935), // red
    Color(0xFF43A047), // green
    Color(0xFFFB8C00), // orange
    Color(0xFF8E24AA), // purple
    Color(0xFF00897B), // teal
)

/** Color for team display-number [number] (1-based), cycling if there are many teams. */
fun teamColor(number: Int): Color =
    TeamColors[(number - 1).coerceAtLeast(0) % TeamColors.size]

/** A tappable player chip showing its team color/number, with an optional ✕ to unteam. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerTeamChip(
    name: String,
    teamLabel: String?,
    teamColor: Color?,
    selected: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?
) {
    val borderColor = when {
        selected           -> MaterialTheme.colorScheme.primary
        teamColor != null  -> teamColor
        else               -> MaterialTheme.colorScheme.outline
    }
    val bg = when {
        selected           -> MaterialTheme.colorScheme.primaryContainer
        teamColor != null  -> teamColor.copy(alpha = 0.12f)
        else               -> MaterialTheme.colorScheme.surface
    }
    Surface(
        onClick = onClick,
        shape   = RoundedCornerShape(50),
        color   = bg,
        border  = BorderStroke(if (selected) 2.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (teamColor != null) {
                Box(
                    modifier = Modifier.size(10.dp).clip(CircleShape).background(teamColor)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(name, style = MaterialTheme.typography.bodyMedium)
            if (teamLabel != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    "T$teamLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = teamColor ?: MaterialTheme.colorScheme.primary
                )
            }
            if (onRemove != null) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove from team",
                    modifier = Modifier.size(16.dp).clickable { onRemove() }
                )
            }
        }
    }
}

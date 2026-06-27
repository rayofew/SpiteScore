package com.reznick.spitescore.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reznick.spitescore.BuildConfig
import com.reznick.spitescore.SpiteScoreApp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAbout: () -> Unit
) {
    val app = LocalContext.current.applicationContext as SpiteScoreApp
    val rosterRepo = app.playerRosterRepository
    val typeRepo = app.gameTypeRepository

    val rosterPlayers = remember { mutableStateListOf(*rosterRepo.getPlayers().toTypedArray()) }
    val gameTypes = remember { mutableStateListOf(*typeRepo.getTypes().toTypedArray()) }
    var newPlayerName by remember { mutableStateOf("") }
    var newGameType by remember { mutableStateOf("") }
    var showResetDialog    by remember { mutableStateOf(false) }
    var showPrivacyPolicy  by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Players", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp))
            }

            items(rosterPlayers, key = { it }) { name ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 4.dp))
                    IconButton(onClick = {
                        rosterPlayers.remove(name)
                        rosterRepo.removePlayer(name)
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove $name",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
                HorizontalDivider()
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newPlayerName,
                        onValueChange = { newPlayerName = it },
                        label = { Text("Add player") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )
                    Button(
                        onClick = {
                            val trimmed = newPlayerName.trim()
                            if (trimmed.isNotBlank() && trimmed !in rosterPlayers) {
                                rosterPlayers.add(trimmed)
                                rosterRepo.addPlayer(trimmed)
                                newPlayerName = ""
                            }
                        },
                        enabled = newPlayerName.isNotBlank()
                    ) { Text("Add") }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
            item { HorizontalDivider() }

            item {
                Text("Games", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp))
            }

            items(gameTypes, key = { it }) { type ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(type, style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 4.dp))
                    IconButton(onClick = {
                        gameTypes.remove(type)
                        typeRepo.removeType(type)
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove $type",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
                HorizontalDivider()
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newGameType,
                        onValueChange = { newGameType = it },
                        label = { Text("Add game") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )
                    Button(
                        onClick = {
                            val trimmed = newGameType.trim()
                            if (trimmed.isNotBlank() && trimmed !in gameTypes) {
                                gameTypes.add(trimmed)
                                typeRepo.addType(trimmed)
                                newGameType = ""
                            }
                        },
                        enabled = newGameType.isNotBlank()
                    ) { Text("Add") }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
            item { HorizontalDivider() }

            item {
                Text(
                    "Data",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            item {
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                ) {
                    Text("Reset Data…")
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
            item { HorizontalDivider() }

            item {
                ListItem(
                    headlineContent = { Text("Tip") },
                    supportingContent = { Text("Support the Spite line") },
                    trailingContent = {
                        Icon(Icons.Default.Launch, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    },
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://ko-fi.com/spiteapps"))
                        app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("Privacy policy") },
                    supportingContent = { Text("No data collected. Ever.") },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    },
                    modifier = Modifier.clickable { showPrivacyPolicy = true }
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("About SpiteScore") },
                    supportingContent = { Text("v${BuildConfig.VERSION_NAME}") },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    },
                    modifier = Modifier.clickable(onClick = onAbout)
                )
            }
        }
    }

    if (showPrivacyPolicy) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPrivacyPolicy = false },
            sheetState = sheetState
        ) {
            PrivacyPolicyContent()
        }
    }

    if (showResetDialog) {
        ResetDataDialog(
            onDismiss = { showResetDialog = false },
            onConfirm = { resetHistory, resetRoster, resetGameTypes ->
                scope.launch {
                    if (resetHistory) {
                        app.gameHistoryRepository.deleteAll()
                    }
                }
                if (resetRoster) {
                    rosterPlayers.clear()
                    rosterRepo.save(emptyList())
                }
                if (resetGameTypes) {
                    // Remove the prefs key so getTypes() re-seeds defaults on next call
                    app.getSharedPreferences("game_types", android.content.Context.MODE_PRIVATE)
                        .edit().remove("types").apply()
                    gameTypes.clear()
                    gameTypes.addAll(app.gameTypeRepository.getTypes())
                }
                showResetDialog = false
            }
        )
    }
}

@Composable
private fun ResetDataDialog(
    onDismiss: () -> Unit,
    onConfirm: (resetHistory: Boolean, resetRoster: Boolean, resetGameTypes: Boolean) -> Unit
) {
    var resetHistory   by remember { mutableStateOf(false) }
    var resetRoster    by remember { mutableStateOf(false) }
    var resetGameTypes by remember { mutableStateOf(false) }

    val nothingSelected = !resetHistory && !resetRoster && !resetGameTypes

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Data") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Choose what to clear. This cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))
                CheckRow("Game history & stats", resetHistory) { resetHistory = it }
                CheckRow("Player roster",        resetRoster)  { resetRoster  = it }
                CheckRow("Game types (resets to defaults)", resetGameTypes) { resetGameTypes = it }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(resetHistory, resetRoster, resetGameTypes) },
                enabled = !nothingSelected,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Reset") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun PrivacyPolicyContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Privacy Policy", style = MaterialTheme.typography.titleLarge)
        Text(
            "Last updated: May 2026",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        HorizontalDivider()

        Text(
            "SpiteScore collects absolutely no data.",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "• No analytics or usage tracking\n" +
            "• No crash reporting\n" +
            "• No Firebase, Amplitude, Sentry, or any third-party SDK\n" +
            "• No internet permission required\n" +
            "• All data (scores, player names, game history) is stored locally on your device and never transmitted anywhere",
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        Text("Data deletion", style = MaterialTheme.typography.titleMedium)
        Text(
            "Uninstalling the app deletes all data. You can also clear data at any time via Settings → Reset Data.",
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        Text("Contact", style = MaterialTheme.typography.titleMedium)
        Text(
            "Questions? Open an issue at github.com/reznick/spitescore or reach out directly.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
package com.reznick.spitescore.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reznick.spitescore.data.db.entities.GameSessionEntity
import com.reznick.spitescore.data.model.GameType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameHistoryScreen(onBack: () -> Unit) {
    var filterType by remember { mutableStateOf<GameType?>(null) }
    // In production: collected from GameHistoryViewModel
    val sessions = remember { emptyList<GameSessionEntity>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Filter chips
            GameFilterRow(filterType) { filterType = it }
            HorizontalDivider()

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No games recorded yet. Start a game to see history here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(sessions) { session ->
                        SessionRow(session)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun GameFilterRow(selected: GameType?, onSelect: (GameType?) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("All") }
            )
        }
        items(GameType.entries.size) { i ->
            val type = GameType.entries[i]
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = { Text(type.displayName) }
            )
        }
    }
}

@Composable
private fun SessionRow(session: GameSessionEntity) {
    val date = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(session.startedAt))
    val duration = formatDuration(session.durationSeconds)
    ListItem(
        headlineContent = { Text(session.gameType.lowercase().replaceFirstChar { it.uppercase() }) },
        supportingContent = { Text("$date · $duration") },
        trailingContent = {
            Text(
                session.winnerSeats.ifEmpty { "—" },
                style = MaterialTheme.typography.labelLarge
            )
        }
    )
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}

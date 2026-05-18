package com.reznick.spitescore.ui.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("SpiteScore", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Track scores for any game. Free, ad-free, telemetry-free, out of spite.",
                style = MaterialTheme.typography.bodyLarge
            )

            HorizontalDivider()

            Text("Why it exists", style = MaterialTheme.typography.titleLarge)
            Text(
                "Every existing score-tracking app either shows ads, asks for an account, " +
                "or quietly phones home. SpiteScore does none of that.",
                style = MaterialTheme.typography.bodyMedium
            )

            HorizontalDivider()

            Text("Privacy", style = MaterialTheme.typography.titleLarge)
            Text(
                "No analytics. No crash reporting. No Firebase. No nothing. " +
                "All data stays on your device.",
                style = MaterialTheme.typography.bodyMedium
            )

            HorizontalDivider()

            Text("Works with SpiteCards", style = MaterialTheme.typography.titleLarge)
            Text(
                "If you play card games with SpiteCards (same room, no internet), " +
                "final scores hand off automatically to SpiteScore. Install separately — " +
                "each app is fully standalone.",
                style = MaterialTheme.typography.bodyMedium
            )

            HorizontalDivider()

            Text("The Spite line", style = MaterialTheme.typography.titleLarge)
            Text(
                "SpiteScore is part of a small family of apps sharing the same ethos: " +
                "free, no ads, no tracking, no accounts.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text("· SpiteCards — virtual deck, links phones in the same room", style = MaterialTheme.typography.bodyMedium)
            Text("· SpitePine — map and track your pine trees", style = MaterialTheme.typography.bodyMedium)

            HorizontalDivider()

            Text(
                "Version 1.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

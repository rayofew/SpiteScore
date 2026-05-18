package com.reznick.spitescore.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onAbout: () -> Unit) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            ListItem(
                headlineContent = { Text("Donate") },
                supportingContent = { Text("Support development") }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Privacy policy") },
                supportingContent = { Text("No data collected. Ever.") }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("About SpiteScore") },
                modifier = Modifier
            )
        }
    }
}

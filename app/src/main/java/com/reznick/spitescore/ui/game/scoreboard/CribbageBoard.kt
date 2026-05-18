package com.reznick.spitescore.ui.game.scoreboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reznick.spitescore.data.model.Player

@Composable
fun CribbageBoard(
    players: List<Player>,
    scores: Map<Int, Int>,
    target: Int = 121,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text("Cribbage Board", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        players.forEach { player ->
            val score = scores[player.seat] ?: 0
            val progress = score.toFloat() / target.toFloat()
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(player.name, style = MaterialTheme.typography.bodyLarge)
                    Text("$score / $target", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(4.dp))
                // Peg track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}

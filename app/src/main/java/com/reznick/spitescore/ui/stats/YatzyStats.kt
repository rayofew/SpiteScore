package com.reznick.spitescore.ui.stats

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reznick.spitescore.data.db.entities.GameSessionEntity
import com.reznick.spitescore.integration.spitecards.PlayerScorecard
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

// ── Scorecard parsing ───────────────────────────────────────────────────────────

private val scorecardJson = Json { ignoreUnknownKeys = true }

/** The full scorecard for a session is stored in [GameSessionEntity.entriesJson]. */
fun parseScorecards(session: GameSessionEntity): List<PlayerScorecard> =
    try { scorecardJson.decodeFromString(ListSerializer(PlayerScorecard.serializer()), session.entriesJson) }
    catch (_: Exception) { emptyList() }

/** A session carries a usable scorecard breakdown (i.e. it came from a Yatzy-style companion). */
fun hasScorecards(session: GameSessionEntity): Boolean = parseScorecards(session).isNotEmpty()

/** Display order + labels for the 13 boxes. Keys match the wire format from SpiteYatzy. */
val YATZY_CATEGORIES: List<Pair<String, String>> = listOf(
    "ones" to "Ones", "twos" to "Twos", "threes" to "Threes",
    "fours" to "Fours", "fives" to "Fives", "sixes" to "Sixes",
    "threeOfAKind" to "3 of a Kind", "fourOfAKind" to "4 of a Kind",
    "fullHouse" to "Full House", "smallStraight" to "Sm. Straight",
    "largeStraight" to "Lg. Straight", "yatzy" to "Yatzy", "chance" to "Chance",
)

// ── Aggregate stats ───────────────────────────────────────────────────────────

data class YatzyPlayerStats(
    val name: String,
    val games: Int = 0,
    val totalSum: Int = 0,
    val best: Int = 0,
    val yatzies: Int = 0,          // times they scored the 50 box, incl. bonus yatzies
    val upperBonusGames: Int = 0,  // games where they cleared the upper bonus
) {
    val avg: Int get() = if (games > 0) totalSum / games else 0
    val bonusPct: Int get() = if (games > 0) upperBonusGames * 100 / games else 0
}

private fun sessionNames(session: GameSessionEntity): List<String> =
    try { scorecardJson.decodeFromString(ListSerializer(String.serializer()), session.playersJson) }
    catch (_: Exception) { session.playersJson.split(",").map { it.trim() }.filter { it.isNotBlank() } }

/** Aggregate per-player Yatzy stats across all sessions that carry scorecards. */
fun computeYatzyStats(sessions: List<GameSessionEntity>): List<YatzyPlayerStats> {
    val acc = mutableMapOf<String, YatzyPlayerStats>()
    for (session in sessions) {
        val cards = parseScorecards(session)
        if (cards.isEmpty()) continue
        val names = sessionNames(session)
        for (card in cards) {
            val name = names.getOrNull(card.seat) ?: continue
            val gotYatzy = (card.categories["yatzy"] ?: 0) >= 50
            val yatzyCount = (if (gotYatzy) 1 else 0) + card.yatzyBonus.coerceAtLeast(0)
            val cur = acc.getOrPut(name) { YatzyPlayerStats(name) }
            acc[name] = cur.copy(
                games           = cur.games + 1,
                totalSum        = cur.totalSum + card.total,
                best            = maxOf(cur.best, card.total),
                yatzies         = cur.yatzies + yatzyCount,
                upperBonusGames = cur.upperBonusGames + if (card.upperBonus > 0) 1 else 0,
            )
        }
    }
    return acc.values.sortedByDescending { it.avg }
}

// ── Yatzy stats card ───────────────────────────────────────────────────────────

@Composable
fun YatzyStatsCard(stats: List<YatzyPlayerStats>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Player",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                listOf("Avg", "Best", "🎲", "Bonus").forEach { col ->
                    Text(
                        col,
                        modifier = Modifier.width(48.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            HorizontalDivider()
            stats.forEachIndexed { idx, s ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        s.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Cell("${s.avg}")
                    Cell("${s.best}", color = MaterialTheme.colorScheme.primary)
                    Cell("${s.yatzies}")
                    Cell("${s.bonusPct}%")
                }
                if (idx < stats.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun Cell(text: String, color: androidx.compose.ui.graphics.Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface) {
    Text(
        text,
        modifier = Modifier.width(48.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        color = color
    )
}

// ── Full scorecard viewer ───────────────────────────────────────────────────────

@Composable
fun ScorecardDialog(session: GameSessionEntity, onDismiss: () -> Unit) {
    val cards = remember(session) { parseScorecards(session).sortedBy { it.seat } }
    val names = remember(session) { sessionNames(session) }
    val labelW = 116.dp
    val colW = 64.dp

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Scorecard") },
        text = {
            Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                // Header: player names
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(labelW))
                    cards.forEach { c ->
                        Text(
                            names.getOrNull(c.seat) ?: "Seat ${c.seat}",
                            modifier = Modifier.width(colW),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                // One row per category
                YATZY_CATEGORIES.forEach { (key, label) ->
                    ScoreRow(label, cards.map { it.categories[key] ?: 0 }, labelW, colW)
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                ScoreRow("Upper bonus", cards.map { it.upperBonus }, labelW, colW)
                ScoreRow("Extra Yatzies", cards.map { it.yatzyBonus }, labelW, colW)
                ScoreRow("Total", cards.map { it.total }, labelW, colW, bold = true)
            }
        }
    )
}

@Composable
private fun ScoreRow(label: String, values: List<Int>, labelW: androidx.compose.ui.unit.Dp, colW: androidx.compose.ui.unit.Dp, bold: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            label,
            modifier = Modifier.width(labelW),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        values.forEach { v ->
            Text(
                "$v",
                modifier = Modifier.width(colW),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

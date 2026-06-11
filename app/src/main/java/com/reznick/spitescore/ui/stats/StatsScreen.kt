package com.reznick.spitescore.ui.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import com.reznick.spitescore.SpiteScoreApp
import com.reznick.spitescore.data.db.entities.GameSessionEntity
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Models ────────────────────────────────────────────────────────────────────

private enum class PlayerSortBy { WINS, WIN_PCT, GAMES }

private data class PlayerStats(
    val name: String,
    val wins: Int   = 0,
    val losses: Int = 0,
    val ties: Int   = 0,
    val noWins: Int = 0
) {
    val games: Int  get() = wins + losses + ties + noWins
    val winPct: Int get() = if (games > 0) wins * 100 / games else 0
}

private sealed interface DetailTarget {
    data class Player(val name: String, val sessions: List<GameSessionEntity>) : DetailTarget
    data class H2H(val rec: H2HRecord, val sessions: List<GameSessionEntity>)  : DetailTarget
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val app      = LocalContext.current.applicationContext as SpiteScoreApp
    val sessions by app.gameHistoryRepository.observeAll().collectAsState(initial = emptyList())

    // "All" is always first; individual types follow alphabetically
    val gameTypes = remember(sessions) {
        listOf("All") + sessions.map { it.gameType }.distinct().sorted()
    }
    val sessionsByType = remember(sessions) {
        mapOf("All" to sessions) + sessions.groupBy { it.gameType }
    }

    val pagerState = rememberPagerState { gameTypes.size }
    val scope      = rememberCoroutineScope()

    var detailTarget by remember { mutableStateOf<DetailTarget?>(null) }
    var scorecardSession by remember { mutableStateOf<GameSessionEntity?>(null) }
    var yatzyDetailName by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stats") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No games recorded yet.\nFinish a game and choose Win, Tie, or No Win.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 16.dp
                ) {
                    gameTypes.forEachIndexed { index, label ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick  = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text     = { Text(label) }
                        )
                    }
                }

                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val gameType     = gameTypes[page]
                    val typeSessions = sessionsByType[gameType] ?: emptyList()

                    val rawPlayerStats = remember(typeSessions) { computePlayerStats(typeSessions) }
                    val h2hRecords     = remember(typeSessions) { computeHeadToHead(typeSessions) }
                    val scorecardSessions = remember(typeSessions) {
                        typeSessions.filter { hasScorecards(it) }.sortedByDescending { it.startedAt }
                    }
                    val yatzyStats = remember(scorecardSessions) { computeYatzyStats(scorecardSessions) }

                    var sortBy by remember { mutableStateOf(PlayerSortBy.WINS) }
                    val playerStats = remember(rawPlayerStats, sortBy) {
                        when (sortBy) {
                            PlayerSortBy.WINS    -> rawPlayerStats.sortedByDescending { it.wins }
                            PlayerSortBy.WIN_PCT -> rawPlayerStats.sortedByDescending { it.winPct }
                            PlayerSortBy.GAMES   -> rawPlayerStats.sortedByDescending { it.games }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "${typeSessions.size} game${if (typeSessions.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        if (playerStats.isNotEmpty()) {
                            item { Text("Player Records", style = MaterialTheme.typography.titleMedium) }
                            item { SortSegmentedRow(sortBy) { sortBy = it } }
                            item {
                                PlayerRecordsCard(playerStats) { name ->
                                    detailTarget = DetailTarget.Player(name, typeSessions)
                                }
                            }
                        }

                        if (h2hRecords.isNotEmpty()) {
                            item { Text("Head to Head", style = MaterialTheme.typography.titleMedium) }
                            item {
                                HeadToHeadCard(h2hRecords) { rec ->
                                    detailTarget = DetailTarget.H2H(rec, typeSessions)
                                }
                            }
                        }

                        // Yatzy-specific section: only for games that sent a full scorecard
                        if (scorecardSessions.isNotEmpty()) {
                            item { Text("Yatzy Stats", style = MaterialTheme.typography.titleMedium) }
                            item { YatzyStatsCard(yatzyStats) { name -> yatzyDetailName = name } }
                            item { Text("Scorecards", style = MaterialTheme.typography.titleMedium) }
                            items(scorecardSessions, key = { it.id }) { session ->
                                ScorecardListRow(session) { scorecardSession = session }
                            }
                        }
                    }
                }
            }
        }
    }

    detailTarget?.let { target ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { detailTarget = null },
            sheetState       = sheetState
        ) {
            when (target) {
                is DetailTarget.Player -> PlayerDetailContent(target.name, target.sessions)
                is DetailTarget.H2H   -> H2HDetailContent(target.rec, target.sessions)
            }
        }
    }

    scorecardSession?.let { session ->
        ScorecardDialog(session = session, onDismiss = { scorecardSession = null })
    }

    yatzyDetailName?.let { name ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { yatzyDetailName = null },
            sheetState       = sheetState
        ) {
            YatzyPlayerDetailContent(name, sessions)
        }
    }
}

// ── Scorecard list row ──────────────────────────────────────────────────────────

@Composable
private fun ScorecardListRow(session: GameSessionEntity, onClick: () -> Unit) {
    val winners = sessionWinners(session)
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(dateFormat.format(Date(session.startedAt)), style = MaterialTheme.typography.bodyMedium)
                if (winners.isNotEmpty()) {
                    Text(
                        "🏆 ${winners.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                "View",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ── Sort segmented button row ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortSegmentedRow(sortBy: PlayerSortBy, onSort: (PlayerSortBy) -> Unit) {
    val options = listOf(
        PlayerSortBy.WINS    to "Wins",
        PlayerSortBy.WIN_PCT to "Win %",
        PlayerSortBy.GAMES   to "Games"
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { idx, (option, label) ->
            SegmentedButton(
                selected = sortBy == option,
                onClick  = { onSort(option) },
                shape    = SegmentedButtonDefaults.itemShape(idx, options.size),
                label    = { Text(label) }
            )
        }
    }
}

// ── Player records card ───────────────────────────────────────────────────────

@Composable
private fun PlayerRecordsCard(
    playerStats: List<PlayerStats>,
    onPlayerClick: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Player",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                listOf("W", "L", "T", "GP", "Win%").forEach { col ->
                    Text(
                        col,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            HorizontalDivider()

            playerStats.forEachIndexed { idx, ps ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayerClick(ps.name) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        ps.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${ps.wins}",
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${ps.losses}",
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "${ps.ties}",
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${ps.games}",
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "${ps.winPct}%",
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (idx < playerStats.lastIndex) HorizontalDivider()
            }
        }
    }
}

// ── Head-to-head card ─────────────────────────────────────────────────────────

@Composable
private fun HeadToHeadCard(
    h2hRecords: List<H2HRecord>,
    onH2HClick: (H2HRecord) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            h2hRecords.forEachIndexed { idx, rec ->
                val leader = rec.leader()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onH2HClick(rec) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            rec.playerA,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (leader == rec.playerA) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${rec.aWins} – ${rec.bWins}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Text(
                            rec.playerB,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (leader == rec.playerB) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (rec.ties > 0) {
                        Text(
                            "${rec.ties} tie${if (rec.ties > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (idx < h2hRecords.lastIndex) HorizontalDivider()
            }
        }
    }
}

// ── Detail sheet content ──────────────────────────────────────────────────────

private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
private val json = Json { ignoreUnknownKeys = true }

private fun sessionPlayers(session: GameSessionEntity): List<String> =
    try { json.decodeFromString(ListSerializer(String.serializer()), session.playersJson) }
    catch (_: Exception) { session.playersJson.split(",").map { it.trim() }.filter { it.isNotBlank() } }

private fun sessionWinners(session: GameSessionEntity): List<String> =
    session.winnerNames.split(",").map { it.trim() }.filter { it.isNotBlank() }

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else  -> "${s}s"
    }
}

/** Returns the current streak for [playerName] as "W3", "L2", "T1", etc., or null if no sessions. */
private fun computeStreak(playerName: String, sessions: List<GameSessionEntity>): String? {
    val sorted = sessions
        .filter { playerName in sessionPlayers(it) }
        .sortedByDescending { it.startedAt }
    if (sorted.isEmpty()) return null

    var count = 0
    var streakType: String? = null

    for (session in sorted) {
        val winners = sessionWinners(session)
        val result: String? = when {
            session.variant == "TIE"                          -> "T"
            session.variant == "WIN" && playerName in winners -> "W"
            session.variant == "WIN"                          -> "L"
            else                                              -> null   // NO_WIN breaks streak
        }
        if (result == null) break
        if (streakType == null) { streakType = result; count = 1 }
        else if (result == streakType) count++
        else break
    }

    return streakType?.let { "$it$count" }
}

@Composable
private fun PlayerDetailContent(playerName: String, sessions: List<GameSessionEntity>) {
    val playerSessions = remember(sessions, playerName) {
        sessions
            .filter { playerName in sessionPlayers(it) }
            .sortedByDescending { it.startedAt }
    }
    val streak = remember(playerSessions) { computeStreak(playerName, playerSessions) }
    val streakColor = when {
        streak?.startsWith("W") == true -> MaterialTheme.colorScheme.primary
        streak?.startsWith("L") == true -> MaterialTheme.colorScheme.error
        else                            -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                playerName,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            if (streak != null) {
                Surface(
                    color  = streakColor.copy(alpha = 0.12f),
                    shape  = MaterialTheme.shapes.small
                ) {
                    Text(
                        streak,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style     = MaterialTheme.typography.labelLarge,
                        color     = streakColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Text(
            "${playerSessions.size} game${if (playerSessions.size != 1) "s" else ""}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
        )
        HorizontalDivider()

        if (playerSessions.isEmpty()) {
            Text(
                "No games recorded.",
                modifier = Modifier.padding(20.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                items(playerSessions) { session ->
                    val players  = sessionPlayers(session)
                    val winners  = sessionWinners(session)
                    val result = when (session.variant) {
                        "WIN"  -> if (playerName in winners) "Win" else "Loss"
                        "TIE"  -> "Tie"
                        else   -> "No Win"
                    }
                    val resultColor = when (result) {
                        "Win"  -> MaterialTheme.colorScheme.primary
                        "Loss" -> MaterialTheme.colorScheme.error
                        else   -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    }
                    val opponents = players.filter { it != playerName }
                    val duration  = formatDuration(session.durationSeconds)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    dateFormat.format(Date(session.startedAt)),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    duration,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Text(result, style = MaterialTheme.typography.labelLarge, color = resultColor)
                        }
                        if (opponents.isNotEmpty()) {
                            Text(
                                "vs. ${opponents.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun H2HDetailContent(rec: H2HRecord, sessions: List<GameSessionEntity>) {
    val h2hSessions = remember(sessions, rec) {
        sessions
            .filter { session ->
                val players = sessionPlayers(session)
                rec.playerA in players && rec.playerB in players
            }
            .sortedByDescending { it.startedAt }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "${rec.playerA} vs ${rec.playerB}",
            style    = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        Text(
            "${rec.aWins}–${rec.bWins}${if (rec.ties > 0) ", ${rec.ties} tied" else ""}",
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
        )
        HorizontalDivider()

        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            items(h2hSessions) { session ->
                val winners    = sessionWinners(session)
                val label = when (session.variant) {
                    "TIE"  -> "Tied"
                    "WIN"  -> when {
                        rec.playerA in winners -> "${rec.playerA} won"
                        rec.playerB in winners -> "${rec.playerB} won"
                        else                   -> "Win"
                    }
                    else   -> "No Win"
                }
                val labelColor = when (session.variant) {
                    "WIN"  -> MaterialTheme.colorScheme.primary
                    else   -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                }
                val duration = formatDuration(session.durationSeconds)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                dateFormat.format(Date(session.startedAt)),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                duration,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Text(label, style = MaterialTheme.typography.labelLarge, color = labelColor)
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

// ── Computation ───────────────────────────────────────────────────────────────

private fun computePlayerStats(sessions: List<GameSessionEntity>): List<PlayerStats> {
    val stats = mutableMapOf<String, PlayerStats>()

    for (session in sessions) {
        val players = sessionPlayers(session)
        val winners = sessionWinners(session)

        for (player in players) {
            val current = stats.getOrPut(player) { PlayerStats(player) }
            stats[player] = when {
                session.variant == "NO_WIN" -> current.copy(noWins = current.noWins + 1)
                session.variant == "TIE"    -> current.copy(ties = current.ties + 1)
                player in winners           -> current.copy(wins = current.wins + 1)
                else                        -> current.copy(losses = current.losses + 1)
            }
        }
    }

    return stats.values.sortedByDescending { it.wins }
}

// ── Head-to-head ──────────────────────────────────────────────────────────────

private data class H2HRecord(
    val playerA: String,
    val playerB: String,
    val aWins: Int = 0,
    val bWins: Int = 0,
    val ties: Int  = 0
) {
    val total: Int get() = aWins + bWins + ties
    fun leader(): String? = when {
        aWins > bWins -> playerA
        bWins > aWins -> playerB
        else          -> null
    }
}

private fun computeHeadToHead(sessions: List<GameSessionEntity>): List<H2HRecord> {
    val records = mutableMapOf<Pair<String, String>, H2HRecord>()
    fun key(a: String, b: String) = if (a <= b) a to b else b to a

    for (session in sessions) {
        if (session.variant == "NO_WIN") continue
        val players = sessionPlayers(session)
        val winners = sessionWinners(session)
        val losers  = players.filter { it !in winners }

        when (session.variant) {
            "WIN" -> {
                for (winner in winners) for (loser in losers) {
                    val k = key(winner, loser)
                    val r = records.getOrPut(k) { H2HRecord(k.first, k.second) }
                    records[k] = if (winner == k.first) r.copy(aWins = r.aWins + 1)
                                 else                   r.copy(bWins = r.bWins + 1)
                }
            }
            "TIE" -> {
                for (i in winners.indices) for (j in i + 1 until winners.size) {
                    val k = key(winners[i], winners[j])
                    val r = records.getOrPut(k) { H2HRecord(k.first, k.second) }
                    records[k] = r.copy(ties = r.ties + 1)
                }
                for (winner in winners) for (loser in losers) {
                    val k = key(winner, loser)
                    val r = records.getOrPut(k) { H2HRecord(k.first, k.second) }
                    records[k] = if (winner == k.first) r.copy(aWins = r.aWins + 1)
                                 else                   r.copy(bWins = r.bWins + 1)
                }
            }
        }
    }

    return records.values.filter { it.total > 0 }.sortedByDescending { it.total }
}

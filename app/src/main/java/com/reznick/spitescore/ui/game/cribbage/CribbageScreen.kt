package com.reznick.spitescore.ui.game.cribbage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reznick.spitescore.data.model.ScoreLogEntry
import com.reznick.spitescore.ui.game.EndGameDialog
import com.reznick.spitescore.ui.game.GameViewModel
import com.reznick.spitescore.ui.game.RenameDialog
import com.reznick.spitescore.ui.game.SetScoreDialog
import com.reznick.spitescore.ui.theme.DeltaNegative
import com.reznick.spitescore.ui.theme.DeltaPositive
import com.reznick.spitescore.ui.wizard.teamColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Split-screen cribbage scorer. The phone lies flat between the two sides: the top
 * half is rotated 180° so each person reads and taps their own half right-side up.
 * A peg track and a small control strip sit in the middle.
 *
 * Guardrails: every tap is undoable one tap at a time ("Take back"), any earlier
 * entry can be reversed from the log, the pads can be locked, a long-press on a
 * score opens direct editing, and reaching the target never records a win by itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CribbageScreen(
    viewModel: GameViewModel,
    snackbarHostState: SnackbarHostState,
    onShowStandardLayout: () -> Unit
) {
    val sides = viewModel.primarySeats
    if (sides.size != 2) return

    var swapSides   by rememberSaveable { mutableStateOf(false) }
    var locked      by rememberSaveable { mutableStateOf(false) }
    var showLog     by rememberSaveable { mutableStateOf(false) }
    var showEndGame by remember { mutableStateOf(false) }
    var editingSeat  by remember { mutableStateOf<Int?>(null) }
    var renamingSeat by remember { mutableStateOf<Int?>(null) }

    val topSeat    = if (swapSides) sides[0] else sides[1]
    val bottomSeat = if (swapSides) sides[1] else sides[0]
    val target     = viewModel.playUntil ?: CribbageRules.DEFAULT_TARGET

    val winnerSeat  = sides.firstOrNull { CribbageRules.hasWon(viewModel.scores[it] ?: 0, target) }
    val padsEnabled = !locked && winnerSeat == null

    fun colorFor(seat: Int) = teamColor(sides.indexOf(seat) + 1)

    @Composable
    fun side(seat: Int) {
        SidePanel(
            name           = viewModel.teamDisplayName(seat),
            score          = viewModel.scores[seat] ?: 0,
            flash          = viewModel.flashDelta[seat],
            isDealer       = viewModel.dealerSeat == seat,
            color          = colorFor(seat),
            takeBackAmount = viewModel.takeBackPreview(seat),
            padsEnabled    = padsEnabled,
            locked         = locked,
            onTap          = { n -> viewModel.recordScore(seat, n) },
            onTakeBack     = { viewModel.takeBack(seat) },
            onSetDealer    = { viewModel.setDealer(seat) },
            onRename       = { renamingSeat = seat },
            onEditScore    = { editingSeat = seat }
        )
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.weight(1f).fillMaxWidth().rotate(180f)) { side(topSeat) }

            CenterStrip(
                topScore     = viewModel.scores[topSeat] ?: 0,
                topBack      = CribbageRules.backPeg(viewModel.scores[topSeat] ?: 0, viewModel.flashDelta[topSeat]),
                topColor     = colorFor(topSeat),
                bottomScore  = viewModel.scores[bottomSeat] ?: 0,
                bottomBack   = CribbageRules.backPeg(viewModel.scores[bottomSeat] ?: 0, viewModel.flashDelta[bottomSeat]),
                bottomColor  = colorFor(bottomSeat),
                target       = target,
                locked       = locked,
                winnerName   = winnerSeat?.let { viewModel.teamDisplayName(it) },
                winnerScore  = winnerSeat?.let { viewModel.scores[it] ?: 0 },
                loserScore   = winnerSeat?.let { w -> viewModel.scores[sides.first { it != w }] ?: 0 },
                onHome       = { viewModel.returnHome() },
                onToggleLock = { locked = !locked },
                onShowLog    = { showLog = true },
                onSwapSides  = { swapSides = !swapSides },
                onListView   = onShowStandardLayout,
                onEndGame    = { showEndGame = true },
                onTakeBackWin = { winnerSeat?.let { viewModel.takeBack(it) } },
                onRecordWin  = { viewModel.endGame(com.reznick.spitescore.ui.game.GameEndResult.WIN) }
            )

            Box(Modifier.weight(1f).fillMaxWidth()) { side(bottomSeat) }
        }
    }

    // ── Sheets & dialogs (these do not rotate, so they stay centered) ──────

    if (showLog) {
        ModalBottomSheet(onDismissRequest = { showLog = false }) {
            ScoreLogSheet(
                entries   = viewModel.scoreLog,
                onReverse = { entry -> viewModel.reverseLogEntry(entry) }
            )
        }
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

    editingSeat?.let { seat ->
        SetScoreDialog(
            playerName   = viewModel.teamDisplayName(seat),
            currentScore = viewModel.scores[seat] ?: 0,
            onSet        = { v -> viewModel.setScore(seat, v); editingSeat = null },
            onDismiss    = { editingSeat = null }
        )
    }

    renamingSeat?.let { seat ->
        val player = viewModel.players.firstOrNull { it.seat == seat }
        if (player == null) {
            renamingSeat = null
        } else {
            RenameDialog(
                currentName = player.name,
                onConfirm   = { newName -> viewModel.renamePlayer(player, newName); renamingSeat = null },
                onDismiss   = { renamingSeat = null }
            )
        }
    }
}

// ── One side ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SidePanel(
    name: String,
    score: Int,
    flash: Int?,
    isDealer: Boolean,
    color: Color,
    takeBackAmount: Int?,
    padsEnabled: Boolean,
    locked: Boolean,
    onTap: (Int) -> Unit,
    onTakeBack: () -> Unit,
    onSetDealer: () -> Unit,
    onRename: () -> Unit,
    onEditScore: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showMore by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color.copy(alpha = 0.07f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Header: name + dealer chip on the left, big score on the right
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                TextButton(
                    onClick        = onRename,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        name,
                        style    = MaterialTheme.typography.titleLarge,
                        color    = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = onSetDealer,
                        label   = { Text(if (isDealer) "Dealer" else "Deal") },
                        leadingIcon = {
                            Icon(Icons.Default.Style, contentDescription = null, Modifier.height(16.dp))
                        },
                        colors = if (isDealer) AssistChipDefaults.assistChipColors(
                            containerColor   = color.copy(alpha = 0.18f),
                            labelColor       = MaterialTheme.colorScheme.onSurface,
                            leadingIconContentColor = color
                        ) else AssistChipDefaults.assistChipColors(
                            labelColor       = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            leadingIconContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        )
                    )
                    if (locked) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Pads locked",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            modifier = Modifier.height(18.dp)
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$score",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.combinedClickable(
                        onClick     = {},
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEditScore()
                        }
                    )
                )
                Text(
                    text  = when {
                        flash == null -> "hold to edit"
                        flash >= 0    -> "+$flash"
                        else          -> "$flash"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        flash == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        flash >= 0    -> DeltaPositive
                        else          -> DeltaNegative
                    }
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Number pad: 1–12 as three rows of four, or 13–29 as three rows of six
        val numbers = if (showMore) CribbageRules.MORE_PAD else CribbageRules.MAIN_PAD
        val perRow  = if (showMore) 6 else 4
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            numbers.chunked(perRow).forEach { row ->
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { n ->
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTap(n)
                            },
                            enabled        = padsEnabled,
                            shape          = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors         = ButtonDefaults.buttonColors(
                                containerColor = color.copy(alpha = 0.9f),
                                contentColor   = Color.White
                            ),
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Text(
                                "$n",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            // Guardrail row: take back the last tap, and the big-hand toggle
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTakeBack()
                    },
                    enabled  = takeBackAmount != null,
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(2f).fillMaxHeight()
                ) {
                    Icon(Icons.Default.Undo, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = takeBackAmount?.let { amt ->
                            if (amt >= 0) "Take back +$amt" else "Take back $amt"
                        } ?: "Nothing to take back",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick  = { showMore = !showMore },
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Text(if (showMore) "1 – 12" else "13 +")
                }
            }
        }
    }
}

// ── Center strip: peg track, controls, win banner ──────────────────────────────

@Composable
private fun CenterStrip(
    topScore: Int, topBack: Int, topColor: Color,
    bottomScore: Int, bottomBack: Int, bottomColor: Color,
    target: Int,
    locked: Boolean,
    winnerName: String?,
    winnerScore: Int?,
    loserScore: Int?,
    onHome: () -> Unit,
    onToggleLock: () -> Unit,
    onShowLog: () -> Unit,
    onSwapSides: () -> Unit,
    onListView: () -> Unit,
    onEndGame: () -> Unit,
    onTakeBackWin: () -> Unit,
    onRecordWin: () -> Unit
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 4.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
            PegTrack(
                lanes  = listOf(
                    PegLane(front = topScore,    back = topBack,    color = topColor),
                    PegLane(front = bottomScore, back = bottomBack, color = bottomColor)
                ),
                target   = target,
                modifier = Modifier.fillMaxWidth().height(58.dp)
            )

            if (winnerName != null && winnerScore != null && loserScore != null) {
                val skunk = CribbageRules.skunkLabel(target, loserScore)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "$winnerName wins $winnerScore – $loserScore",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (skunk != null) {
                                Text(
                                    skunk,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                )
                            }
                        }
                        TextButton(onClick = onTakeBackWin) { Text("Take back") }
                        Button(onClick = onRecordWin) { Text("Record win") }
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onHome) {
                        Icon(Icons.Default.Home, contentDescription = "Save and go home")
                    }
                    IconButton(onClick = onToggleLock) {
                        Icon(
                            if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (locked) "Unlock pads" else "Lock pads",
                            tint = if (locked) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onShowLog) {
                        Icon(Icons.Default.History, contentDescription = "Score log")
                    }
                    IconButton(onClick = onSwapSides) {
                        Icon(Icons.Default.SwapVert, contentDescription = "Swap sides")
                    }
                    IconButton(onClick = onListView) {
                        Icon(Icons.Default.ViewList, contentDescription = "Standard layout")
                    }
                    IconButton(onClick = onEndGame) {
                        Icon(
                            Icons.Default.Flag,
                            contentDescription = "End game",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

// ── Peg track ─────────────────────────────────────────────────────────────────

private data class PegLane(val front: Int, val back: Int, val color: Color)

@Composable
private fun PegTrack(lanes: List<PegLane>, target: Int, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val trackColor   = MaterialTheme.colorScheme.surfaceVariant
    val holeColor    = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val lineColor    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val labelStyle   = TextStyle(
        fontSize = 10.sp,
        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
    val skunk  = CribbageRules.skunkLine(target)
    val dskunk = CribbageRules.doubleSkunkLine(target)

    Canvas(modifier) {
        val labelH  = 14.dp.toPx()
        val padX    = 12.dp.toPx()
        val usableW = size.width - padX * 2
        fun x(score: Int): Float =
            padX + usableW * (score.coerceIn(0, target).toFloat() / target.toFloat())

        val laneH = (size.height - labelH) / lanes.size

        // Lines spanning both lanes
        fun marker(score: Int, label: String, dashed: Boolean) {
            val px = x(score)
            drawLine(
                color       = lineColor,
                start       = Offset(px, labelH),
                end         = Offset(px, size.height),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect  = if (dashed) PathEffect.dashPathEffect(floatArrayOf(6f, 6f)) else null
            )
            val measured = textMeasurer.measure(AnnotatedString(label), labelStyle)
            drawText(measured, topLeft = Offset(px - measured.size.width / 2f, 0f))
        }
        marker(0, "0", dashed = false)
        dskunk?.let { marker(it, "SS", dashed = true) }
        skunk?.let  { marker(it, "S",  dashed = true) }
        marker(target, "$target", dashed = false)

        lanes.forEachIndexed { i, lane ->
            val cy = labelH + laneH * i + laneH / 2f
            val h  = laneH * 0.42f
            drawRoundRect(
                color        = trackColor,
                topLeft      = Offset(padX, cy - h / 2f),
                size         = Size(usableW, h),
                cornerRadius = CornerRadius(h / 2f)
            )
            // Holes every five points, like the board
            var s = 5
            while (s < target) {
                drawCircle(holeColor, radius = 1.5.dp.toPx(), center = Offset(x(s), cy))
                s += 5
            }
            // Back peg (where the last count started)
            drawCircle(
                color  = lane.color.copy(alpha = 0.9f),
                radius = h * 0.5f,
                center = Offset(x(lane.back), cy),
                style  = Stroke(width = 2.dp.toPx())
            )
            // Front peg (current score)
            drawCircle(lane.color, radius = h * 0.62f, center = Offset(x(lane.front), cy))
            drawCircle(Color.White, radius = h * 0.2f, center = Offset(x(lane.front), cy))
        }
    }
}

// ── Score log sheet with per-entry take-back ──────────────────────────────────

private val logTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

@Composable
private fun ScoreLogSheet(entries: List<ScoreLogEntry>, onReverse: (ScoreLogEntry) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "Score log",
            style    = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        Text(
            "Tap “Take back” on any entry to reverse it.",
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
        )
        if (entries.isEmpty()) {
            Text(
                "No scores yet.",
                style    = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(20.dp)
            )
        } else {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                items(entries, key = { it.id }) { entry ->
                    LogRow(entry, onReverse = { onReverse(entry) })
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(12.dp))
    }
}

@Composable
private fun LogRow(entry: ScoreLogEntry, onReverse: () -> Unit) {
    val timeStr = remember(entry.timestamp) { logTimeFormat.format(Date(entry.timestamp)) }
    val sign    = if (entry.delta > 0) "+" else ""
    val muted   = entry.isUndo || entry.reversed
    val color   = when {
        muted           -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        entry.delta > 0 -> DeltaPositive
        else            -> DeltaNegative
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                entry.playerName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (muted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurface
            )
            Text(
                timeStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            )
        }
        Text(
            text = if (entry.isUndo) "↩ $sign${entry.delta}" else "$sign${entry.delta}  →  ${entry.scoreAfter}",
            style = MaterialTheme.typography.labelLarge,
            color = color,
            textDecoration = if (entry.reversed) TextDecoration.LineThrough else null,
            textAlign = TextAlign.End
        )
        Spacer(Modifier.width(8.dp))
        when {
            entry.isUndo   -> Spacer(Modifier.width(96.dp))
            entry.reversed -> Text(
                "taken back",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.width(96.dp),
                textAlign = TextAlign.Center
            )
            else -> TextButton(onClick = onReverse, modifier = Modifier.width(96.dp)) {
                Text("Take back")
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
}

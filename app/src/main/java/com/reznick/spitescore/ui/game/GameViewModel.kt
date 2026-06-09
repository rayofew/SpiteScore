package com.reznick.spitescore.ui.game

import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reznick.spitescore.SpiteScoreApp
import com.reznick.spitescore.data.model.Player
import com.reznick.spitescore.data.model.ScoreLogEntry
import com.reznick.spitescore.data.repository.ActiveGameState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class GameEndResult { WIN, TIE, NO_WIN }

sealed interface GameEvent {
    data object GameEnded  : GameEvent
    data object ReturnHome : GameEvent
    data class  Error(val message: String) : GameEvent
}

class GameViewModel(
    savedStateHandle: SavedStateHandle,
    private val app: SpiteScoreApp
) : ViewModel() {

    // ── Init from nav args ─────────────────────────────────────────────────

    internal val gameId: String = checkNotNull(savedStateHandle["gameId"])
    private val playersArg: String =
        Uri.decode(savedStateHandle.get<String>("players") ?: "")
    private val initialPlayerNames: List<String> =
        if (playersArg.isBlank()) listOf("Player 1", "Player 2")
        else playersArg.split("|").filter { it.isNotBlank() }

    private val repo        = app.activeGameRepository
    private val historyRepo = app.gameHistoryRepository

    private val savedState: ActiveGameState? =
        repo.load()?.takeIf { it.gameId == gameId }

    // ── Persistent game state (survives rotation) ──────────────────────────

    val players = if (savedState != null) {
        mutableStateListOf(*savedState.players.toTypedArray())
    } else {
        mutableStateListOf(*initialPlayerNames.mapIndexed { i, name ->
            Player("p$i", name.ifBlank { "Player ${i + 1}" }, i)
        }.toTypedArray())
    }

    var nextSeat by mutableIntStateOf(savedState?.nextSeat ?: initialPlayerNames.size)
        private set

    val scores = if (savedState != null) {
        mutableStateMapOf(*savedState.scores.entries
            .map { it.key.toInt() to it.value }.toTypedArray())
    } else {
        mutableStateMapOf<Int, Int>().also { m -> players.forEach { m[it.seat] = 0 } }
    }

    /** Bidirectional: seatA → seatB and seatB → seatA when they are partners. */
    val partnerships = if (savedState != null) {
        mutableStateMapOf(*savedState.partnerships.entries
            .map { it.key.toInt() to it.value.toInt() }.toTypedArray())
    } else {
        mutableStateMapOf()
    }

    var selectedSeat by mutableIntStateOf(
        players.firstOrNull()?.seat?.let { primarySeat(it) } ?: 0
    )
        private set

    val startedAt: Long = savedState?.startedAt ?: System.currentTimeMillis()

    var playUntil by mutableStateOf(savedState?.playUntil)
        private set

    var currentGameType by mutableStateOf(savedState?.gameType?.ifBlank { null })
        private set

    // ── Transient display state ────────────────────────────────────────────

    val sessionDelta = mutableStateMapOf<Int, Int>()
    val flashDelta   = mutableStateMapOf<Int, Int>()
    val scoreLog     = if (savedState != null) {
        mutableStateListOf(*savedState.scoreLog.toTypedArray())
    } else {
        mutableStateListOf()
    }

    private val _undoStacks = mutableStateMapOf<Int, List<Int>>()
    private val sessionJobs = mutableMapOf<Int, Job>()

    // ── One-shot events → composable ──────────────────────────────────────

    private val _events = Channel<GameEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // ── Partnership helpers ────────────────────────────────────────────────

    /**
     * The canonical score-holding seat for [seat]'s team.
     * Always the numerically lower seat of the two partners.
     */
    fun primarySeat(seat: Int): Int = minOf(seat, partnerships[seat] ?: seat)

    /** Human-readable name for the team that [seat] belongs to. */
    fun teamDisplayName(seat: Int): String {
        val ps          = primarySeat(seat)
        val primaryName = players.find { it.seat == ps }?.name ?: ""
        val partnerSeat = partnerships[ps]
        val partnerName = partnerSeat?.let { s -> players.find { it.seat == s }?.name }
        return if (partnerName != null) "$primaryName & $partnerName" else primaryName
    }

    // ── Computed helpers (auto-tracked in Compose) ────────────────────────

    val leader: Int?
        get() = scores.entries
            .filter { e -> players.any { it.seat == e.key } }
            .maxByOrNull { it.value }?.key
            ?.takeIf { (scores[it] ?: 0) > 0 }

    val selectedPlayer: Player?
        get() = players.find { it.seat == selectedSeat }

    val selectedTeamName: String
        get() = teamDisplayName(selectedSeat)

    val goalMessage: String?
        get() {
            val limit = playUntil ?: return null
            // Only check primary seats — secondary seats have no score entry after linking
            val achiever = players
                .filter { primarySeat(it.seat) == it.seat }
                .firstOrNull { (scores[it.seat] ?: 0) >= limit }
            return achiever?.let { "${teamDisplayName(it.seat)} hit the goal! 🎯" }
        }

    fun canUndo(seat: Int): Boolean = (_undoStacks[primarySeat(seat)]?.size ?: 0) > 0

    private fun pushUndo(seat: Int) {
        val ps    = primarySeat(seat)
        val stack = _undoStacks[ps] ?: emptyList()
        _undoStacks[ps] = (stack + (scores[ps] ?: 0)).takeLast(10)
    }

    // ── Persistence ────────────────────────────────────────────────────────

    fun saveGame() {
        repo.save(
            ActiveGameState(
                gameId       = gameId,
                players      = players.toList(),
                scores       = scores.entries.associate { (k, v) -> k.toString() to v },
                nextSeat     = nextSeat,
                playUntil    = playUntil,
                gameType     = currentGameType,
                startedAt    = startedAt,
                partnerships = partnerships.entries
                    .associate { (k, v) -> k.toString() to v.toString() },
                scoreLog     = scoreLog.toList()
            )
        )
    }

    // ── Scoring (all ops route through primarySeat) ────────────────────────

    /** Flush the open session for [seat]'s team: cancel debounce, clear delta. */
    fun commitSession(seat: Int) {
        val ps = primarySeat(seat)
        sessionJobs[ps]?.cancel()
        sessionJobs.remove(ps)
        sessionDelta.remove(ps)
    }

    /** Add [delta] to [seat]'s team score; restart the 5-second commit debounce. */
    fun recordScore(seat: Int, delta: Int) {
        val ps           = primarySeat(seat)
        val isNewSession = sessionDelta[ps] == null
        if (isNewSession) pushUndo(ps)
        scores[ps] = (scores[ps] ?: 0) + delta
        val running = (sessionDelta[ps] ?: 0) + delta
        sessionDelta[ps] = running
        flashDelta[ps]   = running

        // Log: one entry per session, updated in-place while the session is open
        val name = teamDisplayName(ps)
        if (isNewSession) {
            scoreLog.add(0, ScoreLogEntry(seat = ps, playerName = name,
                delta = running, scoreAfter = scores[ps] ?: 0))
            if (scoreLog.size > 50) scoreLog.removeAt(scoreLog.lastIndex)
        } else {
            val idx = scoreLog.indexOfFirst { it.seat == ps && !it.isUndo }
            if (idx >= 0) scoreLog[idx] = scoreLog[idx].copy(
                delta = running, scoreAfter = scores[ps] ?: 0)
        }

        sessionJobs[ps]?.cancel()
        sessionJobs[ps] = viewModelScope.launch {
            delay(5_000)
            commitSession(ps)
            saveGame()
        }
    }

    /** Absolutely set [seat]'s team score to [value]. */
    fun setScore(seat: Int, value: Int) {
        val ps   = primarySeat(seat)
        pushUndo(ps)
        commitSession(ps)
        val diff = value - (scores[ps] ?: 0)
        scores[ps]     = value
        flashDelta[ps] = diff
        if (diff != 0) {
            val name = teamDisplayName(ps)
            scoreLog.add(0, ScoreLogEntry(seat = ps, playerName = name,
                delta = diff, scoreAfter = value))
            if (scoreLog.size > 50) scoreLog.removeAt(scoreLog.lastIndex)
        }
        saveGame()
    }

    /** Reverse the last scored session for [seat]'s team. */
    fun undoLastScore(seat: Int) {
        val ps    = primarySeat(seat)
        val stack = _undoStacks[ps]?.takeIf { it.isNotEmpty() } ?: return
        val previous = stack.last()
        _undoStacks[ps] = stack.dropLast(1)
        commitSession(ps)
        val diff = previous - (scores[ps] ?: 0)
        scores[ps] = previous
        if (diff != 0) {
            flashDelta[ps] = diff
            val name = teamDisplayName(ps)
            scoreLog.add(0, ScoreLogEntry(seat = ps, playerName = name,
                delta = diff, scoreAfter = previous, isUndo = true))
            if (scoreLog.size > 50) scoreLog.removeAt(scoreLog.lastIndex)
        }
        saveGame()
    }

    // ── Player management ──────────────────────────────────────────────────

    fun selectPlayer(seat: Int) {
        val ps = primarySeat(seat)
        if (ps == selectedSeat) return
        commitSession(selectedSeat)
        saveGame()
        selectedSeat = ps
    }

    fun addPlayer() {
        val seat = nextSeat++
        players.add(Player("p$seat", "Player ${players.size + 1}", seat))
        scores[seat] = 0
        selectedSeat = seat
        saveGame()
    }

    fun removePlayer(player: Player) {
        // Unlink first so both sides get independent score entries
        if (partnerships.containsKey(player.seat)) unlinkPlayers(player.seat)
        commitSession(player.seat)
        val fallback = players
            .filter { it.seat != player.seat }
            .map { primarySeat(it.seat) }
            .distinct()
            .firstOrNull() ?: -1
        players.remove(player)
        scores.remove(player.seat)
        _undoStacks.remove(player.seat)
        selectedSeat = fallback
        saveGame()
    }

    fun renamePlayer(player: Player, newName: String) {
        val idx = players.indexOfFirst { it.id == player.id }
        if (idx >= 0) players[idx] = players[idx].copy(name = newName)
        saveGame()
    }

    fun resetScores() {
        // Only reset primary seats (secondary seats have no score entry)
        players.forEach { if (primarySeat(it.seat) == it.seat) scores[it.seat] = 0 }
        flashDelta.clear()
        sessionDelta.clear()
        scoreLog.clear()
        _undoStacks.clear()
        sessionJobs.values.forEach { it.cancel() }
        sessionJobs.clear()
        saveGame()
    }

    // ── Partnership management ─────────────────────────────────────────────

    /**
     * Link [seatA] and [seatB] as partners. Their scores are merged into the
     * lower-numbered seat (the primary). Both must currently be unlinked.
     */
    fun linkPlayers(seatA: Int, seatB: Int) {
        if (seatA == seatB) return
        if (partnerships.containsKey(seatA) || partnerships.containsKey(seatB)) return

        val primary   = minOf(seatA, seatB)
        val secondary = maxOf(seatA, seatB)

        commitSession(seatA)
        commitSession(seatB)

        // Merge scores into primary seat
        val combined = (scores[primary] ?: 0) + (scores[secondary] ?: 0)
        scores[primary] = combined
        scores.remove(secondary)
        flashDelta.remove(secondary)

        partnerships[seatA] = seatB
        partnerships[seatB] = seatA

        if (selectedSeat == secondary) selectedSeat = primary
        saveGame()
    }

    /**
     * Remove the partnership for [seat]'s team. Both players keep the shared
     * score and resume as independent scorers.
     */
    fun unlinkPlayers(seat: Int) {
        val partner   = partnerships[seat] ?: return
        val primary   = primarySeat(seat)
        val secondary = if (seat == primary) partner else seat
        val shared    = scores[primary] ?: 0

        partnerships.remove(seat)
        partnerships.remove(partner)

        scores[primary]   = shared
        scores[secondary] = shared
        saveGame()
    }

    // ── Game settings ──────────────────────────────────────────────────────

    fun updateGameSettings(gameType: String?, playUntil: Int?) {
        currentGameType = gameType
        this.playUntil  = playUntil
        saveGame()
    }

    // ── Game end ───────────────────────────────────────────────────────────

    fun endGame(result: GameEndResult) {
        // Work with teams only (primary seats)
        val teams    = players
            .filter { primarySeat(it.seat) == it.seat }
            .sortedByDescending { scores[it.seat] ?: 0 }
        val topScore = teams.firstOrNull()?.let { scores[it.seat] ?: 0 } ?: 0
        val leaders  = teams.filter { (scores[it.seat] ?: 0) == topScore && topScore > 0 }
        val winnerNames = when (result) {
            GameEndResult.WIN, GameEndResult.TIE -> leaders.map { teamDisplayName(it.seat) }
            GameEndResult.NO_WIN                 -> emptyList()
        }
        val allTeamNames = teams.map { teamDisplayName(it.seat) }

        viewModelScope.launch {
            try {
                historyRepo.saveSimpleResult(
                    gameId      = gameId,
                    gameType    = currentGameType ?: "Unknown",
                    playerNames = allTeamNames,
                    winnerNames = winnerNames,
                    resultType  = result.name,
                    startedAt   = startedAt
                )
                repo.clear()
                _events.send(GameEvent.GameEnded)
            } catch (e: Exception) {
                _events.send(GameEvent.Error("Couldn't save result — please try again."))
            }
        }
    }

    fun abandonGame() {
        repo.clear()
        viewModelScope.launch { _events.send(GameEvent.GameEnded) }
    }

    fun returnHome() {
        players.forEach { commitSession(it.seat) }
        saveGame()
        viewModelScope.launch { _events.send(GameEvent.ReturnHome) }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        sessionJobs.values.forEach { it.cancel() }
    }
}

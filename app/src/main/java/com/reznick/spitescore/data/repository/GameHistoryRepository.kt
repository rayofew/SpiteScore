package com.reznick.spitescore.data.repository

import com.reznick.spitescore.data.db.GameSessionDao
import com.reznick.spitescore.data.db.entities.GameSessionEntity
import com.reznick.spitescore.integration.spitecards.GameResultPayload
import com.reznick.spitescore.integration.spitecards.PlayerScorecard
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

class GameHistoryRepository(private val dao: GameSessionDao) {

    fun observeAll(): Flow<List<GameSessionEntity>> = dao.observeAll()

    fun observeByType(type: String): Flow<List<GameSessionEntity>> = dao.observeByType(type)

    suspend fun saveSimpleResult(
        gameId: String,
        gameType: String,
        playerNames: List<String>,
        winnerNames: List<String>,
        resultType: String,
        startedAt: Long
    ) {
        val now = System.currentTimeMillis()
        dao.insert(
            GameSessionEntity(
                id              = gameId,
                gameType        = gameType,
                variant         = resultType,
                configJson      = gameType,
                playersJson     = Json.encodeToString(ListSerializer(String.serializer()), playerNames),
                entriesJson     = "[]",
                winnerNames     = winnerNames.joinToString(","),
                startedAt       = startedAt,
                endedAt         = now,
                durationSeconds = (now - startedAt) / 1000
            )
        )
    }

    /**
     * Persist a game result handed back from a companion app (e.g. SpiteYatzy). Maps it into
     * the same shape internal games use so it shows up in Stats (W/L, head-to-head, streaks),
     * and stashes the full per-player scorecard in [GameSessionEntity.entriesJson] for the
     * detailed viewer. Idempotent: the same payload REPLACEs rather than duplicates.
     */
    suspend fun saveReceivedResult(payload: GameResultPayload) {
        val ordered     = payload.players.sortedBy { it.seat }
        val playerNames = ordered.map { it.name }

        // A game with only one player is practice: keep the score and scorecard for personal
        // bests, but don't record it as a win or a loss. NO_WIN is neutral in the stats math
        // (counts as "games played" only). Two+ players resolve a winner as usual.
        val isPractice  = payload.players.size < 2
        val winnerNames = if (isPractice) emptyList() else payload.players
            .filter { it.seat in payload.winnerSeats }
            .map { it.name }

        val endedAt = runCatching { Instant.parse(payload.playedAt).toEpochMilli() }
            .getOrDefault(System.currentTimeMillis())
        val startedAt = endedAt - payload.durationSeconds * 1000

        dao.insert(
            GameSessionEntity(
                id              = "${payload.sourceApp}_${payload.playedAt}",
                gameType        = payload.gameType.replaceFirstChar { it.uppercase() },
                variant         = if (isPractice) "NO_WIN" else "WIN",
                configJson      = payload.gameType,
                playersJson     = Json.encodeToString(ListSerializer(String.serializer()), playerNames),
                entriesJson     = Json.encodeToString(
                    ListSerializer(PlayerScorecard.serializer()), payload.scorecards
                ),
                winnerNames     = winnerNames.joinToString(","),
                startedAt       = startedAt,
                endedAt         = endedAt,
                durationSeconds = payload.durationSeconds
            )
        )
    }

    suspend fun delete(id: String)  = dao.delete(id)
    suspend fun deleteAll()         = dao.deleteAll()
}

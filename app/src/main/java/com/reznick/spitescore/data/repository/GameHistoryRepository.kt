package com.reznick.spitescore.data.repository

import com.reznick.spitescore.data.db.GameSessionDao
import com.reznick.spitescore.data.db.entities.GameSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

    suspend fun delete(id: String)  = dao.delete(id)
    suspend fun deleteAll()         = dao.deleteAll()
}

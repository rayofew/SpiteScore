package com.reznick.spitescore.data.repository

import com.reznick.spitescore.data.db.GameSessionDao
import com.reznick.spitescore.data.db.entities.GameSessionEntity
import com.reznick.spitescore.data.model.ActiveGame
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GameHistoryRepository(private val dao: GameSessionDao) {

    fun observeAll(): Flow<List<GameSessionEntity>> = dao.observeAll()

    fun observeByType(type: String): Flow<List<GameSessionEntity>> = dao.observeByType(type)

    suspend fun save(game: ActiveGame, endedAt: Long) {
        dao.insert(
            GameSessionEntity(
                id = game.id,
                gameType = game.config.gameType.name,
                variant = game.config.variant,
                configJson = Json.encodeToString(game.config),
                playersJson = Json.encodeToString(game.players),
                entriesJson = Json.encodeToString(game.entries),
                winnerSeats = game.winnerSeats.joinToString(","),
                startedAt = game.startedAt,
                endedAt = endedAt,
                durationSeconds = (endedAt - game.startedAt) / 1000
            )
        )
    }

    suspend fun delete(id: String) = dao.delete(id)
}

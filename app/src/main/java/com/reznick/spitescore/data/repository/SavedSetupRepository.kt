package com.reznick.spitescore.data.repository

import com.reznick.spitescore.data.db.SavedSetupDao
import com.reznick.spitescore.data.db.entities.SavedSetupEntity
import com.reznick.spitescore.data.model.SavedSetup
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SavedSetupRepository(private val dao: SavedSetupDao) {

    fun observeAll(): Flow<List<SavedSetupEntity>> = dao.observeAll()

    suspend fun save(setup: SavedSetup) {
        dao.insert(
            SavedSetupEntity(
                id = setup.id,
                name = setup.name,
                configJson = Json.encodeToString(setup.config),
                defaultPlayersJson = Json.encodeToString(setup.defaultPlayers),
                createdAt = setup.createdAt
            )
        )
    }

    suspend fun delete(id: String) = dao.delete(id)
}

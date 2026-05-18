package com.reznick.spitescore.data.db

import androidx.room.*
import com.reznick.spitescore.data.db.entities.GameSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameSessionDao {
    @Query("SELECT * FROM game_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<GameSessionEntity>>

    @Query("SELECT * FROM game_sessions WHERE gameType = :type ORDER BY startedAt DESC")
    fun observeByType(type: String): Flow<List<GameSessionEntity>>

    @Query("SELECT * FROM game_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GameSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: GameSessionEntity)

    @Query("DELETE FROM game_sessions WHERE id = :id")
    suspend fun delete(id: String)
}

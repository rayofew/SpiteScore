package com.reznick.spitescore.data.db

import androidx.room.*
import com.reznick.spitescore.data.db.entities.SavedSetupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedSetupDao {
    @Query("SELECT * FROM saved_setups ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SavedSetupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setup: SavedSetupEntity)

    @Query("DELETE FROM saved_setups WHERE id = :id")
    suspend fun delete(id: String)
}

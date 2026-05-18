package com.reznick.spitescore.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.reznick.spitescore.data.db.entities.GameSessionEntity
import com.reznick.spitescore.data.db.entities.SavedSetupEntity

@Database(
    entities = [GameSessionEntity::class, SavedSetupEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SpiteScoreDatabase : RoomDatabase() {
    abstract fun gameSessionDao(): GameSessionDao
    abstract fun savedSetupDao(): SavedSetupDao

    companion object {
        @Volatile private var INSTANCE: SpiteScoreDatabase? = null

        fun get(context: Context): SpiteScoreDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                SpiteScoreDatabase::class.java,
                "spitescore.db"
            ).build().also { INSTANCE = it }
        }
    }
}

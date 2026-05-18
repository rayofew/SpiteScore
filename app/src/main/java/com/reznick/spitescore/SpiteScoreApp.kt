package com.reznick.spitescore

import android.app.Application
import com.reznick.spitescore.data.db.SpiteScoreDatabase
import com.reznick.spitescore.data.repository.GameHistoryRepository
import com.reznick.spitescore.data.repository.SavedSetupRepository
import com.reznick.spitescore.integration.spitecards.SpiteCardsBridge

class SpiteScoreApp : Application() {
    val database by lazy { SpiteScoreDatabase.get(this) }
    val gameHistoryRepository by lazy { GameHistoryRepository(database.gameSessionDao()) }
    val savedSetupRepository by lazy { SavedSetupRepository(database.savedSetupDao()) }
    val spiteCardsBridge by lazy { SpiteCardsBridge(this) }
}

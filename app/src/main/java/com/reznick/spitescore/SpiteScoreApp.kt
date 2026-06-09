package com.reznick.spitescore

import android.app.Application
import com.reznick.spitescore.data.db.SpiteScoreDatabase
import com.reznick.spitescore.data.repository.ActiveGameRepository
import com.reznick.spitescore.data.repository.GameHistoryRepository
import com.reznick.spitescore.data.repository.GameTypeRepository
import com.reznick.spitescore.data.repository.PlayerRosterRepository
import com.reznick.spitescore.data.repository.SavedSetupRepository
import com.reznick.spitescore.data.repository.ThemeRepository
import com.reznick.spitescore.integration.spitecards.SpiteBridge

class SpiteScoreApp : Application() {
    val database by lazy { SpiteScoreDatabase.get(this) }
    val gameHistoryRepository by lazy { GameHistoryRepository(database.gameSessionDao()) }
    val savedSetupRepository by lazy { SavedSetupRepository(database.savedSetupDao()) }
    val spiteBridge by lazy { SpiteBridge(this) }
    val activeGameRepository by lazy { ActiveGameRepository(this) }
    val playerRosterRepository by lazy { PlayerRosterRepository(this) }
    val gameTypeRepository by lazy { GameTypeRepository(this) }
    val themeRepository by lazy { ThemeRepository(this) }
}

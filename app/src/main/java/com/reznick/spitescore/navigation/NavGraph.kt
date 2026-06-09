package com.reznick.spitescore.navigation

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.reznick.spitescore.SpiteScoreApp
import com.reznick.spitescore.data.model.Player
import com.reznick.spitescore.data.repository.ActiveGameState
import com.reznick.spitescore.data.repository.ThemeMode
// Uri is still used by Screen.Game.route() helper above
import com.reznick.spitescore.integration.spitecards.GameResultPayload
import com.reznick.spitescore.ui.about.AboutScreen
import com.reznick.spitescore.ui.game.GameScreen
import com.reznick.spitescore.ui.home.HomeScreen
import com.reznick.spitescore.ui.settings.SettingsScreen
import com.reznick.spitescore.ui.stats.StatsScreen
import com.reznick.spitescore.ui.wizard.NewGameWizardScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object NewGameWizard : Screen("wizard")
    object Game : Screen("game/{gameId}/{players}") {
        fun route(gameId: String, players: List<String>) =
            "game/$gameId/${Uri.encode(players.joinToString("|"))}"
    }
    object Stats : Screen("stats")
    object Settings : Screen("settings")
    object About : Screen("about")
}

@Composable
fun SpiteScoreNavHost(
    incomingResult: GameResultPayload? = null,
    currentTheme: ThemeMode = ThemeMode.SYSTEM,
    onThemeChange: (ThemeMode) -> Unit = {}
) {
    val navController = rememberNavController()

    NavHost(
        navController      = navController,
        startDestination   = Screen.Home.route,
        enterTransition    = { fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.95f) },
        exitTransition     = { fadeOut(tween(150)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition  = { fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.95f) }
    ) {
        composable(Screen.Home.route) {
            val app = LocalContext.current.applicationContext as SpiteScoreApp
            val repo = app.activeGameRepository
            val scope = rememberCoroutineScope()
            HomeScreen(
                incomingResult = incomingResult,
                hasSavedGame = repo.hasGame(),
                onNewGame = {
                    repo.clear()
                    navController.navigate(Screen.NewGameWizard.route)
                },
                onContinueGame = {
                    val saved = repo.load()
                    if (saved != null) {
                        navController.navigate(Screen.Game.route(saved.gameId, saved.players.map { it.name }))
                    }
                },
                onSaveResult = {
                    incomingResult?.let { result ->
                        scope.launch { app.gameHistoryRepository.saveReceivedResult(result) }
                    }
                },
                onStats = { navController.navigate(Screen.Stats.route) },
                onSettings = { navController.navigate(Screen.Settings.route) },
                onAbout = { navController.navigate(Screen.About.route) }
            )
        }
        composable(Screen.NewGameWizard.route) {
            val app = LocalContext.current.applicationContext as SpiteScoreApp
            NewGameWizardScreen(
                onGameStarted = { gameId, playerNames, playUntil, gameType, teamGroups ->
                    val playerList = playerNames.mapIndexed { i, name -> Player("p$i", name, i) }
                    // Each group is a list of seat indices; flatten to seat → teamId
                    val teamMap = teamGroups
                        .flatMapIndexed { idx, group ->
                            group.map { seat -> seat.toString() to (idx + 1) }
                        }
                        .toMap()
                    app.activeGameRepository.save(
                        ActiveGameState(
                            gameId       = gameId,
                            players      = playerList,
                            scores       = playerList.associate { it.seat.toString() to 0 },
                            nextSeat     = playerList.size,
                            playUntil    = playUntil,
                            gameType     = gameType,
                            startedAt    = System.currentTimeMillis(),
                            teams        = teamMap
                        )
                    )
                    playerNames.forEach { app.playerRosterRepository.addPlayer(it) }
                    if (!gameType.isNullOrBlank()) app.gameTypeRepository.addType(gameType)
                    navController.navigate(Screen.Game.route(gameId, playerNames))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Game.route,
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType },
                navArgument("players") { type = NavType.StringType }
            )
        ) {
            // gameId and players are read by GameViewModel via SavedStateHandle automatically
            val goHome: () -> Unit = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            }
            GameScreen(
                onGameEnd    = goHome,
                onReturnHome = goHome
            )
        }
        composable(Screen.Stats.route) {
            StatsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onAbout = { navController.navigate(Screen.About.route) },
                currentTheme = currentTheme,
                onThemeChange = onThemeChange
            )
        }
        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
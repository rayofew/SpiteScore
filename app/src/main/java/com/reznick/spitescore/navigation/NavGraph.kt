package com.reznick.spitescore.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.reznick.spitescore.integration.spitecards.GameResultPayload
import com.reznick.spitescore.ui.about.AboutScreen
import com.reznick.spitescore.ui.game.GameScreen
import com.reznick.spitescore.ui.history.GameHistoryScreen
import com.reznick.spitescore.ui.home.HomeScreen
import com.reznick.spitescore.ui.settings.SettingsScreen
import com.reznick.spitescore.ui.wizard.NewGameWizardScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object NewGameWizard : Screen("wizard")
    object Game : Screen("game/{gameId}/{players}") {
        fun route(gameId: String, players: List<String>) =
            "game/$gameId/${Uri.encode(players.joinToString("|"))}"
    }
    object GameHistory : Screen("history")
    object Settings : Screen("settings")
    object About : Screen("about")
}

@Composable
fun SpiteScoreNavHost(incomingResult: GameResultPayload? = null) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                incomingResult = incomingResult,
                onNewGame = { navController.navigate(Screen.NewGameWizard.route) },
                onHistory = { navController.navigate(Screen.GameHistory.route) },
                onSettings = { navController.navigate(Screen.Settings.route) },
                onAbout = { navController.navigate(Screen.About.route) }
            )
        }
        composable(Screen.NewGameWizard.route) {
            NewGameWizardScreen(
                onGameStarted = { gameId, players ->
                    navController.navigate(Screen.Game.route(gameId, players))
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
        ) { backStack ->
            val gameId = backStack.arguments?.getString("gameId") ?: return@composable
            val playersArg = Uri.decode(backStack.arguments?.getString("players") ?: "")
            val playerNames = if (playersArg.isBlank()) listOf("Player 1", "Player 2")
                              else playersArg.split("|").filter { it.isNotBlank() }
            GameScreen(
                gameId = gameId,
                playerNames = playerNames,
                onGameEnd = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.GameHistory.route) {
            GameHistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onAbout = { navController.navigate(Screen.About.route) }
            )
        }
        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}

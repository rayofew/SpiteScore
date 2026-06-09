package com.reznick.spitescore.integration

/**
 * Registry of games that are played in a separate "Spite" companion app rather
 * than scored inside SpiteScore. Keyed off the free-text game-type name the user
 * picks in the new-game flow, so it stays decoupled from the [GameType] enum.
 */
object CompanionGames {

    const val YATZY_PACKAGE = "com.reznick.spiteyatzy"

    /** Names surfaced as suggestions in the game-type picker. */
    val displayNames: List<String> = listOf("Yatzy")

    /** The companion app's package for [gameTypeName], or null if it's scored in-app. */
    fun packageFor(gameTypeName: String): String? =
        when (gameTypeName.trim().lowercase()) {
            "yatzy" -> YATZY_PACKAGE
            else    -> null
        }

    /** The lowercase protocol token sent to the companion app for [gameTypeName]. */
    fun tokenFor(gameTypeName: String): String = gameTypeName.trim().lowercase()
}

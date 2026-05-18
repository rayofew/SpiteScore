package com.reznick.spitescore.integration.spitecards

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.reznick.spitescore.data.model.ActiveGame
import com.reznick.spitescore.data.model.GameType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val SPITECARDS_PACKAGE = "com.reznick.spitecards"
private const val ACTION_RECEIVE_SETUP = "com.reznick.spite.action.START_GAME_SESSION"
private const val ACTION_RECEIVE_RESULT = "com.reznick.spite.action.RECEIVE_GAME_RESULT"
private const val MIME_SETUP = "application/vnd.spite.gamesetup+json"
private const val MIME_RESULT = "application/vnd.spite.gameresult+json"
private const val EXTRA_PAYLOAD = "payload"

private val json = Json { ignoreUnknownKeys = true }

class SpiteCardsBridge(private val context: Context) {

    fun isSpiteCardsInstalled(): Boolean {
        val intent = Intent(ACTION_RECEIVE_SETUP).apply {
            setPackage(SPITECARDS_PACKAGE)
            type = MIME_SETUP
        }
        return context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
    }

    fun sendGameSetup(game: ActiveGame): Result<Unit> = runCatching {
        require(game.config.gameType.supportsSpiteCards) { "${game.config.gameType} doesn't have a SpiteCards equivalent." }
        val payload = GameSetupPayload(
            gameType = game.config.gameType.name.lowercase(),
            variant = game.config.variant.lowercase(),
            ruleOptions = buildMap {
                put("shortGame", game.config.shortGame)
                put("muggins", game.config.muggins)
            },
            players = game.players.map { SetupPlayer(it.name) }
        )
        val intent = Intent(ACTION_RECEIVE_SETUP).apply {
            setPackage(SPITECARDS_PACKAGE)
            type = MIME_SETUP
            putExtra(EXTRA_PAYLOAD, json.encodeToString(payload))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun parseIncomingGameResult(intent: Intent): Result<GameResultPayload> = runCatching {
        val raw = intent.getStringExtra(EXTRA_PAYLOAD)
            ?: error("Missing payload extra in incoming intent.")
        val result = json.decodeFromString<GameResultPayload>(raw)
        if (!SchemaVersion.isCompatible(result.schemaVersion)) {
            error(SchemaVersion.incompatibleMessage(result.schemaVersion))
        }
        result
    }

    fun openSpiteCardsInPlayStore() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("market://details?id=$SPITECARDS_PACKAGE")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}

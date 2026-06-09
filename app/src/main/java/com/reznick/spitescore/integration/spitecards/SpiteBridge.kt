package com.reznick.spitescore.integration.spitecards

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.reznick.spitescore.data.model.Player
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val ACTION_RECEIVE_SETUP = "com.reznick.spite.action.START_GAME_SESSION"
private const val MIME_SETUP = "application/vnd.spite.gamesetup+json"
private const val EXTRA_PAYLOAD = "payload"

private val json = Json { ignoreUnknownKeys = true }

/**
 * Bridge to any "Spite" companion app (SpiteYatzy, SpiteCards, …) over the shared
 * `com.reznick.spite.action.*` intent protocol. The target app is passed per call
 * so a single bridge serves the whole ecosystem.
 */
class SpiteBridge(private val context: Context) {

    fun isInstalled(targetPackage: String): Boolean {
        val intent = Intent(ACTION_RECEIVE_SETUP).apply {
            setPackage(targetPackage)
            type = MIME_SETUP
        }
        return context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .isNotEmpty()
    }

    fun sendGameSetup(
        targetPackage: String,
        gameType: String,
        players: List<Player>,
        variant: String = "",
        ruleOptions: Map<String, Boolean> = emptyMap()
    ): Result<Unit> = runCatching {
        val payload = GameSetupPayload(
            gameType = gameType,
            variant = variant,
            ruleOptions = ruleOptions,
            players = players.map { SetupPlayer(name = it.name, id = it.id) }
        )
        val intent = Intent(ACTION_RECEIVE_SETUP).apply {
            setPackage(targetPackage)
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

    fun openInPlayStore(targetPackage: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("market://details?id=$targetPackage")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}

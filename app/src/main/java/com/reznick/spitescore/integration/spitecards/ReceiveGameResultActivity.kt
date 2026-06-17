package com.reznick.spitescore.integration.spitecards

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.reznick.spitescore.MainActivity
import com.reznick.spitescore.SpiteScoreApp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/** Intent extra (boolean): import the result in the background and never show SpiteScore. */
const val EXTRA_SILENT = "silent"

/**
 * Receives a finished game from a companion app (SpiteYatzy, SpiteCards, …).
 *
 * Default: hand the result to [MainActivity], which displays it and saves it.
 * When the sender sets [EXTRA_SILENT] (SpiteYatzy's "Done"): persist the result in the
 * background and finish without ever showing SpiteScore, so the sender stays in front.
 */
class ReceiveGameResultActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = applicationContext as SpiteScoreApp
        val parsed = app.spiteBridge.parseIncomingGameResult(intent)

        if (intent.getBooleanExtra(EXTRA_SILENT, false)) {
            parsed.onSuccess { result ->
                // App-lifetime scope: the save completes even though this activity finishes now.
                app.applicationScope.launch {
                    runCatching { app.gameHistoryRepository.saveReceivedResult(result) }
                }
            }
            finish() // never bring SpiteScore to the foreground
            return
        }

        parsed
            .onSuccess { result ->
                startActivity(Intent(this, MainActivity::class.java).apply {
                    action = Intent.ACTION_MAIN
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    putExtra(
                        MainActivity.EXTRA_GAME_RESULT,
                        Json.encodeToString(GameResultPayload.serializer(), result)
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                })
            }
            .onFailure {
                startActivity(Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                })
            }
        finish()
    }
}

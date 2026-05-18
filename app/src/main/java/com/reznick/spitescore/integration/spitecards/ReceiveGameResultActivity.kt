package com.reznick.spitescore.integration.spitecards

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.reznick.spitescore.MainActivity

class ReceiveGameResultActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bridge = SpiteCardsBridge(applicationContext)
        bridge.parseIncomingGameResult(intent)
            .onSuccess { result ->
                val mainIntent = android.content.Intent(this, MainActivity::class.java).apply {
                    action = android.content.Intent.ACTION_MAIN
                    addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                    putExtra(
                        MainActivity.EXTRA_GAME_RESULT,
                        kotlinx.serialization.json.Json.encodeToString(GameResultPayload.serializer(), result)
                    )
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(mainIntent)
            }
            .onFailure {
                val mainIntent = android.content.Intent(this, MainActivity::class.java).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(mainIntent)
            }
        finish()
    }
}

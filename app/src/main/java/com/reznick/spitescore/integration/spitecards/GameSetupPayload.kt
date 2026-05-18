package com.reznick.spitescore.integration.spitecards

import kotlinx.serialization.Serializable

// Sent to SpiteCards to start a replay session
@Serializable
data class GameSetupPayload(
    val schemaVersion: Int = SchemaVersion.CURRENT,
    val sourceApp: String = "spitescore",
    val gameType: String,
    val variant: String = "",
    val ruleOptions: Map<String, Boolean> = emptyMap(),
    val players: List<SetupPlayer>
)

@Serializable
data class SetupPlayer(val name: String)

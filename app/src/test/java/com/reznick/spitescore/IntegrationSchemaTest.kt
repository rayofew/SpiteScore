package com.reznick.spitescore

import com.reznick.spitescore.integration.spitecards.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class IntegrationSchemaTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GameResultPayload round-trips correctly`() {
        val payload = GameResultPayload(
            schemaVersion = 1,
            sourceApp = "spitecards",
            gameType = "cassino",
            variant = "spade",
            ruleOptions = mapOf("shortGame" to false, "muggins" to true),
            players = listOf(
                ResultPlayer("Ray", 0),
                ResultPlayer("Sarah", 1)
            ),
            scores = listOf(ResultScore(0, 11), ResultScore(1, 8)),
            winnerSeats = listOf(0),
            playedAt = "2026-06-12T19:30:00Z",
            durationSeconds = 1840
        )
        val encoded = json.encodeToString(GameResultPayload.serializer(), payload)
        val decoded = json.decodeFromString(GameResultPayload.serializer(), encoded)

        assertEquals(payload.schemaVersion, decoded.schemaVersion)
        assertEquals(payload.gameType, decoded.gameType)
        assertEquals(payload.winnerSeats, decoded.winnerSeats)
        assertEquals(payload.players.size, decoded.players.size)
        assertEquals(payload.scores.find { it.seat == 0 }?.total, decoded.scores.find { it.seat == 0 }?.total)
    }

    @Test
    fun `unknown fields are ignored on incoming payload`() {
        val withExtraFields = """
            {
              "schemaVersion": 1,
              "sourceApp": "spitecards",
              "gameType": "cassino",
              "players": [{"name": "Ray", "seat": 0}],
              "scores": [{"seat": 0, "total": 11}],
              "winnerSeats": [0],
              "playedAt": "2026-06-12T19:30:00Z",
              "durationSeconds": 1840,
              "unknownFutureField": "ignored"
            }
        """.trimIndent()
        val decoded = json.decodeFromString(GameResultPayload.serializer(), withExtraFields)
        assertEquals("cassino", decoded.gameType)
    }

    @Test
    fun `schema version 1 is compatible`() {
        assertTrue(SchemaVersion.isCompatible(1))
    }

    @Test
    fun `future schema version is incompatible`() {
        assertFalse(SchemaVersion.isCompatible(99))
        assertTrue(SchemaVersion.incompatibleMessage(99).contains("99"))
    }

    @Test
    fun `GameSetupPayload encodes correctly for SpiteCards handoff`() {
        val setup = GameSetupPayload(
            gameType = "cassino",
            variant = "spade",
            ruleOptions = mapOf("shortGame" to false),
            players = listOf(SetupPlayer("Ray"), SetupPlayer("Sarah"))
        )
        val encoded = json.encodeToString(GameSetupPayload.serializer(), setup)
        assertTrue(encoded.contains("\"sourceApp\":\"spitescore\""))
        assertTrue(encoded.contains("\"schemaVersion\":1"))
    }
}

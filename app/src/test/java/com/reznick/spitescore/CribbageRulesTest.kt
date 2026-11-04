package com.reznick.spitescore

import com.reznick.spitescore.ui.game.cribbage.CribbageRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CribbageRulesTest {

    @Test
    fun `pads cover 1 through 29 exactly once`() {
        val all = CribbageRules.MAIN_PAD + CribbageRules.MORE_PAD
        assertEquals((1..29).toList(), all)
    }

    @Test
    fun `skunk lines for a standard 121 game`() {
        assertEquals(91, CribbageRules.skunkLine(121))
        assertEquals(61, CribbageRules.doubleSkunkLine(121))
    }

    @Test
    fun `skunk lines for a short 61 game`() {
        assertEquals(31, CribbageRules.skunkLine(61))
        assertNull(CribbageRules.doubleSkunkLine(61))
    }

    @Test
    fun `skunk labels`() {
        assertNull(CribbageRules.skunkLabel(121, 91))
        assertNull(CribbageRules.skunkLabel(121, 100))
        assertEquals("Skunk!", CribbageRules.skunkLabel(121, 90))
        assertEquals("Skunk!", CribbageRules.skunkLabel(121, 61))
        assertEquals("Double skunk!", CribbageRules.skunkLabel(121, 60))
        assertEquals("Double skunk!", CribbageRules.skunkLabel(121, 0))
    }

    @Test
    fun `win is at or past the target`() {
        assertFalse(CribbageRules.hasWon(120, 121))
        assertTrue(CribbageRules.hasWon(121, 121))
        assertTrue(CribbageRules.hasWon(125, 121))
    }

    @Test
    fun `back peg trails the front peg by the last delta`() {
        assertEquals(0, CribbageRules.backPeg(0, null))
        assertEquals(40, CribbageRules.backPeg(48, 8))
        // A take-back must not put the back peg ahead of the front peg
        assertEquals(44, CribbageRules.backPeg(44, -4))
        // Never below zero
        assertEquals(0, CribbageRules.backPeg(3, 5))
    }
}

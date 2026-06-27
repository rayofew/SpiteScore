package com.reznick.spitescore.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Shared identity with SpiteYatzy — warm & editorial. A cream "paper" background, one refined
 * clay accent, antique gold for highlights, plus warm-roast variants for the dark toggle.
 */
object SpiteColors {
    // Brand accent — refined terracotta / clay.
    val Accent = Color(0xFFB04A36)
    val AccentDark = Color(0xFF8A3829)
    val AccentContainer = Color(0xFFF1D9CE)
    val OnAccentContainer = Color(0xFF3A130A)

    // Highlights — antique gold, not neon.
    val Gold = Color(0xFFA9802F)
    val GoldContainer = Color(0xFFEDE0BD)
    val OnGold = Color(0xFF382A09)

    // Warm paper surfaces (light).
    val Paper = Color(0xFFF7F1E7)
    val PaperSurface = Color(0xFFFFFDF8)
    val PaperSurfaceVariant = Color(0xFFEEE6D8)
    val PaperOutline = Color(0xFFD8CCBB)
    val OnPaper = Color(0xFF2A241E)
    val OnPaperMuted = Color(0xFF6E655A)

    // Warm brick error.
    val Error = Color(0xFFA8392B)
    val ErrorContainer = Color(0xFFF1D8D0)
    val OnErrorContainer = Color(0xFF3A0E07)
}

// ── Score-delta colours (used directly in GameScreen) — warm and on-brand ─────
val DeltaPositive = Color(0xFF4A6B4E)   // muted green
val DeltaNegative = Color(0xFFA8392B)   // warm brick

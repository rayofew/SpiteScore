package com.reznick.spitescore.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// The SpiteYatzy warm cream/clay identity — brand-locked and always light (no dark mode), so the
// logo and colors stay consistent on every device, exactly like SpiteYatzy.
private val LightColorScheme = lightColorScheme(
    primary              = SpiteColors.Accent,
    onPrimary            = SpiteColors.PaperSurface,
    primaryContainer     = SpiteColors.AccentContainer,
    onPrimaryContainer   = SpiteColors.OnAccentContainer,
    secondary            = SpiteColors.Gold,
    onSecondary          = SpiteColors.OnGold,
    secondaryContainer   = SpiteColors.GoldContainer,
    onSecondaryContainer = SpiteColors.OnGold,
    tertiary             = SpiteColors.Gold,
    onTertiary           = SpiteColors.PaperSurface,
    background           = SpiteColors.Paper,
    onBackground         = SpiteColors.OnPaper,
    surface              = SpiteColors.PaperSurface,
    onSurface            = SpiteColors.OnPaper,
    surfaceVariant       = SpiteColors.PaperSurfaceVariant,
    onSurfaceVariant     = SpiteColors.OnPaperMuted,
    outline              = SpiteColors.PaperOutline,
    error                = SpiteColors.Error,
    onError              = SpiteColors.PaperSurface,
    errorContainer       = SpiteColors.ErrorContainer,
    onErrorContainer     = SpiteColors.OnErrorContainer,
)

@Composable
fun SpiteScoreTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = SpiteTypography,
        content     = content,
    )
}

package com.reznick.spitescore.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Light scheme ──────────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary              = Terracotta40,
    onPrimary            = Color.White,
    primaryContainer     = Terracotta90,
    onPrimaryContainer   = Terracotta10,

    secondary            = Plum40,
    onSecondary          = Color.White,
    secondaryContainer   = Plum90,
    onSecondaryContainer = Plum10,

    tertiary             = Amber40,
    onTertiary           = Color.White,
    tertiaryContainer    = Amber90,
    onTertiaryContainer  = Amber10,

    error                = ErrorLight,
    onError              = Color.White,
    errorContainer       = ErrorContainerLight,
    onErrorContainer     = Color(0xFF410002),

    background           = Terracotta99,
    onBackground         = WarmNeutral10,
    surface              = Terracotta99,
    onSurface            = WarmNeutral10,
    surfaceVariant       = WarmNeutral95,
    onSurfaceVariant     = WarmNeutral30,
    outline              = WarmNeutral30,
)

// ── Dark scheme ───────────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary              = Terracotta80,
    onPrimary            = Terracotta20,
    primaryContainer     = Terracotta40,
    onPrimaryContainer   = Terracotta90,

    secondary            = Plum80,
    onSecondary          = Plum20,
    secondaryContainer   = Plum40,
    onSecondaryContainer = Plum90,

    tertiary             = Amber80,
    onTertiary           = Amber10,
    tertiaryContainer    = Amber40,
    onTertiaryContainer  = Amber90,

    error                = ErrorDark,
    onError              = Color(0xFF690005),
    errorContainer       = ErrorContainerDark,
    onErrorContainer     = Color(0xFFFFDAD6),

    background           = WarmNeutral10,
    onBackground         = WarmNeutral90,
    surface              = WarmNeutral10,
    onSurface            = WarmNeutral90,
    surfaceVariant       = WarmNeutral20,
    onSurfaceVariant     = WarmNeutral90,
    outline              = Color(0xFF9E8B85),
)

@Composable
fun SpiteScoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = SpiteTypography,
        content     = content
    )
}

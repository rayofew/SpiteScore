@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.reznick.spitescore.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.reznick.spitescore.R

/*
 * Shared identity with SpiteYatzy — an editorial type pairing, both bundled (fully offline):
 *  - Playfair Display (serif) for the big editorial moments — the brand, headlines, winners.
 *  - Inter (sans) for everything functional — titles, body, labels, and the score figures.
 * Both are variable fonts; we pin specific weights through the wght axis.
 */
private fun sans(weight: Int) = Font(
    R.font.inter_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private fun serif(weight: Int) = Font(
    R.font.playfair_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Sans = FontFamily(sans(400), sans(500), sans(600), sans(700))
val Serif = FontFamily(serif(500), serif(600), serif(700))

val SpiteTypography = Typography(
    // Display / headline — Playfair serif, tight tracking for an editorial feel.
    displayLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight(600), fontSize = 44.sp, lineHeight = 50.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight(600), fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-0.4).sp),
    displaySmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight(600), fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.3).sp),
    headlineLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight(600), fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.3).sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight(600), fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight(600), fontSize = 22.sp, lineHeight = 28.sp),

    // Titles — Inter, confident weights.
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight(600), fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.1).sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight(600), fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight(600), fontSize = 14.sp, lineHeight = 20.sp),

    // Body — Inter, comfortable reading.
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight(400), fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight(400), fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight(400), fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.1.sp),

    // Labels — Inter, a little tracking for a refined, "small-caps" feel.
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight(600), fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight(500), fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight(500), fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.3.sp),
)

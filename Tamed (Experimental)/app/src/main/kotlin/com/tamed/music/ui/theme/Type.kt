/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tamed.music.R

private val DisplayFontFamily = FontFamily(
    Font(R.font.pp_editorial_new_regular, FontWeight.Normal),
    Font(R.font.pp_editorial_new_ultrabold, FontWeight.SemiBold),
    Font(R.font.pp_editorial_new_ultrabold, FontWeight.Bold),
)

private val UiFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun buildTypography(
    displayFontFamily: FontFamily,
    uiFontFamily: FontFamily,
) = Typography(
    displayLarge = TextStyle(
        fontFamily = displayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.6).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = displayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.5).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = displayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = displayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.35).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = displayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = displayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = displayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.15).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = uiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = uiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = uiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = uiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = uiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = uiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = uiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = uiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp,
    ),
)

val AppTypography = buildTypography(DisplayFontFamily, UiFontFamily)
val SystemTypography = buildTypography(FontFamily.Default, FontFamily.Default)

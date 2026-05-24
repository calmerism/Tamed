/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.ui.theme

import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette

object TamedAppleColors {
    val Background = Color(0xFF0A0A0A)
    val BackgroundElevated = Color(0xFF111111)
    val Surface = Color(0xFF151515)
    val SurfaceStrong = Color(0xFF212121)
    val Glass = Color(0xFF242424).copy(alpha = 0.82f)
    val GlassBorder = Color.White.copy(alpha = 0.08f)
    val PrimaryText = Color.White
    val SecondaryText = Color.White.copy(alpha = 0.66f)
    val TertiaryText = Color.White.copy(alpha = 0.46f)
    val Divider = Color.White.copy(alpha = 0.08f)
    val SearchBubble = Color(0xFF242424)
    val AccentFallback = Color(0xFFFF6B81)
}

object TamedAppleShapes {
    val card = RoundedCornerShape(14.dp)
    val panel = RoundedCornerShape(22.dp)
    val miniPlayer = RoundedCornerShape(26.dp)
    val pill = RoundedCornerShape(999.dp)
}

object TamedAppleTypography {
    @Composable
    fun largeTitle(): TextStyle =
        MaterialTheme.typography.headlineLarge.copy(
            fontSize = 32.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
            color = applePrimaryTextColor(),
        )

    @Composable
    fun sectionTitle(): TextStyle =
        MaterialTheme.typography.headlineSmall.copy(
            fontSize = 22.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-0.2).sp,
            color = applePrimaryTextColor(),
        )

    @Composable
    fun cardTitle(): TextStyle =
        MaterialTheme.typography.titleLarge.copy(
            fontSize = 21.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp,
            color = applePrimaryTextColor(),
        )

    @Composable
    fun cardSubtitle(): TextStyle =
        MaterialTheme.typography.bodyMedium.copy(
            fontSize = 14.sp,
            lineHeight = 18.sp,
            color = appleSecondaryTextColor(),
        )

    @Composable
    fun metadata(): TextStyle =
        MaterialTheme.typography.bodySmall.copy(
            fontSize = 13.sp,
            lineHeight = 16.sp,
            color = appleTertiaryTextColor(),
        )
}

@Composable
fun appleBackgroundColor(): Color = MaterialTheme.colorScheme.background

@Composable
fun appleSurfaceColor(): Color = MaterialTheme.colorScheme.surfaceContainerLow

@Composable
fun appleSurfaceStrongColor(): Color = MaterialTheme.colorScheme.surfaceContainerHigh

@Composable
fun appleGlassColor(): Color {
    val darkTheme = isSystemInDarkTheme()
    return appleSurfaceStrongColor().copy(alpha = if (darkTheme) 0.94f else 0.98f)
}

@Composable
fun appleGlassBorderColor(): Color {
    val darkTheme = isSystemInDarkTheme()
    return MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (darkTheme) 0.42f else 0.78f)
}

@Composable
fun applePrimaryTextColor(): Color = MaterialTheme.colorScheme.onBackground

@Composable
fun appleSecondaryTextColor(): Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)

@Composable
fun appleTertiaryTextColor(): Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.54f)

@Composable
fun appleDividerColor(): Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.56f)

@Composable
fun appleSearchBubbleColor(): Color = MaterialTheme.colorScheme.surfaceContainerHigh

@Composable
fun appleSelectedContainerColor(): Color =
    if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.12f)
    else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)

fun extractDominantColor(bitmap: Bitmap): Color {
    val palette = Palette.from(bitmap)
        .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
        .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
        .generate()

    val swatchColor = listOfNotNull(
        palette.vibrantSwatch?.rgb,
        palette.dominantSwatch?.rgb,
        palette.mutedSwatch?.rgb,
        palette.darkVibrantSwatch?.rgb,
        palette.darkMutedSwatch?.rgb,
    ).firstOrNull()

    return swatchColor?.let(::Color)?.let(::normalizeAccentColor)
        ?: TamedAppleColors.AccentFallback
}

fun normalizeAccentColor(color: Color): Color {
    val alphaFixed = color.copy(alpha = 1f)
    val baseline = if (ColorUtils.calculateLuminance(alphaFixed.toArgb()) < 0.12f) {
        lerp(alphaFixed, Color.White, 0.18f)
    } else {
        alphaFixed
    }
    return lerp(baseline, TamedAppleColors.AccentFallback, 0.12f)
}

fun artworkBackdropBrush(accent: Color): Brush = Brush.verticalGradient(
    colors = listOf(
        lerp(accent, Color.Black, 0.32f).copy(alpha = 0.88f),
        lerp(accent, TamedAppleColors.Background, 0.64f).copy(alpha = 0.92f),
        TamedAppleColors.Background,
    ),
)

fun artworkCardOverlay(accent: Color): Brush = Brush.verticalGradient(
    colors = listOf(
        Color.Transparent,
        Color.Transparent,
        Color.Black.copy(alpha = 0.12f),
        lerp(accent, Color.Black, 0.68f).copy(alpha = 0.92f),
    ),
    startY = 0f,
    endY = Float.POSITIVE_INFINITY,
)

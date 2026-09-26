/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import com.tamed.music.ui.theme.SfProFontFamily
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlin.math.abs
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import com.tamed.music.R
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.graphics.Shape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.graphics.lerp
import com.tamed.music.ui.theme.LocalBackdropColors
import com.tamed.music.ui.theme.TamedAppleShapes
import com.tamed.music.ui.theme.TamedAppleTypography
import com.tamed.music.ui.theme.appleDividerColor
import com.tamed.music.ui.theme.appleGlassBorderColor
import com.tamed.music.ui.theme.appleGlassColor
import com.tamed.music.ui.theme.applePrimaryTextColor
import com.tamed.music.ui.theme.appleSearchBubbleColor
import com.tamed.music.ui.theme.appleSecondaryTextColor
import com.tamed.music.ui.theme.appleSelectedContainerColor
import com.tamed.music.ui.theme.appleSurfaceColor
import com.tamed.music.ui.theme.appleSurfaceStrongColor
import com.tamed.music.ui.theme.artworkCardOverlay
import com.tamed.music.ui.theme.TamedAppleColors
import com.tamed.music.ui.utils.resize

data class GlassBottomBarItem(
    val key: String,
    val label: String,
    @DrawableRes val selectedIconRes: Int,
    @DrawableRes val unselectedIconRes: Int,
)

@Composable
fun GlassBottomBar(
    items: List<GlassBottomBarItem>,
    selectedKey: String?,
    onItemClick: (GlassBottomBarItem) -> Unit,
    onSearchClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    solid: Boolean = false,
) {
    val glassColor = if (solid) {
        appleSurfaceStrongColor()
    } else {
        val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
        if (darkTheme) {
            Color.Black.copy(alpha = 0.35f)
        } else {
            Color.White.copy(alpha = 0.75f)
        }
    }
    val searchBubbleColor = if (solid) appleSurfaceStrongColor() else appleSearchBubbleColor()

    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val glassScope = LocalGlassScope.current
    val barId = remember { kotlin.random.Random.nextLong() }
    val glassModifier = if (glassScope != null && !solid) {
        with(glassScope) {
            Modifier.glassBackground(
                id = barId,
                scale = 0.05f,
                blur = 0.6f,
                centerDistortion = 0.02f,
                shape = TamedAppleShapes.pill,
                elevation = 6.dp,
                tint = glassColor,
                darkness = if (darkTheme) 0.1f else 0.0f
            )
        }
    } else {
        Modifier.background(glassColor)
    }

    val searchBubbleId = remember { kotlin.random.Random.nextLong() }
    val searchBubbleGlassModifier = if (glassScope != null && !solid) {
        with(glassScope) {
            Modifier.glassBackground(
                id = searchBubbleId,
                scale = 0.05f,
                blur = 0.6f,
                centerDistortion = 0.02f,
                shape = TamedAppleShapes.panel,
                elevation = 6.dp,
                tint = searchBubbleColor,
                darkness = if (darkTheme) 0.1f else 0.0f
            )
        }
    } else {
        Modifier.background(searchBubbleColor)
    }
    val primaryTextColor = applePrimaryTextColor()
    val secondaryTextColor = appleSecondaryTextColor()
    val selectedContainerColor = appleSelectedContainerColor()

    val selectedIndex = remember(selectedKey, items) {
        items.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)
    }
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        ),
        label = "liquidIndex"
    )

    val diff = selectedIndex.toFloat() - animatedIndex
    val stretchX = 1f + abs(diff) * 0.35f
    val squeezeY = 1f - abs(diff) * 0.15f

    Box(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(76.dp)
                    .clip(TamedAppleShapes.pill)
                    .then(glassModifier)
                    .border(
                        width = 1.dp,
                        color = appleGlassBorderColor(),
                        shape = TamedAppleShapes.pill
                    )
                    .padding(horizontal = 9.dp, vertical = 7.dp),
            ) {
                val parentWidth = maxWidth
                val tabCount = items.size
                if (tabCount > 0) {
                    val tabWidth = parentWidth / tabCount

                    // Liquid sliding active capsule background
                    Box(
                        modifier = Modifier
                            .width(tabWidth)
                            .fillMaxHeight()
                            .graphicsLayer {
                                translationX = animatedIndex * tabWidth.toPx()
                                scaleX = stretchX
                                scaleY = squeezeY
                                transformOrigin = TransformOrigin(
                                    pivotFractionX = if (diff > 0f) 0.1f else 0.9f,
                                    pivotFractionY = 0.5f
                                )
                            }
                            .clip(TamedAppleShapes.pill)
                            .background(selectedContainerColor)
                            .border(
                                0.5.dp,
                                if (darkTheme) Color.White.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                TamedAppleShapes.pill
                            )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items.forEach { item ->
                        val selected = item.key == selectedKey
                        val itemContent by animateColorAsState(
                            targetValue = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                secondaryTextColor
                            },
                            label = "bottomBarItemContent",
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(TamedAppleShapes.pill)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onItemClick(item) }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                painter = painterResource(if (selected) item.selectedIconRes else item.unselectedIconRes),
                                contentDescription = item.label,
                                tint = itemContent,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = itemContent,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            if (onSearchClick != null) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(TamedAppleShapes.panel)
                        .then(searchBubbleGlassModifier)
                        .border(
                            width = 1.dp,
                            color = appleGlassBorderColor(),
                            shape = TamedAppleShapes.panel
                        )
                        .clickable(onClick = onSearchClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(com.tamed.music.R.drawable.search),
                        contentDescription = "Search",
                        tint = primaryTextColor,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = TamedAppleTypography.sectionTitle(),
            )
            if (!subtitle.isNullOrBlank()) {
                val formattedSubtitle = if (subtitle.all { it.isUpperCase() || !it.isLetter() }) {
                    subtitle.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                } else {
                    subtitle
                }
                Text(
                    text = formattedSubtitle,
                    style = TamedAppleTypography.headerSubtitle(),
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun glassToggleButtonColors(primary: Boolean = false): ToggleButtonColors {
    val isLightContent = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f
    return ToggleButtonDefaults.toggleButtonColors(
        checkedContainerColor = if (isLightContent) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
        checkedContentColor = if (isLightContent) Color.White else MaterialTheme.colorScheme.primary,
        containerColor = if (isLightContent) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f),
        contentColor = if (isLightContent) Color.White else MaterialTheme.colorScheme.onSurface
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ToggleButtonColors = glassToggleButtonColors(primary = false),
    content: @Composable RowScope.() -> Unit
) {
    androidx.compose.material3.ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        content = content
    )
}

@Composable
fun glassButtonColors(primary: Boolean = false): ButtonColors {
    val isLightContent = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f
    val containerColor = if (isLightContent) {
        if (primary) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.14f)
    } else {
        if (primary) Color.Black.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)
    }
    val contentColor = if (isLightContent) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    return ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColor
    )
}

@Composable
fun Modifier.appleGlassEffect(
    shape: CornerBasedShape,
    surfaceOpacity: Float = 0.16f,
    tint: Color = Color.Unspecified,
    highlightAlpha: Float = 0.30f,
): Modifier {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backdropColors = LocalBackdropColors.current
    val backdropPrimary = backdropColors.firstOrNull()
    val hasBackdropColor = backdropPrimary != null && backdropPrimary != Color.Unspecified

    val baseTint = if (tint != Color.Unspecified) {
        tint
    } else if (hasBackdropColor && darkTheme) {
        // Blend dynamic backdrop color into translucent liquid glass
        lerp(backdropPrimary, Color.White, 0.20f).copy(alpha = (surfaceOpacity * 1.5f).coerceIn(0.18f, 0.40f))
    } else if (darkTheme) {
        // Translucent liquid glass that lets underlying backdrop colors shine through
        Color.White.copy(alpha = surfaceOpacity)
    } else if (hasBackdropColor) {
        lerp(backdropPrimary, Color.White, 0.75f).copy(alpha = 0.85f)
    } else {
        // Light theme frosted glass
        Color.White.copy(alpha = 0.72f)
    }

    val rimBrush = remember(darkTheme, highlightAlpha, backdropPrimary) {
        val rimColor = if (hasBackdropColor && darkTheme) {
            lerp(backdropPrimary, Color.White, 0.65f)
        } else {
            Color.White
        }
        Brush.verticalGradient(
            colors = if (darkTheme) {
                listOf(
                    rimColor.copy(alpha = highlightAlpha),
                    rimColor.copy(alpha = highlightAlpha * 0.35f),
                    Color.Transparent,
                )
            } else {
                listOf(
                    Color.White.copy(alpha = highlightAlpha * 1.5f),
                    Color.White.copy(alpha = highlightAlpha * 0.4f),
                    Color.Black.copy(alpha = 0.06f),
                )
            }
        )
    }

    return this
        .clip(shape)
        .background(baseTint)
        .border(
            width = 0.7.dp,
            brush = rimBrush,
            shape = shape,
        )
}

@Composable
fun GlassIconCircleButton(
    @DrawableRes iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 36.dp,
    accent: Color = Color.Unspecified,
    solid: Boolean = true,
    solidColor: Color = Color.Unspecified,
    onPhoto: Boolean = false,
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val iconSize = buttonSize * 0.48f

    if (onPhoto) {
        Box(
            modifier = modifier
                .size(buttonSize)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(iconSize),
            )
        }
        return
    }

    val discColor = when {
        solid && solidColor != Color.Unspecified -> solidColor
        solid && darkTheme -> Color(0xFF161618)
        solid && !darkTheme -> Color(0xFFEBEBEF)
        darkTheme -> Color.White.copy(alpha = 0.12f)
        else -> Color.Black.copy(alpha = 0.06f)
    }
    val showBorder = false
    val borderColor = if (darkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val iconTint = when {
        accent != Color.Unspecified -> accent
        darkTheme -> Color.White
        else -> Color(0xFF1C1C1E)
    }

    Box(
        modifier = modifier
            .size(buttonSize)
            .clip(CircleShape)
            .background(discColor)
            .then(
                if (showBorder) {
                    Modifier.border(0.5.dp, borderColor, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun AppleHeroPlayButton(
    onClick: () -> Unit,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color(0xFF1C1C1E),
            modifier = Modifier
                .size(44.dp)
                .then(if (!isPlaying) Modifier.offset(x = 2.dp) else Modifier),
        )
    }
}

@Composable
fun ApplePillPlayButton(
    onClick: () -> Unit,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier,
    text: String = if (isPlaying) "Pause" else "Play",
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                contentDescription = text,
                tint = Color(0xFF1C1C1E),
                modifier = Modifier
                    .size(if (isPlaying) 19.dp else 21.dp)
                    .then(if (!isPlaying) Modifier.offset(x = 1.dp) else Modifier),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                fontFamily = SfProFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color(0xFF1C1C1E),
                letterSpacing = (-0.2).sp,
            )
        }
    }
}



@Composable
fun GlassActionPill(
    modifier: Modifier = Modifier,
    solid: Boolean = false,
    solidColor: Color = Color.Unspecified,
    onPhoto: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    if (onPhoto) {
        // Bare row of icons — no pill, no background, no border
        Row(
            modifier = modifier.height(50.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
        return
    }

    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val pillShape = CircleShape
    val pillBgColor = when {
        solid && solidColor != Color.Unspecified -> solidColor
        darkTheme -> Color.White.copy(alpha = 0.18f)
        else -> Color.Black.copy(alpha = 0.10f)
    }
    val borderColor = if (darkTheme) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.10f)

    Row(
        modifier = modifier
            .height(50.dp)
            .clip(pillShape)
            .background(pillBgColor)
            .border(0.5.dp, borderColor, pillShape)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(TamedAppleShapes.panel)
            .background(appleGlassColor())
            .border(0.5.dp, appleGlassBorderColor(), TamedAppleShapes.panel)
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun MediaCard(
    title: String,
    subtitle: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = TamedAppleColors.AccentFallback,
    metadata: String? = null,
    badge: String? = null,
    tall: Boolean = false,
    square: Boolean = false,
    video: Boolean = false,
    cardSize: Dp? = null,
    textBelow: Boolean = true,
    imageSize: Int = 500,
    isExplicit: Boolean = false,
) {
    val context = LocalContext.current
    val cardShape = when {
        video -> RoundedCornerShape(14.dp)
        tall && !textBelow -> RoundedCornerShape(18.dp)
        else -> TamedAppleShapes.card
    }

    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f

    val effectiveWidth = cardSize ?: when {
        video -> 220.dp
        square -> 160.dp
        tall -> 250.dp
        else -> 168.dp
    }

    val imageModifier = Modifier
        .then(
            when {
                video -> Modifier.width(effectiveWidth).aspectRatio(16f / 9f)
                square -> Modifier.size(effectiveWidth)
                tall -> Modifier.width(effectiveWidth).height(330.dp)
                else -> Modifier.width(effectiveWidth).height(222.dp)
            }
        )
        .then(
            if (isLight) Modifier.shadow(
                elevation = 8.dp,
                shape = cardShape,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.14f),
            ) else Modifier
        )
        .clip(cardShape)
        .border(
            width = 0.5.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f),
                    if (isLight) Color.Black.copy(alpha = 0.02f) else Color.White.copy(alpha = 0.02f)
                )
            ),
            shape = cardShape
        )
        .clickable(onClick = onClick)

    if (textBelow) {
        Column(
            modifier = Modifier
                .width(effectiveWidth)
                .then(modifier),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = imageModifier) {
                AsyncImage(
                    model = remember(imageUrl) {
                        ImageRequest.Builder(context)
                            .data(imageUrl?.resize(if (video) 720 else imageSize, if (video) 405 else imageSize))
                            .crossfade(true)
                            .build()
                    },
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                if (!badge.isNullOrBlank()) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = applePrimaryTextColor()
                    ),
                    maxLines = if (video) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isExplicit) {
                        ExplicitTag(
                            color = appleSecondaryTextColor(),
                            size = 12.dp,
                        )
                    }
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = appleSecondaryTextColor()
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!metadata.isNullOrBlank()) {
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = appleSecondaryTextColor().copy(alpha = 0.8f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    } else {
        Box(
            modifier = modifier
                .then(
                    if (square) {
                        if (cardSize != null) Modifier.size(cardSize)
                        else Modifier.fillMaxWidth().aspectRatio(1f)
                    } else {
                        Modifier
                            .width(effectiveWidth)
                            .then(if (tall) Modifier.height(effectiveWidth * 1.26f) else Modifier.height(222.dp))
                    }
                )
                .clip(cardShape)
                .border(0.5.dp, Color.White.copy(alpha = 0.06f), cardShape)
                .clickable(onClick = onClick),
        ) {
            if (tall) {
                // Layer 1 – Ambient blurred backdrop extending through the full card
                AsyncImage(
                    model = remember(imageUrl) {
                        ImageRequest.Builder(context)
                            .data(imageUrl?.resize(160, 160))
                            .crossfade(true)
                            .build()
                    },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(32.dp),
                )

                // Layer 2 – 1:1 Uncropped Album Art pinned at top with progressive alpha fade into blur
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .align(Alignment.TopCenter)
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.00f to Color.White,
                                    0.52f to Color.White,
                                    0.72f to Color.White.copy(alpha = 0.78f),
                                    0.86f to Color.White.copy(alpha = 0.38f),
                                    0.95f to Color.White.copy(alpha = 0.10f),
                                    1.00f to Color.Transparent,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        },
                ) {
                    AsyncImage(
                        model = remember(imageUrl) {
                            ImageRequest.Builder(context)
                                .data(imageUrl?.resize(imageSize, imageSize))
                                .crossfade(true)
                                .build()
                        },
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                AsyncImage(
                    model = remember(imageUrl) {
                        ImageRequest.Builder(context)
                            .data(imageUrl?.resize(imageSize, imageSize))
                            .crossfade(true)
                            .build()
                    },
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = if (square) 0.12f else 0f)),
                )
            }

            // Bottom gradient – darkens smoothly for pristine text legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val artEnd = (size.width / size.height).coerceIn(0.60f, 0.82f)
                        val overlayBrush = if (tall) {
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.50f to Color.Transparent,
                                0.72f to Color.Black.copy(alpha = 0.35f),
                                1.0f to Color.Black.copy(alpha = 0.75f),
                            )
                        } else {
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.38f to Color.Transparent,
                                0.68f to Color.Black.copy(alpha = 0.48f),
                                1.0f to Color.Black.copy(alpha = 0.92f),
                            )
                        }
                        onDrawBehind { drawRect(brush = overlayBrush) }
                    },
            )

            // Subtle top sheen for depth
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.36f)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.04f), Color.Transparent),
                        ),
                    ),
            )

            if (!badge.isNullOrBlank()) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (!metadata.isNullOrBlank()) {
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.70f),
                            letterSpacing = 0.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = if (tall) 16.sp else 15.sp,
                        lineHeight = if (tall) 20.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = if (tall) 14.sp else 13.sp,
                        lineHeight = if (tall) 18.sp else 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.85f),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun CompactMediaCard(
    title: String,
    subtitle: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(TamedAppleShapes.panel)
            .background(appleGlassColor())
            .border(
                width = 0.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.02f)
                    )
                ),
                shape = TamedAppleShapes.panel
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = applePrimaryTextColor(),
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = TamedAppleTypography.cardSubtitle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painter = painterResource(com.tamed.music.R.drawable.navigate_next),
            contentDescription = null,
            tint = appleSecondaryTextColor(),
        )
    }
}

@Composable
fun <T> SectionCarousel(
    items: List<T>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    horizontalSpacing: Int = 12,
    key: ((T) -> Any)? = null,
    cardContent: @Composable (T) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing.dp),
        flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
    ) {
        items(
            items = items,
            key = key?.let { k -> { item: T -> k(item) } },
        ) { item ->
            cardContent(item)
        }
    }
}

@Composable
fun LibraryEntryRow(
    title: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = TamedAppleColors.AccentFallback,
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (darkTheme) Color(0xFF1C1C1E) else Color(0xFFE5E5EA)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = title,
                tint = if (darkTheme) Color.White else Color(0xFF1C1C1E),
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = applePrimaryTextColor(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                letterSpacing = (-0.2).sp,
            ),
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(com.tamed.music.R.drawable.navigate_next),
            contentDescription = null,
            tint = appleSecondaryTextColor(),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun AppleMoodGenreCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    stripeColor: Long? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "card_scale"
    )

    val cardShape = RoundedCornerShape(18.dp)
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .appleGlassEffect(
                shape = cardShape,
                surfaceOpacity = if (darkTheme) 0.04f else 0.45f,
                highlightAlpha = if (darkTheme) 0.28f else 0.35f,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = applePrimaryTextColor(),
                letterSpacing = (-0.2).sp
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun EqualizerAnimation(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    isPlaying: Boolean = true,
) {
    if (!isPlaying) {
        val staticHeights = remember { listOf(0.25f, 0.5f, 0.3f, 0.4f) }
        Row(
            modifier = modifier.size(width = 16.dp, height = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            staticHeights.forEach { heightFactor ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            scaleY = heightFactor
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                        }
                        .clip(RoundedCornerShape(1.dp))
                        .background(color)
                )
            }
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    val bar1 = infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar1",
    )
    val bar2 = infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar2",
    )
    val bar3 = infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar3",
    )
    val bar4 = infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar4",
    )
    val bars = remember { listOf(bar1, bar2, bar3, bar4) }

    Row(
        modifier = modifier.size(width = 16.dp, height = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { barState ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleY = barState.value
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                    }
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}

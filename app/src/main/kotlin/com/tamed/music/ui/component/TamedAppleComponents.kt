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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
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
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TamedAppleTypography.sectionTitle(),
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                val formattedSubtitle = if (subtitle.all { it.isUpperCase() || !it.isLetter() }) {
                    subtitle.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                } else {
                    subtitle
                }
                Text(
                    text = formattedSubtitle,
                    style = TamedAppleTypography.metadata(),
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
    return ToggleButtonDefaults.toggleButtonColors(
        checkedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
        checkedContentColor = MaterialTheme.colorScheme.primary,
        containerColor = if (primary) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f),
        contentColor = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
    val containerColor = if (primary) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }
    val contentColor = if (primary) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    return ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColor
    )
}

@Composable
fun GlassIconCircleButton(
    @DrawableRes iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Color.Unspecified,
    solid: Boolean = false,
    solidColor: Color = Color.Unspecified,
) {
    val resolvedAccent = if (accent == Color.Unspecified) applePrimaryTextColor() else accent
    
    val glassScope = LocalGlassScope.current
    val buttonId = remember { kotlin.random.Random.nextLong() }
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val buttonBgColor = if (solid && solidColor != Color.Unspecified) solidColor else appleGlassColor()
    val buttonGlassModifier = if (glassScope != null) {
        with(glassScope) {
            Modifier.glassBackground(
                id = buttonId,
                scale = 0.05f,
                blur = 0.6f,
                centerDistortion = 0.02f,
                shape = CircleShape,
                elevation = 4.dp,
                tint = buttonBgColor,
                darkness = if (darkTheme) 0.1f else 0.0f
            )
        }
    } else {
        Modifier.background(buttonBgColor)
    }

    Box(
        modifier = modifier
            .size(50.dp)
            .clip(CircleShape)
            .then(buttonGlassModifier)
            .border(0.5.dp, appleGlassBorderColor(), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = resolvedAccent,
            modifier = Modifier.size(22.dp),
        )
    }
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
    tall: Boolean = true,
    square: Boolean = false,
    cardSize: Dp? = null,
    textBelow: Boolean = true,
    imageSize: Int = 500,
) {
    val context = LocalContext.current
    val titleStyle = if (tall) {
        TamedAppleTypography.cardTitle()
    } else {
        MaterialTheme.typography.titleMedium.copy(
            fontSize = 18.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
    val subtitleStyle = if (tall) {
        TamedAppleTypography.cardSubtitle()
    } else {
        MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp,
            lineHeight = 15.sp,
            color = Color.White.copy(alpha = 0.72f),
        )
    }

    val imageModifier = Modifier
        .then(
            if (square) {
                val size = cardSize ?: 160.dp
                Modifier.size(size)
            } else {
                Modifier
                    .width(cardSize ?: if (tall) 280.dp else 168.dp)
                    .then(if (tall) Modifier.height(320.dp) else Modifier.height(222.dp))
            }
        )
        .clip(TamedAppleShapes.card)
        .border(
            width = 0.5.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.08f),
                    Color.White.copy(alpha = 0.02f)
                )
            ),
            shape = TamedAppleShapes.card
        )
        .clickable(onClick = onClick)

    if (textBelow) {
        Column(
            modifier = Modifier
                .width(cardSize ?: if (tall) 280.dp else 168.dp)
                .then(modifier),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = imageModifier) {
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
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = applePrimaryTextColor()
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = appleSecondaryTextColor()
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                            .width(cardSize ?: if (tall) 280.dp else 168.dp)
                            .then(if (tall) Modifier.height(320.dp) else Modifier.height(222.dp))
                    }
                )
                .clip(TamedAppleShapes.card)
                .border(0.5.dp, Color.White.copy(alpha = 0.06f), TamedAppleShapes.card)
                .clickable(onClick = onClick),
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
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (square) 0.12f else 0f)),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val overlayBrush = artworkCardOverlay(accent)
                        onDrawBehind {
                            drawRect(brush = overlayBrush)
                        }
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
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (!metadata.isNullOrBlank()) {
                    Text(
                        text = metadata,
                        style = TamedAppleTypography.metadata(),
                        color = Color.White.copy(alpha = 0.84f),
                    )
                }
                Text(
                    text = title,
                    style = titleStyle,
                    color = Color.White,
                    maxLines = if (tall) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = subtitleStyle,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 2,
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
    horizontalSpacing: Int = 16,
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
        items(items) { item ->
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
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                accent,
                                accent.copy(alpha = 0.75f)
                            )
                        )
                    )
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = applePrimaryTextColor(),
                    fontWeight = FontWeight.Medium,
                ),
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(com.tamed.music.R.drawable.navigate_next),
                contentDescription = null,
                tint = appleSecondaryTextColor(),
            )
        }
    }
}

@Composable
fun EqualizerAnimation(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")

    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    val bar4Height by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )

    Row(
        modifier = modifier.size(width = 16.dp, height = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val barHeights = listOf(bar1Height, bar2Height, bar3Height, bar4Height)
        barHeights.forEach { heightFactor ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightFactor)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}

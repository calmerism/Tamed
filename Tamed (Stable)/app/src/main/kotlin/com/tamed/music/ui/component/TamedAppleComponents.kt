/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
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
    val glassColor = if (solid) appleSurfaceStrongColor() else appleGlassColor()
    val glassBorderColor = appleGlassBorderColor()
    val searchBubbleColor = if (solid) appleSurfaceStrongColor() else appleSearchBubbleColor()
    val primaryTextColor = applePrimaryTextColor()
    val secondaryTextColor = appleSecondaryTextColor()
    val selectedContainerColor = appleSelectedContainerColor()

    Box(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(76.dp)
                    .clip(TamedAppleShapes.pill)
                    .background(glassColor)
                    .border(0.5.dp, glassBorderColor, TamedAppleShapes.pill)
                    .padding(horizontal = 9.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val selected = item.key == selectedKey
                    val itemContainer by animateColorAsState(
                        targetValue = if (selected) {
                            selectedContainerColor
                        } else {
                            Color.Transparent
                        },
                        label = "bottomBarItemContainer",
                    )
                    val itemContent by animateColorAsState(
                        targetValue = if (selected) {
                            primaryTextColor
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
                            .background(itemContainer)
                            .clickable { onItemClick(item) }
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

            if (onSearchClick != null) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(TamedAppleShapes.panel)
                        .background(searchBubbleColor)
                        .border(0.5.dp, glassBorderColor, TamedAppleShapes.panel)
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
    Box(
        modifier = modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(if (solid) if (solidColor != Color.Unspecified) solidColor else appleSurfaceStrongColor() else appleGlassColor())
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
                    .data(imageUrl?.resize(1200, 1200))
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
            .background(appleSurfaceColor())
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
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = title,
                    tint = accent,
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

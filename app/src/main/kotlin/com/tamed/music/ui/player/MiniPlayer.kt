/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.R
import com.tamed.music.constants.SwipeSensitivityKey
import com.tamed.music.extensions.togglePlayPause
import androidx.compose.foundation.isSystemInDarkTheme
import com.tamed.music.ui.theme.TamedAppleColors
import com.tamed.music.ui.theme.TamedAppleShapes
import com.tamed.music.ui.theme.appleGlassColor
import com.tamed.music.ui.theme.appleMiniPlayerGlassColor
import com.tamed.music.ui.theme.applePrimaryTextColor
import com.tamed.music.ui.theme.appleSecondaryTextColor
import com.tamed.music.ui.theme.extractDominantColor
import com.tamed.music.utils.rememberPreference
import com.tamed.music.ui.component.LocalGlassScope
import com.tamed.music.ui.component.GlassScope
import kotlin.math.roundToInt

@Composable
fun MiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val layoutDirection = LocalLayoutDirection.current
    val context = LocalContext.current
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnail by rememberPreference(com.tamed.music.constants.SwipeThumbnailKey, true)
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val accent = remember { mutableStateOf(TamedAppleColors.AccentFallback) }
    val glassColor = appleMiniPlayerGlassColor()
    val primaryTextColor = applePrimaryTextColor()
    val secondaryTextColor = appleSecondaryTextColor()

    val glassScope = LocalGlassScope.current
    val miniPlayerId = remember { kotlin.random.Random.nextLong() }
    val miniPlayerGlassModifier = if (glassScope != null) {
        with(glassScope) {
            Modifier.glassBackground(
                id = miniPlayerId,
                scale = 0.05f,
                blur = 0.6f,
                centerDistortion = 0.02f,
                shape = TamedAppleShapes.miniPlayer,
                elevation = 8.dp,
                tint = glassColor,
                darkness = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) 0.1f else 0.0f
            )
        }
    } else {
        Modifier.background(glassColor)
    }

    LaunchedEffect(mediaMetadata?.thumbnailUrl) {
        val thumbnail = mediaMetadata?.thumbnailUrl ?: return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(thumbnail)
            .size(Size(180, 180))
            .allowHardware(false)
            .build()
        val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
        val bitmap = result?.image?.toBitmap() ?: return@LaunchedEffect
        accent.value = extractDominantColor(bitmap)
    }

    SwipeableMiniPlayerBox(
        modifier = modifier,
        swipeSensitivity = swipeSensitivity,
        swipeThumbnail = swipeThumbnail,
        playerConnection = playerConnection,
        layoutDirection = layoutDirection,
        coroutineScope = coroutineScope,
        pureBlack = pureBlack,
        useLegacyBackground = false,
    ) { offsetX ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .clip(TamedAppleShapes.miniPlayer)
                .then(miniPlayerGlassModifier)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.White.copy(alpha = 0.08f)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (duration > 0L) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f)
                    .height(4.dp)
                    .align(Alignment.BottomStart)
                    .background(accent.value),
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AsyncImage(
                    model = mediaMetadata?.thumbnailUrl,
                    contentDescription = mediaMetadata?.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mediaMetadata?.title ?: "",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = primaryTextColor,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = mediaMetadata?.artists?.joinToString { it.name }.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = secondaryTextColor,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiniPlayerDockButton(
                        iconRes = R.drawable.skip_previous,
                        onClick = playerConnection::seekToPrevious,
                        enabled = playerConnection.player.hasPreviousMediaItem(),
                        tint = primaryTextColor,
                        background = accent.value.copy(alpha = 0.14f),
                    )
                    MiniPlayerDockButton(
                        iconRes = if (playbackState == Player.STATE_BUFFERING || isPlaying) R.drawable.pause else R.drawable.play,
                        onClick = { playerConnection.player.togglePlayPause() },
                        enabled = true,
                        tint = if (playbackState == Player.STATE_BUFFERING) accent.value else primaryTextColor,
                        background = accent.value.copy(alpha = 0.22f),
                    )
                    MiniPlayerDockButton(
                        iconRes = R.drawable.skip_next,
                        onClick = playerConnection::seekToNext,
                        enabled = playerConnection.player.hasNextMediaItem(),
                        tint = primaryTextColor,
                        background = accent.value.copy(alpha = 0.14f),
                    )
                }
            }
        }
    }
}


@Composable
private fun MiniPlayerDockButton(
    iconRes: Int,
    onClick: () -> Unit,
    enabled: Boolean,
    tint: Color,
    background: Color,
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val resolvedBg = if (darkTheme) {
        Color.White.copy(alpha = if (background.alpha > 0.15f) 0.16f else 0.08f)
    } else {
        Color.Black.copy(alpha = if (background.alpha > 0.15f) 0.08f else 0.04f)
    }
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(TamedAppleShapes.pill)
            .background(if (enabled) resolvedBg else resolvedBg.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (enabled) tint else tint.copy(alpha = 0.45f),
            modifier = Modifier.size(18.dp),
        )
    }
}

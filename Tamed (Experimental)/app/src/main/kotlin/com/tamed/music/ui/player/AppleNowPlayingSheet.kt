/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.ui.player

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.tamed.music.R
import com.tamed.music.extensions.togglePlayPause
import com.tamed.music.models.MediaMetadata
import com.tamed.music.playback.PlayerConnection
import com.tamed.music.ui.component.BottomSheetPageState
import com.tamed.music.ui.component.BottomSheetState
import com.tamed.music.ui.component.MenuState
import com.tamed.music.ui.menu.PlayerMenu
import com.tamed.music.ui.theme.TamedAppleColors
import com.tamed.music.ui.theme.TamedAppleShapes
import com.tamed.music.ui.theme.artworkBackdropBrush
import com.tamed.music.ui.utils.ShowMediaInfo
import com.tamed.music.utils.makeTimeString

@Composable
fun AppleNowPlayingSheet(
    mediaMetadata: MediaMetadata,
    isPlaying: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    currentSongLiked: Boolean,
    position: Long,
    duration: Long,
    sliderPosition: Long?,
    accent: Color,
    repeatMode: Int,
    shuffleModeEnabled: Boolean,
    playerVolume: Float,
    sleepTimerEnabled: Boolean,
    sleepTimerTimeLeft: Long,
    disableBlur: Boolean,
    navController: NavController,
    state: BottomSheetState,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    bottomSheetPageState: BottomSheetPageState,
    clipboardManager: ClipboardManager,
    context: Context,
    onSeekTo: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onShowLyrics: () -> Unit,
    onShowQueue: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSleepTimerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayedPosition = sliderPosition ?: position
    val sliderValue = remember(displayedPosition, duration) {
        if (duration <= 0L) 0f else {
            (displayedPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(artworkBackdropBrush(accent)),
    ) {
        AsyncImage(
            model = mediaMetadata.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .let { base -> if (disableBlur) base else base.blur(72.dp) },
            alpha = 0.38f,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.22f),
                            Color.Black.copy(alpha = 0.08f),
                            TamedAppleColors.Background.copy(alpha = 0.52f),
                            TamedAppleColors.Background.copy(alpha = 0.18f),
                        )
                    )
                ),
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top + WindowInsetsSides.Bottom
                    )
                )
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            val compactLayout = maxHeight < 820.dp
            val bottomInset = WindowInsets.systemBars.only(WindowInsetsSides.Bottom)
                .asPaddingValues()
                .calculateBottomPadding()
            val artworkSize = minOf(
                maxWidth - 8.dp,
                if (compactLayout) maxHeight * 0.34f else maxHeight * 0.4f,
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(5.dp)
                        .clip(TamedAppleShapes.pill)
                        .background(Color.White.copy(alpha = 0.36f))
                        .clickable(onClick = state::collapseSoft),
                )

                Box(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Thumbnail(
                        sliderPositionProvider = { sliderPosition },
                        modifier = Modifier
                            .size(artworkSize)
                            .clip(RoundedCornerShape(28.dp)),
                        isPlayerExpanded = true,
                        embeddedMode = true,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = bottomInset + 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mediaMetadata.title,
                                style = if (compactLayout) {
                                    MaterialTheme.typography.headlineSmall.copy(
                                        color = TamedAppleColors.PrimaryText,
                                        fontWeight = FontWeight.Bold,
                                    )
                                } else {
                                    MaterialTheme.typography.headlineMedium.copy(
                                        color = TamedAppleColors.PrimaryText,
                                        fontWeight = FontWeight.Bold,
                                    )
                                },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(),
                            )
                            Text(
                                text = mediaMetadata.artists.joinToString { it.name },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TamedAppleColors.SecondaryText,
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .basicMarquee(),
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            HeaderActionButton(
                                iconRes = R.drawable.share,
                                onClick = {
                                    clipboardManager.setPrimaryClip(
                                        android.content.ClipData.newPlainText(
                                            mediaMetadata.title,
                                            "https://music.youtube.com/watch?v=${mediaMetadata.id}",
                                        )
                                    )
                                },
                            )
                            HeaderActionButton(
                                iconRes = if (currentSongLiked) R.drawable.favorite else R.drawable.favorite_border,
                                onClick = playerConnection::toggleLike,
                                active = currentSongLiked,
                            )
                            HeaderActionButton(
                                iconRes = R.drawable.more_horiz,
                                onClick = {
                                    menuState.show {
                                        PlayerMenu(
                                            mediaMetadata = mediaMetadata,
                                            navController = navController,
                                            playerBottomSheetState = state,
                                            onShowDetailsDialog = {
                                                bottomSheetPageState.show {
                                                    ShowMediaInfo(mediaMetadata.id)
                                                }
                                            },
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            )
                        }
                    }

                    Slider(
                        value = sliderValue,
                        onValueChange = { fraction ->
                            if (duration > 0L) {
                                onSeekTo((duration * fraction).toLong())
                            }
                        },
                        onValueChangeFinished = onSeekFinished,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.22f),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (compactLayout) 16.dp else 18.dp),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = makeTimeString(displayedPosition),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TamedAppleColors.SecondaryText,
                            ),
                        )
                        Text(
                            text = makeTimeString(duration.coerceAtLeast(0L)),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TamedAppleColors.SecondaryText,
                            ),
                        )
                    }

                    TransportBar(
                        isPlaying = isPlaying,
                        canSkipPrevious = canSkipPrevious,
                        canSkipNext = canSkipNext,
                        accent = accent,
                        onPrevious = playerConnection::seekToPrevious,
                        onPlayPause = { playerConnection.player.togglePlayPause() },
                        onNext = playerConnection::seekToNext,
                        modifier = Modifier.padding(top = if (compactLayout) 18.dp else 22.dp),
                    )

                    FooterActionsRow(
                        items = listOf(
                            FooterAction(
                                iconRes = R.drawable.queue_music,
                                label = "Queue",
                                active = false,
                                onClick = onShowQueue,
                            ),
                            FooterAction(
                                iconRes = R.drawable.shuffle,
                                label = "Shuffle",
                                active = shuffleModeEnabled,
                                onClick = onToggleShuffle,
                            ),
                            FooterAction(
                                iconRes = when (repeatMode) {
                                    Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                    else -> R.drawable.repeat
                                },
                                label = "Repeat",
                                active = repeatMode != Player.REPEAT_MODE_OFF,
                                onClick = onToggleRepeat,
                            ),
                            FooterAction(
                                iconRes = R.drawable.lyrics,
                                label = "Lyrics",
                                active = false,
                                onClick = onShowLyrics,
                            ),
                        ),
                        modifier = Modifier.padding(top = 14.dp),
                    )

                    AnimatedVisibility(visible = sleepTimerEnabled && sleepTimerTimeLeft > 0L) {
                        Text(
                            text = "Sleep timer ${makeTimeString(sleepTimerTimeLeft)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TamedAppleColors.SecondaryText,
                                fontWeight = FontWeight.Medium,
                            ),
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

private data class FooterAction(
    val iconRes: Int,
    val label: String,
    val active: Boolean,
    val onClick: () -> Unit,
)

@Composable
private fun HeaderActionButton(
    iconRes: Int,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (active) Color.White else Color.White.copy(alpha = 0.18f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (active) Color.Black else Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun TransportBar(
    isPlaying: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    accent: Color,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(TamedAppleShapes.panel)
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransportButton(
            iconRes = R.drawable.skip_previous,
            onClick = onPrevious,
            enabled = canSkipPrevious,
            accent = accent,
            large = false,
        )
        TransportButton(
            iconRes = if (isPlaying) R.drawable.pause else R.drawable.play,
            onClick = onPlayPause,
            enabled = true,
            accent = Color.White,
            large = true,
        )
        TransportButton(
            iconRes = R.drawable.skip_next,
            onClick = onNext,
            enabled = canSkipNext,
            accent = accent,
            large = false,
        )
    }
}

@Composable
private fun FooterActionsRow(
    items: List<FooterAction>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        items.forEach { item ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = item.onClick)
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    painter = painterResource(item.iconRes),
                    contentDescription = item.label,
                    tint = if (item.active) Color.White else TamedAppleColors.SecondaryText,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (item.active) Color.White else TamedAppleColors.SecondaryText,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TransportButton(
    iconRes: Int,
    onClick: () -> Unit,
    enabled: Boolean,
    accent: Color,
    large: Boolean,
) {
    val size = if (large) 82.dp else 58.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(if (large) TamedAppleShapes.panel else RoundedCornerShape(18.dp))
            .background(
                if (large) Color.White else accent.copy(alpha = if (enabled) 0.22f else 0.08f)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (large) Color.Black else Color.White.copy(alpha = if (enabled) 1f else 0.42f),
            modifier = Modifier.size(if (large) 30.dp else 24.dp),
        )
    }
}

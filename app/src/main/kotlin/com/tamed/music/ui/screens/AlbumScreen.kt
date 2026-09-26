/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.tamed.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import com.tamed.music.ui.screens.rememberAlbumCanvas
import com.tamed.music.ui.player.CanvasArtworkPlayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.sp
import com.tamed.music.ui.component.glassButtonColors
import com.tamed.music.ui.component.GlassIconCircleButton
import com.tamed.music.ui.component.AppleHeroPlayButton
import com.tamed.music.ui.component.ApplePillPlayButton
import com.tamed.music.ui.theme.SfProFontFamily
import androidx.compose.ui.text.TextStyle
import com.tamed.music.ui.component.GlassActionPill
import com.tamed.music.ui.component.appleGlassEffect
import androidx.compose.material3.HorizontalDivider
import com.tamed.music.ui.component.shimmer.ShimmerHost
import com.tamed.music.ui.component.shimmer.ButtonPlaceholder
import com.tamed.music.ui.component.shimmer.ListItemPlaceHolder
import com.tamed.music.ui.component.shimmer.TextPlaceholder
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.tamed.music.ui.component.ToggleButton
import com.tamed.music.ui.component.glassToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.tamed.music.extensions.togglePlayPause
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.tamed.music.constants.AppBarHeight
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastForEachReversed
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.tamed.music.LocalDatabase
import com.tamed.music.LocalDownloadUtil
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.R
import com.tamed.music.constants.HideExplicitKey
import com.tamed.music.db.entities.Album
import com.tamed.music.playback.ExoDownloadService
import com.tamed.music.playback.queues.LocalAlbumRadio
import com.tamed.music.ui.component.AlbumGradient
import com.tamed.music.ui.component.ExplicitTag
import com.tamed.music.ui.theme.AmbientBackdrop
import com.tamed.music.constants.ArtistBackgroundStyleKey
import com.tamed.music.constants.TamedCanvasKey

import com.tamed.music.ui.component.IconButton

import com.tamed.music.ui.component.LocalMenuState
import com.tamed.music.ui.component.NavigationTitle
import com.tamed.music.ui.component.SongListItem
import com.tamed.music.ui.component.YouTubeGridItem
import com.tamed.music.ui.menu.AlbumMenu
import com.tamed.music.ui.menu.SelectionSongMenu
import com.tamed.music.ui.menu.SongMenu
import com.tamed.music.ui.menu.YouTubeAlbumMenu
import com.tamed.music.ui.utils.backToMain

import com.tamed.music.constants.SwipeToSongKey
import com.tamed.music.extensions.toMediaItem
import com.tamed.music.ui.component.SwipeToSongBox
import com.tamed.music.utils.rememberPreference
import com.tamed.music.utils.navigateToArtist
import com.tamed.music.viewmodels.AlbumViewModel
import com.tamed.music.ui.component.ExpandableText
import com.tamed.music.ui.theme.TamedAppleColors
import com.tamed.music.ui.theme.TamedAppleTypography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.layout.navigationBarsPadding
import com.tamed.music.App
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlbumScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return

    val scope = rememberCoroutineScope()

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlistId by viewModel.playlistId.collectAsState()
    val albumWithSongs by viewModel.albumWithSongs.collectAsState()
    val otherVersions by viewModel.otherVersions.collectAsState()

    val description by viewModel.albumDescription.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val (tamedCanvasEnabled) = rememberPreference(key = TamedCanvasKey, defaultValue = false)
    val swipeToSongEnabled by rememberPreference(key = SwipeToSongKey, defaultValue = true)
    val filteredSongs = remember(albumWithSongs, hideExplicit) {
        var songs = albumWithSongs?.songs ?: emptyList()
        if (hideExplicit) {
            songs = songs.filter { !it.song.explicit }
        }
        
        songs
    }

    var showAlbumDescriptionSheet by rememberSaveable { mutableStateOf(false) }
    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }
    if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    LaunchedEffect(filteredSongs) {
        selection.fastForEachReversed { songId ->
            if (filteredSongs.find { it.id == songId } == null) {
                selection.remove(songId)
            }
        }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(albumWithSongs) {
        if (albumWithSongs?.album?.isLocal == true) {
            downloadState = Download.STATE_COMPLETED
            return@LaunchedEffect
        }
        val songs = albumWithSongs?.songs?.map { it.id }
        if (songs.isNullOrEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it]?.state == Download.STATE_QUEUED ||
                                downloads[it]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    val hasExplicitContent = remember(albumWithSongs) {
        albumWithSongs?.album?.explicit == true
    }
    val albumArtists = remember(albumWithSongs) {
        albumWithSongs?.artists?.takeIf { it.isNotEmpty() }
            ?: albumWithSongs?.songs?.firstOrNull()?.artists.orEmpty()
    }
    val albumArtistNames = remember(albumArtists) { albumArtists.joinToString { it.name } }

    val lazyListState = rememberLazyListState()

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
        }
    }

    val albumThumbnail = albumWithSongs?.album?.thumbnailUrl ?: com.tamed.music.ui.theme.BackdropCache.get(viewModel.albumId)
    LaunchedEffect(albumThumbnail) {
        if (albumThumbnail != null) {
            com.tamed.music.ui.theme.BackdropCache.put(viewModel.albumId, albumThumbnail)
        }
    }

    AmbientBackdrop(
        thumbnailUrl = albumThumbnail,
        forceStyle = com.tamed.music.constants.HomeBackgroundStyle.BACKDROP
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
        val albumWithSongs = albumWithSongs
        if (albumWithSongs != null && albumWithSongs.songs.isNotEmpty()) {
            item(key = "album_header") {
                val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
                val headerOffset = with(LocalDensity.current) {
                    -(systemBarsTopPadding + AppBarHeight).roundToPx()
                }

                val firstSongTitle = albumWithSongs.songs.firstOrNull()?.title
                val canvasArtwork = rememberAlbumCanvas(
                    albumTitle = albumWithSongs.album.title,
                    artistName = albumArtists.firstOrNull()?.name,
                    firstSongTitle = firstSongTitle,
                    enabled = tamedCanvasEnabled,
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    if (canvasArtwork != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .graphicsLayer {
                                    translationY = headerOffset.toFloat()
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .drawWithCache {
                                    val fadeBrush = Brush.verticalGradient(
                                        0.0f to Color.White,
                                        0.50f to Color.White,
                                        0.70f to Color.White.copy(alpha = 0.80f),
                                        0.84f to Color.White.copy(alpha = 0.40f),
                                        0.94f to Color.White.copy(alpha = 0.12f),
                                        1.0f to Color.Transparent,
                                    )
                                    onDrawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = fadeBrush,
                                            blendMode = BlendMode.DstIn,
                                        )
                                    }
                                }
                        ) {
                            CanvasArtworkPlayer(
                                primaryUrl = canvasArtwork.preferredAnimationUrl,
                                fallbackUrl = canvasArtwork.videoUrl,
                                isPlaying = true,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        // Removed custom header gradient to let ambient backdrop show uniformly
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(20.dp))

                        // Album Artwork
                        if (canvasArtwork == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 56.dp)
                                    .aspectRatio(1f)
                                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(18.dp))
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                            ) {
                                AsyncImage(
                                    model = albumWithSongs.album.thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                        } else {
                            Spacer(Modifier.height(240.dp))
                        }

                        // Metadata
                        val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = albumWithSongs.album.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color.White else Color(0xFF1C1C1E),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(6.dp))

                            val artistColor = if (isDarkTheme) Color.White.copy(alpha = 0.85f) else Color(0xFF1C1C1E).copy(alpha = 0.85f)
                            if (albumArtists.size == 1 && albumWithSongs.artists.isNotEmpty()) {
                                val artist = albumArtists.first()
                                Text(
                                    text = artist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = artistColor,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable {
                                        navController.navigateToArtist(context, artist.id, artist.name)
                                    }
                                )
                            } else if (albumArtistNames.isNotBlank()) {
                                val linkStyles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = artistColor,
                                        textDecoration = TextDecoration.None
                                    )
                                )
                                Text(
                                    text = if (albumWithSongs.artists.isNotEmpty()) {
                                        buildAnnotatedString {
                                            albumWithSongs.artists.fastForEachIndexed { idx, artist ->
                                                val link = LinkAnnotation.Clickable(
                                                    tag = artist.id,
                                                    styles = linkStyles,
                                                ) {
                                                    navController.navigateToArtist(context, artist.id, artist.name)
                                                }
                                                withLink(link) { append(artist.name) }
                                                if (idx != albumWithSongs.artists.lastIndex) append(", ")
                                            }
                                        }
                                    } else {
                                        buildAnnotatedString { append(albumArtistNames) }
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = artistColor,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (hasExplicitContent) {
                                    ExplicitTag(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        size = 14.dp
                                    )
                                }
                                Text(
                                    text = buildString {
                                        append(stringResource(R.string.album_text))
                                        albumWithSongs.album.year?.let { append(" \u2022 $it") }
                                        append(" \u2022 ${albumWithSongs.songs.size} tracks")
                                        val totalSec = albumWithSongs.songs.sumOf { it.song.duration }
                                        val h = totalSec / 3600
                                        val m = (totalSec % 3600) / 60
                                        if (h > 0) append(" \u2022 ${h}h ${m}m") else append(" \u2022 ${m}m")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Apple Music 3-Button Action Row (Shuffle, Pill Play, Favorite)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Circular Shuffle Button
                            GlassIconCircleButton(
                                iconRes = R.drawable.shuffle,
                                contentDescription = stringResource(R.string.shuffle),
                                onClick = {
                                    playerConnection.playQueue(
                                        LocalAlbumRadio(albumWithSongs.copy(songs = albumWithSongs.songs.shuffled()))
                                    )
                                },
                                buttonSize = 40.dp,
                                solid = false,
                            )

                            Spacer(Modifier.width(12.dp))

                            // 2. Solid White Pill Play Button
                            val isAlbumPlaying = isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id
                            ApplePillPlayButton(
                                onClick = {
                                    if (isAlbumPlaying) {
                                        playerConnection.player.pause()
                                    } else if (mediaMetadata?.album?.id == albumWithSongs.album.id) {
                                        playerConnection.player.play()
                                    } else {
                                        playerConnection.playQueue(LocalAlbumRadio(albumWithSongs, startIndex = 0))
                                    }
                                },
                                isPlaying = isAlbumPlaying,
                                modifier = Modifier.width(134.dp),
                            )

                            Spacer(Modifier.width(12.dp))

                            // 3. Circular Favorite Button
                            val isSaved = albumWithSongs.album.bookmarkedAt != null
                            GlassIconCircleButton(
                                iconRes = if (isSaved) R.drawable.favorite else R.drawable.favorite_border,
                                contentDescription = if (isSaved) stringResource(R.string.saved) else stringResource(R.string.save),
                                onClick = {
                                    database.query { update(albumWithSongs.album.toggleLike()) }
                                },
                                accent = if (isSaved) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                buttonSize = 40.dp,
                                solid = false,
                            )
                        }

                        if (!description.isNullOrBlank()) {
                            val cleanDesc = description.orEmpty().replace("From Wikipedia (", "").trim()
                            Spacer(Modifier.height(18.dp))
                            ExpandableText(
                                text = cleanDesc,
                                collapsedMaxLines = 2,
                                textAlign = TextAlign.Start,
                                onClick = { showAlbumDescriptionSheet = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }


            if (filteredSongs.isNotEmpty()) {
                itemsIndexed(
                    items = filteredSongs,
                    key = { _, song -> song.id },
                ) { index, song ->
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    val textPrimary = if (isDarkTheme) Color.White else Color(0xFF1C1C1E)
                    val textSecondary = if (isDarkTheme) Color.White.copy(alpha = 0.55f) else Color(0xFF1C1C1E).copy(alpha = 0.55f)
                    val isItemActive = song.id == mediaMetadata?.id
                    val activeRowBg = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

                    val onCheckedChange: (Boolean) -> Unit = {
                        if (it) {
                            selection.add(song.id)
                        } else {
                            selection.remove(song.id)
                        }
                    }

                    val content: @Composable () -> Unit = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 1.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (isItemActive) {
                                        Modifier.background(activeRowBg)
                                    } else {
                                        Modifier
                                    }
                                )
                                .combinedClickable(
                                    onClick = {
                                         if (inSelectMode) {
                                             onCheckedChange(song.id !in selection)
                                         } else if (song.id == mediaMetadata?.id) {
                                             if (playerConnection.player.playWhenReady) {
                                                 playerConnection.player.pause()
                                             } else {
                                                 playerConnection.player.play()
                                             }
                                         } else {
                                             val player = playerConnection.player
                                             val isCurrentAlbum = mediaMetadata?.album?.id == albumWithSongs.album.id ||
                                                 (playerConnection.service.currentQueue as? LocalAlbumRadio)?.albumWithSongs?.album?.id == albumWithSongs.album.id
                                             val songIndexInPlayer = if (isCurrentAlbum) {
                                                 (0 until player.mediaItemCount).firstOrNull {
                                                     player.getMediaItemAt(it).mediaId == song.id
                                                 }
                                             } else null

                                             if (songIndexInPlayer != null) {
                                                 player.seekToDefaultPosition(songIndexInPlayer)
                                                 player.playWhenReady = true
                                             } else {
                                                 playerConnection.playQueue(
                                                     LocalAlbumRadio(albumWithSongs, startIndex = index),
                                                 )
                                             }
                                         }
                                    },
                                    onLongClick = {
                                        if (inSelectMode) {
                                            onCheckedChange(song.id !in selection)
                                        } else {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        }
                                    },
                                )
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (inSelectMode) {
                                Checkbox(
                                    checked = song.id in selection,
                                    onCheckedChange = onCheckedChange,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier.width(22.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    if (isItemActive && isPlaying) {
                                        Icon(
                                            painter = painterResource(R.drawable.volume_up),
                                            contentDescription = "Playing",
                                            tint = primaryColor,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    } else {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 14.sp,
                                                fontWeight = if (isItemActive) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isItemActive) textPrimary else textSecondary,
                                            ),
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = song.song.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.5.sp,
                                            fontWeight = if (isItemActive) FontWeight.SemiBold else FontWeight.Normal,
                                            color = textPrimary,
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    if (song.song.explicit) {
                                        ExplicitTag(
                                            color = textSecondary,
                                            size = 12.dp,
                                        )
                                    }
                                }

                                val artistString = song.artists.joinToString { it.name }
                                val subtitle = if (artistString.isNotBlank() && artistString != albumArtistNames) {
                                    artistString
                                } else {
                                    null
                                }

                                if (subtitle != null) {
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            color = textSecondary,
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, radius = 16.dp),
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        }
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.more_horiz),
                                    contentDescription = null,
                                    tint = if (isItemActive) textPrimary.copy(alpha = 0.85f) else textSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    if (swipeToSongEnabled && !inSelectMode) {
                        SwipeToSongBox(
                            mediaItem = song.toMediaItem(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            content()
                        }
                    } else {
                        content()
                    }
                }
            }

            if (otherVersions.isNotEmpty()) {
                item(key = "other_versions_title") {
                    NavigationTitle(
                        title = stringResource(R.string.other_versions),
                        modifier = Modifier.animateItem()
                    )
                }
                item(key = "other_versions_list") {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                    ) {
                        items(
                            items = otherVersions.distinctBy { it.id },
                            key = { it.id },
                        ) { item ->
                            YouTubeGridItem(
                                item = item,
                                isActive = mediaMetadata?.album?.id == item.id,
                                isPlaying = isPlaying,
                                coroutineScope = scope,
                                modifier =
                                Modifier
                                    .combinedClickable(
                                        onClick = { navController.navigate("album/${item.id}") },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                YouTubeAlbumMenu(
                                                    albumItem = item,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(Modifier.height(50.dp))
            }
        } else {
            item(key = "loading") {
                ShimmerHost(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(80.dp))
                    
                    // Album Cover Placeholder
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 56.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // Title Placeholder
                    TextPlaceholder(modifier = Modifier.width(200.dp))
                    Spacer(Modifier.height(8.dp))
                    
                    // Artist Placeholder
                    TextPlaceholder(modifier = Modifier.width(120.dp))
                    Spacer(Modifier.height(16.dp))
                    
                    // Metadata Placeholder
                    TextPlaceholder(modifier = Modifier.width(160.dp))
                    Spacer(Modifier.height(24.dp))
                    
                    // Buttons Placeholder
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ButtonPlaceholder(modifier = Modifier.weight(1f))
                        ButtonPlaceholder(modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        ButtonPlaceholder(modifier = Modifier.weight(1f))
                        ButtonPlaceholder(modifier = Modifier.weight(1f))
                        ButtonPlaceholder(modifier = Modifier.weight(1f))
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    
                    // Songs Placeholder
                    repeat(6) {
                        ListItemPlaceHolder()
                    }
                }
            }
        }
    }

    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val appBarAlpha by animateFloatAsState(
        targetValue = if (transparentAppBar) 0f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "appBarAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val alpha = appBarAlpha
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F0F11).copy(alpha = alpha * 0.85f),
                            Color(0xFF0F0F11).copy(alpha = alpha * 0.40f),
                            Color.Transparent
                        )
                    )
                )
            }
            .padding(horizontal = 20.dp)
            .padding(
                top = systemBarsTopPadding + 8.dp,
                bottom = 12.dp
            )
    ) {
        if (inSelectMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onExitSelectionMode) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
                Text(
                    text = pluralStringResource(R.plurals.n_selected, selection.size, selection.size),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = selection.size == filteredSongs.size && selection.isNotEmpty(),
                        onCheckedChange = {
                            if (selection.size == filteredSongs.size) {
                                selection.clear()
                            } else {
                                selection.clear()
                                selection.addAll(filteredSongs.map { it.id })
                            }
                        }
                    )
                    IconButton(
                        enabled = selection.isNotEmpty(),
                        onClick = {
                            menuState.show {
                                SelectionSongMenu(
                                    songSelection = selection.mapNotNull { songId ->
                                        filteredSongs.find { it.id == songId }
                                    },
                                    onDismiss = menuState::dismiss,
                                    clearAction = onExitSelectionMode
                                )
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconCircleButton(
                    iconRes = R.drawable.arrow_back,
                    contentDescription = stringResource(R.string.back_button_desc),
                    onClick = navController::navigateUp,
                    onPhoto = true,
                    buttonSize = 36.dp,
                )

                if (!transparentAppBar) {
                    Text(
                        text = albumWithSongs?.album?.title.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GlassIconCircleButton(
                        iconRes = R.drawable.share,
                        contentDescription = "Share",
                        onClick = {
                            val intent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    "https://music.youtube.com/playlist?list=${albumWithSongs?.album?.playlistId}"
                                )
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, null))
                        },
                        onPhoto = true,
                        buttonSize = 36.dp,
                    )

                    GlassIconCircleButton(
                        iconRes = R.drawable.more_horiz,
                        contentDescription = "More options",
                        onClick = {
                            albumWithSongs?.let { album ->
                                menuState.show {
                                    AlbumMenu(
                                        originalAlbum = Album(album = album.album, artists = album.artists),
                                        navController = navController,
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        },
                        onPhoto = true,
                        buttonSize = 36.dp,
                    )
                }
            }
        }
    }

    if (showAlbumDescriptionSheet && !description.isNullOrBlank()) {
        val cleanDesc = description.orEmpty().replace("From Wikipedia (", "").trim()
        ModalBottomSheet(
            onDismissRequest = { showAlbumDescriptionSheet = false },
            containerColor = TamedAppleColors.Surface,
            contentColor = Color.White,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "About ${albumWithSongs?.album?.title ?: "Album"}",
                    style = TamedAppleTypography.sectionTitle(),
                )
                Text(
                    text = cleanDesc,
                    style = TextStyle(
                        fontFamily = SfProFontFamily,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Start,
                    ),
                )
            }
        }
    }
    }
}

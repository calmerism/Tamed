@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.luminance
import com.tamed.music.ui.component.EmptyPlaceholder
import com.tamed.music.ui.theme.SfProFontFamily
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import com.tamed.music.ui.component.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tamed.music.LocalDatabase
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.R
import com.tamed.music.constants.AppBarHeight
import com.tamed.music.constants.DisableBlurKey
import com.tamed.music.constants.HideExplicitKey
import com.tamed.music.constants.HomeBackgroundStyle
import com.tamed.music.db.entities.PlaylistEntity
import com.tamed.music.db.entities.PlaylistSongMap
import com.tamed.music.extensions.metadata
import com.tamed.music.extensions.toMediaItem
import com.tamed.music.extensions.togglePlayPause
import com.tamed.music.innertube.models.SongItem
import com.tamed.music.innertube.models.WatchEndpoint
import com.tamed.music.models.toMediaMetadata
import com.tamed.music.playback.queues.YouTubeQueue
import com.tamed.music.ui.component.GlassIconCircleButton
import com.tamed.music.ui.component.AppleHeroPlayButton
import com.tamed.music.ui.component.ApplePillPlayButton
import com.tamed.music.ui.component.AlbumGradient
import com.tamed.music.ui.component.DraggableScrollbar
import com.tamed.music.ui.component.IconButton
import com.tamed.music.ui.component.LocalMenuState
import com.tamed.music.ui.component.YouTubeListItem
import com.tamed.music.ui.component.shimmer.ButtonPlaceholder
import com.tamed.music.ui.component.shimmer.ListItemPlaceHolder
import com.tamed.music.ui.component.shimmer.ShimmerHost
import com.tamed.music.ui.component.shimmer.TextPlaceholder
import com.tamed.music.ui.menu.SelectionMediaMetadataMenu
import com.tamed.music.ui.menu.YouTubePlaylistMenu
import com.tamed.music.ui.menu.YouTubeSongMenu
import com.tamed.music.ui.theme.PlayerColorExtractor
import com.tamed.music.ui.utils.ItemWrapper
import com.tamed.music.ui.utils.backToMain
import com.tamed.music.ui.utils.formatCompactCount
import com.tamed.music.utils.rememberPreference
import com.tamed.music.utils.navigateToArtist
import com.tamed.music.viewmodels.OnlinePlaylistViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TopAppBarDefaults
import com.tamed.music.ui.theme.AmbientBackdrop
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val viewCounts by viewModel.viewCounts.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val error by viewModel.error.collectAsState()

    var selection by remember { mutableStateOf(false) }
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)

    // System bars padding
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pullRefreshState = rememberPullToRefreshState()

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by
        rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    val filteredSongs =
        remember(songs, query) {
            if (query.text.isEmpty()) {
                songs.mapIndexed { index, song -> index to song }
            } else {
                songs
                    .mapIndexed { index, song -> index to song }
                    .filter { (_, song) ->
                        song.title.contains(query.text, ignoreCase = true) ||
                            song.artists.fastAny { it.name.contains(query.text, ignoreCase = true) }
                    }
            }
        }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (selection) {
        BackHandler { selection = false }
    }

    val wrappedSongs =
        remember(filteredSongs) { filteredSongs.map { item -> ItemWrapper(item) } }
            .toMutableStateList()

    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 } }

    // Gradient colors state for playlist cover
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Extract gradient colors from playlist cover
    LaunchedEffect(playlist?.thumbnail) {
        val thumbnailUrl = playlist?.thumbnail
        if (thumbnailUrl != null) {
            val request =
                ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .size(
                        PlayerColorExtractor.Config.IMAGE_SIZE,
                        PlayerColorExtractor.Config.IMAGE_SIZE
                    )
                    .allowHardware(false)
                    .build()

            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()

            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette =
                        withContext(Dispatchers.Default) {
                            Palette.from(bitmap)
                                .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                                .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                                .generate()
                        }

                    val extractedColors =
                        PlayerColorExtractor.extractGradientColors(
                            palette = palette,
                            fallbackColor = fallbackColor
                        )
                    gradientColors = extractedColors
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    // Calculate gradient opacity based on scroll position
    val gradientAlpha by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex == 0) {
                val offset = lazyListState.firstVisibleItemScrollOffset
                (1f - (offset / 600f)).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    val transparentAppBar by remember {
        derivedStateOf { !disableBlur && !selection && !showTopBarTitle }
    }



    val headerItems by remember {
        derivedStateOf {
            val current = playlist
            if (!isLoading && current != null && !isSearching) 1 else 0
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (
                    songs.size >= 5 &&
                        lastVisibleIndex != null &&
                        lastVisibleIndex >= songs.size - 5
                ) {
                    viewModel.loadMoreSongs()
                }
            }
    }

    val playlistThumbnail = playlist?.thumbnail
        ?: dbPlaylist?.playlist?.thumbnailUrl
        ?: com.tamed.music.ui.theme.BackdropCache.get(viewModel.playlistId)

    LaunchedEffect(playlistThumbnail) {
        if (playlistThumbnail != null) {
            com.tamed.music.ui.theme.BackdropCache.put(viewModel.playlistId, playlistThumbnail)
        }
    }

    AmbientBackdrop(
        modifier = Modifier.fillMaxSize(),
        thumbnailUrl = playlistThumbnail,
        forceStyle = HomeBackgroundStyle.BACKDROP
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh
            ),
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding =
                LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues(),
        ) {
            playlist.let { playlist ->
                if (isLoading) {
                    // Shimmer Loading State
                    item(key = "shimmer") {
                        ShimmerHost {
                            Column(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(top = systemBarsTopPadding + AppBarHeight),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Playlist art placeholder
                                Box(
                                    modifier =
                                        Modifier.padding(top = 8.dp, bottom = 20.dp)
                                            .size(240.dp)
                                            .shimmer()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.onSurface)
                                )

                                // Title placeholder
                                TextPlaceholder(
                                    height = 28.dp,
                                    modifier =
                                        Modifier.fillMaxWidth(0.6f).padding(horizontal = 32.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Author placeholder
                                TextPlaceholder(
                                    height = 20.dp,
                                    modifier = Modifier.fillMaxWidth(0.4f)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Metadata placeholder
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    repeat(2) {
                                        TextPlaceholder(
                                            height = 32.dp,
                                            modifier = Modifier.width(80.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Buttons placeholder
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                                ) {
                                    Box(
                                        modifier =
                                            Modifier.size(48.dp)
                                                .shimmer()
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurface)
                                    )
                                    ButtonPlaceholder(modifier = Modifier.weight(1f).height(48.dp))
                                    ButtonPlaceholder(modifier = Modifier.weight(1f).height(48.dp))
                                    Box(
                                        modifier =
                                            Modifier.size(48.dp)
                                                .shimmer()
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurface)
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            repeat(6) { ListItemPlaceHolder() }
                        }
                    }
                } else if (playlist != null) {
                    if (!isSearching) {
                        // Hero Header
                        item(key = "header") {
                            val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                            val textPrimary = if (isDarkTheme) Color.White else Color(0xFF1C1C1E)
                            val textSecondary = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color(0xFF1C1C1E).copy(alpha = 0.65f)

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(Modifier.height(20.dp))

                                    // Playlist Artwork
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
                                            model = playlist.thumbnail,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    Spacer(Modifier.height(24.dp))

                                    // Metadata
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = playlist.title,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = SfProFontFamily,
                                            color = textPrimary,
                                            letterSpacing = (-0.3).sp,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )

                                        Spacer(Modifier.height(6.dp))

                                        playlist.author?.let { artist ->
                                            Text(
                                                text = artist.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontFamily = SfProFontFamily,
                                                color = textSecondary,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.clickable { 
                                                    val artistId = artist.id
                                                    if (artistId != null) {
                                                        navController.navigateToArtist(context, artistId, artist.name, null)
                                                    }
                                                }
                                            )

                                            Spacer(Modifier.height(4.dp))
                                        }

                                        val details = buildString {
                                            append("Playlist")
                                            if (songs.isNotEmpty()) {
                                                append(" \u2022 ${songs.size} tracks")
                                                val totalSec = songs.sumOf { it.duration ?: 0 }
                                                if (totalSec > 0) {
                                                    val h = totalSec / 3600
                                                    val m = (totalSec % 3600) / 60
                                                    if (h > 0) append(" \u2022 ${h}h ${m}m") else append(" \u2022 ${m}m")
                                                }
                                            } else if (!playlist.songCountText.isNullOrBlank()) {
                                                append(" \u2022 ${playlist.songCountText}")
                                            }
                                        }
                                        Text(
                                            text = details,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = SfProFontFamily,
                                            color = textSecondary,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    Spacer(Modifier.height(20.dp))

                                    val hasLike = playlist.id != "LM"
                                    val hasPlay = playlist.playEndpoint != null
                                    val hasShuffle = playlist.shuffleEndpoint != null

                                    // Apple Music 3-Button Action Row (Shuffle, Play, Save)
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
                                                if (hasShuffle) {
                                                    playerConnection.playQueue(YouTubeQueue(playlist.shuffleEndpoint!!))
                                                } else if (songs.isNotEmpty()) {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue(
                                                            endpoint = playlist.playEndpoint ?: WatchEndpoint(videoId = songs.first().id),
                                                            preloadItem = songs.first().toMediaMetadata(),
                                                        )
                                                    )
                                                }
                                            },
                                            buttonSize = 40.dp,
                                            solid = false,
                                        )

                                        Spacer(Modifier.width(12.dp))

                                        // 2. Solid White Pill Play Button
                                        val isPlaylistPlaying = isPlaying && songs.any { it.id == mediaMetadata?.id }
                                        ApplePillPlayButton(
                                            onClick = {
                                                if (isPlaylistPlaying) {
                                                    playerConnection.player.pause()
                                                } else if (mediaMetadata?.id != null && songs.any { it.id == mediaMetadata?.id }) {
                                                    playerConnection.player.play()
                                                } else if (hasPlay) {
                                                    playerConnection.playQueue(YouTubeQueue(playlist.playEndpoint!!))
                                                } else if (songs.isNotEmpty()) {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue(
                                                            endpoint = WatchEndpoint(videoId = songs.first().id),
                                                            preloadItem = songs.first().toMediaMetadata(),
                                                        )
                                                    )
                                                }
                                            },
                                            isPlaying = isPlaylistPlaying,
                                            modifier = Modifier.width(134.dp),
                                        )

                                        Spacer(Modifier.width(12.dp))

                                        // 3. Circular Save Button
                                        if (hasLike) {
                                            val isBookmarked = dbPlaylist?.playlist?.bookmarkedAt != null
                                            GlassIconCircleButton(
                                                iconRes = if (isBookmarked) R.drawable.favorite else R.drawable.favorite_border,
                                                contentDescription = if (isBookmarked) stringResource(R.string.saved) else stringResource(R.string.save),
                                                onClick = {
                                                    if (dbPlaylist?.playlist == null) {
                                                        database.transaction {
                                                            val playlistEntity = PlaylistEntity(
                                                                name = playlist.title,
                                                                browseId = playlist.id,
                                                                thumbnailUrl = playlist.thumbnail,
                                                                isEditable = playlist.isEditable,
                                                                playEndpointParams = playlist.playEndpoint?.params,
                                                                shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                                                radioEndpointParams = playlist.radioEndpoint?.params
                                                            ).toggleLike()
                                                            insert(playlistEntity)
                                                            songs.onEach { song -> insert(song.toMediaMetadata()) }
                                                                .mapIndexed { index, song ->
                                                                    PlaylistSongMap(
                                                                        songId = song.id,
                                                                        playlistId = playlistEntity.id,
                                                                        position = index,
                                                                        setVideoId = song.setVideoId,
                                                                    )
                                                                }.forEach(::insert)
                                                        }
                                                    } else {
                                                        database.transaction {
                                                            val currentPlaylist = dbPlaylist!!.playlist
                                                            update(currentPlaylist, playlist)
                                                            update(currentPlaylist.toggleLike())
                                                        }
                                                    }
                                                },
                                                accent = if (isBookmarked) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                                buttonSize = 40.dp,
                                                solid = false,
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))
                                }
                            }
                        }

                        if (wrappedSongs.isEmpty()) {
                            item(key = "empty") {
                                EmptyPlaceholder(
                                    icon = R.drawable.music_note,
                                    text = stringResource(R.string.empty_playlist),
                                )
                            }
                        }
                    }

                    // Songs List
                    items(items = wrappedSongs, key = { it.item.second.id }) { song ->
                        YouTubeListItem(
                            item = song.item.second,
                            viewCountText =
                                viewCounts[song.item.second.id]?.let { count ->
                                    formatCompactCount(count.toLong())
                                },
                            isActive = mediaMetadata?.id == song.item.second.id,
                            isPlaying = isPlaying,
                            isSelected = song.isSelected && selection,
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            YouTubeSongMenu(
                                                song = song.item.second,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                    onLongClick = {},
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_horiz),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            },
                            modifier =
                                Modifier.combinedClickable(
                                        enabled = !hideExplicit || !song.item.second.explicit,
                                        onClick = {
                                            if (!selection) {
                                                if (song.item.second.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue(
                                                            song.item.second
                                                                .toPlaylistPlaybackEndpoint(
                                                                    playlistId = playlist.id,
                                                                    playlistPlayParams =
                                                                        playlist.playEndpoint
                                                                            ?.params,
                                                                ),
                                                            song.item.second.toMediaMetadata(),
                                                        ),
                                                    )
                                                }
                                            } else {
                                                song.isSelected = !song.isSelected
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(
                                                HapticFeedbackType.LongPress
                                            )
                                            if (selection) {
                                                song.isSelected = !song.isSelected
                                            } else {
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song.item.second,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            }
                                        },
                                    )
                                    .animateItem(),
                        )
                    }

                    if (viewModel.continuation != null && songs.isNotEmpty() && isLoadingMore) {
                        item(key = "loading_more") {
                            ShimmerHost { repeat(2) { ListItemPlaceHolder() } }
                        }
                    }
                } else {
                    val isPrivatePlaylist = error?.contains("PLAYLIST_PRIVATE") == true
                    item(key = "error") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isPrivatePlaylist) {
                                Image(
                                    painter = painterResource(R.drawable.lock),
                                    contentDescription = null,
                                    modifier = Modifier.size(120.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.playlist_private_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.playlist_private_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text =
                                        if (error != null) {
                                            stringResource(R.string.error_unknown)
                                        } else {
                                            stringResource(R.string.playlist_not_found)
                                        },
                                    style = MaterialTheme.typography.titleLarge,
                                    color =
                                        if (error != null) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text =
                                        if (error != null) {
                                            error!!
                                        } else {
                                            stringResource(R.string.playlist_not_found_desc)
                                        },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (error != null) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { viewModel.retry() }, shapes = ButtonDefaults.shapes()) {
                                        Text(stringResource(R.string.retry))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        DraggableScrollbar(
            modifier =
                Modifier.padding(
                        LocalPlayerAwareWindowInsets.current
                            .union(WindowInsets.ime)
                            .asPaddingValues()
                    )
                    .align(Alignment.CenterEnd),
            scrollState = lazyListState,
            headerItems = headerItems
        )

        val appBarAlpha by animateFloatAsState(
            targetValue = if (transparentAppBar) 0f else 1f,
            animationSpec = tween(durationMillis = 200),
            label = "appBarAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F0F11).copy(alpha = appBarAlpha * 0.85f),
                            Color(0xFF0F0F11).copy(alpha = appBarAlpha * 0.40f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 20.dp)
                .padding(
                    top = systemBarsTopPadding + 8.dp,
                    bottom = 12.dp
                )
        ) {
            if (selection) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { selection = false }, onLongClick = {}) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                    val count = wrappedSongs.count { it.isSelected }
                    Text(
                        text = pluralStringResource(R.plurals.n_song, count, count),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = SfProFontFamily,
                        color = Color.White,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (count == wrappedSongs.size) {
                                    wrappedSongs.forEach { it.isSelected = false }
                                } else {
                                    wrappedSongs.forEach { it.isSelected = true }
                                }
                            },
                            onLongClick = {}
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (count == wrappedSongs.size) R.drawable.deselect
                                    else R.drawable.select_all
                                ),
                                contentDescription = null,
                                tint = Color.White,
                            )
                        }
                        IconButton(
                            enabled = count > 0,
                            onClick = {
                                menuState.show {
                                    SelectionMediaMetadataMenu(
                                        songSelection = wrappedSongs
                                            .filter { it.isSelected }
                                            .map { it.item.second.toMediaItem().metadata!! },
                                        onDismiss = menuState::dismiss,
                                        clearAction = { selection = false },
                                        currentItems = emptyList()
                                    )
                                }
                            },
                            onLongClick = {}
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_horiz),
                                contentDescription = null,
                                tint = Color.White,
                            )
                        }
                    }
                }
            } else if (isSearching) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassIconCircleButton(
                        iconRes = R.drawable.arrow_back,
                        contentDescription = stringResource(R.string.back_button_desc),
                        onClick = {
                            isSearching = false
                            query = TextFieldValue()
                        },
                        onPhoto = true,
                        buttonSize = 36.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = SfProFontFamily,
                                color = Color.White.copy(alpha = 0.5f),
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = SfProFontFamily,
                            color = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.weight(1f).focusRequester(focusRequester)
                    )
                    if (query.text.isNotEmpty()) {
                        IconButton(onClick = { query = TextFieldValue() }, onLongClick = {}) {
                            Icon(
                                painter = painterResource(R.drawable.close),
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

                    if (showTopBarTitle) {
                        Text(
                            text = playlist?.title.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        GlassIconCircleButton(
                            iconRes = R.drawable.search,
                            contentDescription = stringResource(R.string.search),
                            onClick = { isSearching = true },
                            onPhoto = true,
                            buttonSize = 36.dp,
                        )

                        GlassIconCircleButton(
                            iconRes = R.drawable.more_horiz,
                            contentDescription = "More options",
                            onClick = {
                                playlist?.let { pl ->
                                    menuState.show {
                                        YouTubePlaylistMenu(
                                            playlist = pl,
                                            songs = songs,
                                            coroutineScope = coroutineScope,
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
        }

        PullToRefreshDefaults.Indicator(
            isRefreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier.windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
                    )
                    .align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun MetadataChip(icon: Int, text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

private fun SongItem.toPlaylistPlaybackEndpoint(
    playlistId: String,
    playlistPlayParams: String?,
): WatchEndpoint {
    val baseEndpoint = endpoint ?: WatchEndpoint(videoId = id)
    return baseEndpoint.copy(
        videoId = baseEndpoint.videoId ?: id,
        playlistId = baseEndpoint.playlistId ?: playlistId,
        playlistSetVideoId = baseEndpoint.playlistSetVideoId ?: setVideoId,
        params = baseEndpoint.params ?: playlistPlayParams,
    )
}

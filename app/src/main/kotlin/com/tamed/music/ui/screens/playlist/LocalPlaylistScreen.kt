/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.tamed.music.ui.screens.playlist

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import com.tamed.music.ui.component.ToggleButton
import com.tamed.music.ui.component.glassToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastSumBy
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tamed.music.LocalDatabase
import com.tamed.music.LocalDownloadUtil
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.LocalSyncUtils
import com.tamed.music.R
import com.tamed.music.constants.AppBarHeight
import com.tamed.music.constants.DisableBlurKey
import com.tamed.music.constants.PlaylistEditLockKey
import com.tamed.music.constants.PlaylistSongSortDescendingKey
import com.tamed.music.constants.PlaylistSongSortType
import com.tamed.music.constants.PlaylistSongSortTypeKey
import com.tamed.music.constants.SwipeToSongKey
import com.tamed.music.db.entities.Playlist
import com.tamed.music.db.entities.PlaylistSong
import com.tamed.music.db.entities.PlaylistSongMap
import com.tamed.music.extensions.move
import com.tamed.music.extensions.toMediaItem
import com.tamed.music.extensions.togglePlayPause
import com.tamed.music.innertube.YouTube
import com.tamed.music.innertube.models.SongItem
import com.tamed.music.innertube.utils.completed
import com.tamed.music.models.toMediaMetadata
import com.tamed.music.playback.ExoDownloadService
import com.tamed.music.playback.queues.ListQueue
import com.tamed.music.playback.queues.LocalMixQueue
import com.tamed.music.ui.component.DefaultDialog
import com.tamed.music.ui.component.EditPlaylistDialog
import com.tamed.music.ui.component.DraggableScrollbar
import com.tamed.music.ui.component.EmptyPlaceholder
import com.tamed.music.ui.component.PlaylistTagChips
import com.tamed.music.ui.component.AssignTagsDialog
import com.tamed.music.ui.component.GlassIconCircleButton
import com.tamed.music.ui.component.AppleHeroPlayButton
import com.tamed.music.ui.component.ApplePillPlayButton
import com.tamed.music.ui.component.IconButton
import com.tamed.music.ui.component.LocalMenuState
import com.tamed.music.ui.component.SongListItem
import com.tamed.music.ui.component.SortHeader
import com.tamed.music.ui.component.shimmer.ButtonPlaceholder
import com.tamed.music.ui.component.shimmer.ListItemPlaceHolder
import com.tamed.music.ui.component.shimmer.ShimmerHost
import com.tamed.music.ui.component.shimmer.TextPlaceholder
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.sp
import com.tamed.music.ui.menu.PlaylistMenu
import com.tamed.music.ui.menu.SelectionSongMenu
import com.tamed.music.ui.menu.removeSongFromRemotePlaylist
import com.tamed.music.ui.menu.SongMenu
import com.tamed.music.ui.theme.SfProFontFamily
import com.tamed.music.ui.screens.playlist.PlaylistSuggestionsSection
import com.tamed.music.ui.theme.PlayerColorExtractor
import com.tamed.music.ui.utils.ItemWrapper
import com.tamed.music.ui.utils.backToMain
import com.tamed.music.ui.utils.formatCompactCount
import com.tamed.music.utils.makeTimeString
import com.tamed.music.utils.rememberEnumPreference
import com.tamed.music.utils.rememberPreference
import com.tamed.music.viewmodels.LocalPlaylistViewModel
import com.valentinilk.shimmer.shimmer
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDateTime
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TopAppBarDefaults
import com.tamed.music.constants.HomeBackgroundStyle
import com.tamed.music.ui.theme.AmbientBackdrop
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween

@SuppressLint("RememberReturnType")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LocalPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: LocalPlaylistViewModel = hiltViewModel(),
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
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val mutableSongs = remember { mutableStateListOf<PlaylistSong>() }
    val playlistLength = remember(songs) {
        songs.fastSumBy { it.song.song.duration }
    }
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        PlaylistSongSortTypeKey,
        PlaylistSongSortType.CUSTOM
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(
        PlaylistSongSortDescendingKey,
        true
    )
    var locked by rememberPreference(PlaylistEditLockKey, defaultValue = true)
    val swipeToSongEnabled by rememberPreference(SwipeToSongKey, defaultValue = true)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)
    var showAssignTagsDialog by remember { mutableStateOf(false) }

    if (showAssignTagsDialog && playlist != null) {
        AssignTagsDialog(
            database = database,
            playlistId = playlist!!.id,
            onDismiss = { showAssignTagsDialog = false }
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pullRefreshState = rememberPullToRefreshState()

    // System bars padding
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) {
            songs
        } else {
            songs.filter { song ->
                song.song.song.title.contains(query.text, ignoreCase = true) ||
                        song.song.artists.fastAny { it.name.contains(query.text, ignoreCase = true) }
            }
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    var selection by remember { mutableStateOf(false) }

    val wrappedSongs = remember(filteredSongs) {
        filteredSongs.map { item -> ItemWrapper(item) }
    }.toMutableStateList()

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (selection) {
        BackHandler {
            selection = false
        }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableStateOf(Download.STATE_STOPPED) }

    val editable: Boolean = playlist?.playlist?.isEditable == true

    LaunchedEffect(songs) {
        mutableSongs.apply {
            clear()
            addAll(songs)
        }
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it.song.id]?.state == Download.STATE_QUEUED ||
                                downloads[it.song.id]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it.song.id]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        playlist?.let { playlistData ->
            EditPlaylistDialog(
                initialName = playlistData.playlist.name,
                initialThumbnailUrl = playlistData.playlist.thumbnailUrl,
                fallbackThumbnails = playlistData.songThumbnails.filterNotNull(),
                onDismiss = { showEditDialog = false },
                onSave = { name, thumbnailUrl ->
                    database.query {
                        update(
                            playlistData.playlist.copy(
                                name = name,
                                thumbnailUrl = thumbnailUrl,
                                lastUpdateTime = LocalDateTime.now(),
                            )
                        )
                    }
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        playlistData.playlist.browseId?.let { YouTube.renamePlaylist(it, name) }
                    }
                },
            )
        }
    }

    var showRemoveDownloadDialog by remember { mutableStateOf(false) }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(
                        R.string.remove_download_playlist_confirm,
                        playlist?.playlist!!.name
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = { showRemoveDownloadDialog = false },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        if (!editable) {
                            database.transaction {
                                playlist?.id?.let { clearPlaylist(it) }
                            }
                        }
                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.song.id,
                                false
                            )
                        }
                    },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    var showDeletePlaylistDialog by remember { mutableStateOf(false) }
    if (showDeletePlaylistDialog) {
        DefaultDialog(
            onDismiss = { showDeletePlaylistDialog = false },
            content = {
                Text(
                    text = stringResource(
                        R.string.delete_playlist_confirm,
                        playlist?.playlist!!.name
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = { showDeletePlaylistDialog = false },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                        database.query {
                            playlist?.let { delete(it.playlist) }
                        }
                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            playlist?.playlist?.browseId?.let { YouTube.deletePlaylist(it) }
                        }
                        navController.popBackStack()
                    },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    val headerItems by remember {
        derivedStateOf {
            val current = playlist
            val hasContent = current != null &&
                (current.songCount > 0 || current.playlist.remoteSongCount != 0)
            if (hasContent && !isSearching) 2 else 0
        }
    }
    val lazyListState = rememberLazyListState()
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        scrollThresholdPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    ) { from, to ->
        if (to.index >= headerItems && from.index >= headerItems) {
            val currentDragInfo = dragInfo
            dragInfo = if (currentDragInfo == null) {
                (from.index - headerItems) to (to.index - headerItems)
            } else {
                currentDragInfo.first to (to.index - headerItems)
            }
            mutableSongs.move(from.index - headerItems, to.index - headerItems)
        }
    }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                database.transaction {
                    move(viewModel.playlistId, from, to)
                }

                if (viewModel.playlist.value?.playlist?.browseId != null) {
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        val playlistSongMap = database.playlistSongMaps(viewModel.playlistId, 0)
                        val successorIndex = if (from > to) to else to + 1
                        val successorSetVideoId = playlistSongMap.getOrNull(successorIndex)?.setVideoId

                        playlistSongMap.getOrNull(from)?.setVideoId?.let { setVideoId ->
                            YouTube.moveSongPlaylist(
                                viewModel.playlist.value?.playlist?.browseId!!,
                                setVideoId,
                                successorSetVideoId
                            )
                        }
                    }
                }
                dragInfo = null
            }
        }
    }

    val showTopBarTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }

    // Gradient colors state for playlist cover
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Extract gradient colors from playlist cover
    LaunchedEffect(playlist?.thumbnails) {
        val thumbnailUrl = playlist?.thumbnails?.firstOrNull()
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context)
                .data(thumbnailUrl)
                .size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE)
                .allowHardware(false)
                .build()

            val result = runCatching {
                context.imageLoader.execute(request)
            }.getOrNull()

            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette = withContext(Dispatchers.Default) {
                        Palette.from(bitmap)
                            .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                            .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                            .generate()
                    }

                    val extractedColors = PlayerColorExtractor.extractGradientColors(
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
        derivedStateOf {
            !disableBlur && !selection && !showTopBarTitle
        }
    }



    val playlistThumbnail = playlist?.thumbnails?.firstOrNull()
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
            contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues(),
        ) {
            playlist?.let { playlist ->
                if (playlist.songCount == 0 && playlist.playlist.remoteSongCount == 0) {
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.music_note,
                            text = stringResource(R.string.playlist_is_empty),
                        )
                    }
                } else {
                    if (!isSearching) {
                        // Hero Header
                        item(key = "header") {
                            val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                            val textPrimary = if (isDarkTheme) Color.White else Color(0xFF1C1C1E)
                            val textSecondary = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color(0xFF1C1C1E).copy(alpha = 0.65f)

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
                                    if (playlist.thumbnails.size == 1) {
                                        AsyncImage(
                                            model = playlist.thumbnails[0],
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else if (playlist.thumbnails.size > 1) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            Row(modifier = Modifier.weight(1f)) {
                                                AsyncImage(
                                                    model = playlist.thumbnails.getOrNull(0),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                                )
                                                AsyncImage(
                                                    model = playlist.thumbnails.getOrNull(1),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                                )
                                            }
                                            Row(modifier = Modifier.weight(1f)) {
                                                AsyncImage(
                                                    model = playlist.thumbnails.getOrNull(2),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                                )
                                                AsyncImage(
                                                    model = playlist.thumbnails.getOrNull(3),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                                )
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.queue_music),
                                                contentDescription = null,
                                                modifier = Modifier.size(80.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Playlist Name
                                Text(
                                    text = playlist.playlist.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily,
                                    color = textPrimary,
                                    letterSpacing = (-0.3).sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Metadata Subtitle
                                val songCount = if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
                                    playlist.playlist.remoteSongCount
                                } else {
                                    playlist.songCount
                                }
                                val details = buildString {
                                    append("Playlist")
                                    if (songCount > 0) {
                                        append(" \u2022 $songCount tracks")
                                    }
                                    if (playlistLength > 0) {
                                        val h = playlistLength / 3600
                                        val m = (playlistLength % 3600) / 60
                                        if (h > 0) append(" \u2022 ${h}h ${m}m") else append(" \u2022 ${m}m")
                                    }
                                }
                                Text(
                                    text = details,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = SfProFontFamily,
                                    color = textSecondary,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // Apple Music 3-Button Action Row (Shuffle, Play, Like/Delete)
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
                                                ListQueue(
                                                    title = playlist.playlist.name,
                                                    items = songs.shuffled().map { it.song.toMediaItem() },
                                                )
                                            )
                                        },
                                        buttonSize = 40.dp,
                                        solid = false,
                                    )

                                    Spacer(Modifier.width(12.dp))

                                    // 2. Solid White Pill Play Button
                                    ApplePillPlayButton(
                                        onClick = {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = playlist.playlist.name,
                                                    items = songs.map { it.song.toMediaItem() },
                                                )
                                            )
                                        },
                                        modifier = Modifier.width(134.dp),
                                    )

                                    Spacer(Modifier.width(12.dp))

                                    // 3. Like / Delete Button
                                    if (editable) {
                                        GlassIconCircleButton(
                                            iconRes = R.drawable.delete,
                                            contentDescription = "Delete",
                                            onClick = { showDeletePlaylistDialog = true },
                                            buttonSize = 40.dp,
                                            solid = false,
                                        )
                                    } else {
                                        val liked = playlist.playlist.bookmarkedAt != null
                                        GlassIconCircleButton(
                                            iconRes = if (liked) R.drawable.favorite else R.drawable.favorite_border,
                                            contentDescription = if (liked) stringResource(R.string.saved) else stringResource(R.string.save),
                                            onClick = {
                                                database.transaction {
                                                    update(playlist.playlist.toggleLike())
                                                }
                                            },
                                            accent = if (liked) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                            buttonSize = 40.dp,
                                            solid = false,
                                        )
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                // Secondary Action Buttons (Download, Edit/Sync)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    GlassIconCircleButton(
                                        iconRes = when (downloadState) {
                                            Download.STATE_COMPLETED -> R.drawable.offline
                                            else -> R.drawable.download
                                        },
                                        contentDescription = stringResource(R.string.download),
                                        onClick = {
                                            when (downloadState) {
                                                Download.STATE_COMPLETED -> {
                                                    showRemoveDownloadDialog = true
                                                }
                                                Download.STATE_DOWNLOADING -> {
                                                    songs.forEach { song ->
                                                        DownloadService.sendRemoveDownload(
                                                            context,
                                                            ExoDownloadService::class.java,
                                                            song.song.id,
                                                            false,
                                                        )
                                                    }
                                                }
                                                else -> {
                                                    songs.forEach { song ->
                                                        val downloadRequest = DownloadRequest
                                                            .Builder(song.song.id, song.song.id.toUri())
                                                            .setCustomCacheKey(song.song.id)
                                                            .setData(song.song.song.title.toByteArray())
                                                            .build()
                                                        DownloadService.sendAddDownload(
                                                            context,
                                                            ExoDownloadService::class.java,
                                                            downloadRequest,
                                                            false,
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        accent = if (downloadState == Download.STATE_COMPLETED) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                        solid = false,
                                    )

                                    if (editable) {
                                        GlassIconCircleButton(
                                            iconRes = R.drawable.edit,
                                            contentDescription = stringResource(R.string.edit),
                                            onClick = { showEditDialog = true },
                                            solid = false,
                                        )
                                    } else if (playlist.playlist.browseId != null) {
                                        GlassIconCircleButton(
                                            iconRes = R.drawable.sync,
                                            contentDescription = "Sync",
                                            onClick = {
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    val playlistPage = YouTube.playlist(playlist.playlist.browseId)
                                                        .completed()
                                                        .getOrNull() ?: return@launch
                                                    database.transaction {
                                                        clearPlaylist(playlist.id)
                                                        playlistPage.songs
                                                            .map(SongItem::toMediaMetadata)
                                                            .onEach(::insert)
                                                            .mapIndexed { index, song ->
                                                                PlaylistSongMap(
                                                                    songId = song.id,
                                                                    playlistId = playlist.id,
                                                                    position = index,
                                                                )
                                                            }
                                                            .forEach(::insert)
                                                    }
                                                }
                                            },
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 20.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Start Mix Button
                                    Button(
                                        onClick = {
                                            playerConnection.playQueue(
                                                LocalMixQueue(
                                                    database = database,
                                                    playlistId = playlist.id,
                                                    maxMixSize = 50,
                                                ),
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shapes = ButtonDefaults.shapes(),
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.mix),
                                            contentDescription = "Start Mix",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }

                    // Sort Header
                    item(key = "sort_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 16.dp),
                        ) {
                            SortHeader(
                                sortType = sortType,
                                sortDescending = sortDescending,
                                onSortTypeChange = onSortTypeChange,
                                onSortDescendingChange = onSortDescendingChange,
                                sortTypeText = { sortType ->
                                    when (sortType) {
                                        PlaylistSongSortType.CUSTOM -> R.string.sort_by_custom
                                        PlaylistSongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                        PlaylistSongSortType.NAME -> R.string.sort_by_name
                                        PlaylistSongSortType.ARTIST -> R.string.sort_by_artist
                                        PlaylistSongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            if (editable && sortType == PlaylistSongSortType.CUSTOM) {
                                IconButton(
                                    onClick = { locked = !locked },
                                    onLongClick = {},
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(if (locked) R.drawable.lock else R.drawable.lock_open),
                                        contentDescription = null,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Songs List
            if (!selection) {
                itemsIndexed(
                    items = if (isSearching) filteredSongs else mutableSongs,
                    key = { _, song -> song.map.id },
                ) { index, song ->
                    ReorderableItem(
                        state = reorderableState,
                        key = song.map.id,
                        modifier = Modifier.graphicsLayer {
                            compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                        }
                    ) {
                        val currentItem by rememberUpdatedState(song)

                        fun deleteFromPlaylist() {
                            val map = currentItem.map
                            val browseId = playlist?.playlist?.browseId
                            coroutineScope.launch(Dispatchers.IO) {
                                database.withTransaction {
                                    move(map.playlistId, map.position, Int.MAX_VALUE)
                                    delete(map.copy(position = Int.MAX_VALUE))
                                }
                                if (browseId != null) {
                                    removeSongFromRemotePlaylist(browseId, map)
                                }
                            }
                        }

                        val dismissBoxState = rememberSwipeToDismissBoxState(
                            positionalThreshold = { totalDistance -> totalDistance },
                            confirmValueChange = { targetValue ->
                                targetValue == SwipeToDismissBoxValue.Settled || !lazyListState.isScrollInProgress
                            }
                        )
                        var processedDismiss by remember { mutableStateOf(false) }
                        LaunchedEffect(dismissBoxState.currentValue) {
                            val dv = dismissBoxState.currentValue
                            if (!processedDismiss && (
                                        dv == SwipeToDismissBoxValue.StartToEnd ||
                                                dv == SwipeToDismissBoxValue.EndToStart
                                        )
                            ) {
                                processedDismiss = true
                                deleteFromPlaylist()
                            }
                            if (dv == SwipeToDismissBoxValue.Settled) {
                                processedDismiss = false
                            }
                        }

                        val content: @Composable () -> Unit = {
                            SongListItem(
                                song = song.song,
                                viewCountText =
                                    viewCounts[song.song.id]?.let { count -> formatCompactCount(count.toLong()) },
                                isActive = song.song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                showInLibraryIcon = true,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song.song,
                                                    playlistSong = song,
                                                    playlistBrowseId = playlist?.playlist?.browseId,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                        onLongClick = {},
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }

                                    if (sortType == PlaylistSongSortType.CUSTOM && !locked && !selection && !isSearching && editable) {
                                        IconButton(
                                            onClick = { },
                                            onLongClick = {},
                                            modifier = Modifier
                                                .draggableHandle()
                                                .graphicsLayer { alpha = 0.99f },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.drag_handle),
                                                contentDescription = null,
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (song.song.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                val player = playerConnection.player
                                                val isCurrentPlaylist = playerConnection.queueTitle.value == playlist?.playlist?.name
                                                val songIndexInPlayer = if (isCurrentPlaylist) {
                                                    (0 until player.mediaItemCount).firstOrNull {
                                                        player.getMediaItemAt(it).mediaId == song.song.id
                                                    }
                                                } else null

                                                if (songIndexInPlayer != null) {
                                                    player.seekToDefaultPosition(songIndexInPlayer)
                                                    player.playWhenReady = true
                                                } else {
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = playlist!!.playlist.name,
                                                            items = songs.map { it.song.toMediaItem() },
                                                            startIndex = songs.indexOfFirst { it.map.id == song.map.id },
                                                        ),
                                                    )
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (selection) {
                                                wrappedSongs.find { it.item.map.id == song.map.id }?.let { it.isSelected = !it.isSelected }
                                            } else {
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = song.song,
                                                        playlistSong = song,
                                                        playlistBrowseId = playlist?.playlist?.browseId,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            }
                                        },
                                    ),
                            )
                        }

                        if (locked || selection || swipeToSongEnabled) {
                            content()
                        } else {
                            SwipeToDismissBox(
                                state = dismissBoxState,
                                backgroundContent = {},
                            ) {
                                content()
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = wrappedSongs,
                    key = { _, song -> song.item.map.id },
                ) { index, songWrapper ->
                    ReorderableItem(
                        state = reorderableState,
                        key = songWrapper.item.map.id,
                        modifier = Modifier.graphicsLayer {
                            compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                        }
                    ) {
                        val currentItem by rememberUpdatedState(songWrapper.item)

                        fun deleteFromPlaylist() {
                            val map = currentItem.map
                            coroutineScope.launch(Dispatchers.IO) {
                                database.withTransaction {
                                    move(map.playlistId, map.position, Int.MAX_VALUE)
                                    delete(map.copy(position = Int.MAX_VALUE))
                                }
                            }
                        }

                        val dismissBoxState = rememberSwipeToDismissBoxState(
                            positionalThreshold = { totalDistance -> totalDistance },
                            confirmValueChange = { targetValue ->
                                targetValue == SwipeToDismissBoxValue.Settled || !lazyListState.isScrollInProgress
                            }
                        )
                        var processedDismiss2 by remember { mutableStateOf(false) }
                        LaunchedEffect(dismissBoxState.currentValue) {
                            val dv = dismissBoxState.currentValue
                            if (!processedDismiss2 && (
                                        dv == SwipeToDismissBoxValue.StartToEnd ||
                                                dv == SwipeToDismissBoxValue.EndToStart
                                        )
                            ) {
                                processedDismiss2 = true
                                deleteFromPlaylist()
                            }
                            if (dv == SwipeToDismissBoxValue.Settled) {
                                processedDismiss2 = false
                            }
                        }

                        val content: @Composable () -> Unit = {
                            SongListItem(
                                song = songWrapper.item.song,
                                viewCountText =
                                    viewCounts[songWrapper.item.song.id]?.let { count -> formatCompactCount(count.toLong()) },
                                isActive = songWrapper.item.song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                showInLibraryIcon = true,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = songWrapper.item.song,
                                                    playlistBrowseId = playlist?.playlist?.browseId,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                        onLongClick = {},
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    if (sortType == PlaylistSongSortType.CUSTOM && !locked && !selection && !isSearching && editable) {
                                        IconButton(
                                            onClick = { },
                                            onLongClick = {},
                                            modifier = Modifier
                                                .draggableHandle()
                                                .graphicsLayer { alpha = 0.99f },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.drag_handle),
                                                contentDescription = null,
                                            )
                                        }
                                    }
                                },
                                isSelected = songWrapper.isSelected && selection,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (!selection) {
                                                if (songWrapper.item.song.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = playlist!!.playlist.name,
                                                            items = songs.map { it.song.toMediaItem() },
                                                            startIndex = index,
                                                        ),
                                                    )
                                                }
                                            } else {
                                                songWrapper.isSelected = !songWrapper.isSelected
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (selection) {
                                                songWrapper.isSelected = !songWrapper.isSelected
                                            } else {
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = songWrapper.item.song,
                                                        playlistBrowseId = playlist?.playlist?.browseId,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            }
                                        },
                                    ),
                            )
                        }

                        if (locked || !editable || swipeToSongEnabled) {
                            content()
                        } else {
                            SwipeToDismissBox(
                                state = dismissBoxState,
                                backgroundContent = {},
                            ) {
                                content()
                            }
                        }
                    }
                }
            }

            // Playlist Suggestions Section
            if (!selection && !isSearching) {
                item {
                    PlaylistSuggestionsSection(
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }

        }

        DraggableScrollbar(
            modifier = Modifier
                .padding(
                    LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
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
                                    if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all
                                ),
                                contentDescription = null,
                                tint = Color.White,
                            )
                        }

                        IconButton(
                            enabled = count > 0,
                            onClick = {
                                menuState.show {
                                    SelectionSongMenu(
                                        songSelection = wrappedSongs.filter { it.isSelected }
                                            .map { it.item.song },
                                        songPosition = wrappedSongs.filter { it.isSelected }
                                            .map { it.item.map },
                                        onDismiss = menuState::dismiss,
                                        clearAction = {
                                            selection = false
                                            wrappedSongs.clear()
                                        }
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
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
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
                            text = playlist?.playlist?.name.orEmpty(),
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
                                        PlaylistMenu(
                                            playlist = pl,
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss,
                                            songList = songs.map { it.song },
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
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime))
                .align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun MetadataChip(
    icon: Int,
    text: String,
    modifier: Modifier = Modifier
) {
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

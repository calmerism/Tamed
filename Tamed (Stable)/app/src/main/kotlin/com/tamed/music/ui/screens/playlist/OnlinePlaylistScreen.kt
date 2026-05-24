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
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.tamed.music.db.entities.PlaylistEntity
import com.tamed.music.db.entities.PlaylistSongMap
import com.tamed.music.extensions.metadata
import com.tamed.music.extensions.toMediaItem
import com.tamed.music.extensions.togglePlayPause
import com.tamed.music.innertube.models.SongItem
import com.tamed.music.innertube.models.WatchEndpoint
import com.tamed.music.models.toMediaMetadata
import com.tamed.music.playback.queues.YouTubeQueue
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
import com.tamed.music.viewmodels.OnlinePlaylistViewModel

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
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
                            val headerOffset = with(LocalDensity.current) {
                                -(systemBarsTopPadding + AppBarHeight).roundToPx()
                            }
                            
                            Box(modifier = Modifier.fillMaxWidth()) {
                                // Gradient background
                                AlbumGradient(
                                    thumbnailUrl = playlist.thumbnail,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .offset { IntOffset(x = 0, y = headerOffset) }
                                )
                                
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
                                            .clip(RoundedCornerShape(12.dp))
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
                                            .padding(horizontal = 20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = playlist.title,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
            
                                        Spacer(Modifier.height(6.dp))
            
                                        playlist.author?.let { artist ->
                                            Text(
                                                text = artist.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.clickable { 
                                                    if (artist.id != null) {
                                                        navController.navigate("artist/${artist.id}")
                                                    }
                                                }
                                            )
                                            
                                            Spacer(Modifier.height(6.dp))
                                        }
                                        
                                        playlist.songCountText?.let { songCountText ->
                                            Text(
                                                text = songCountText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    Spacer(Modifier.height(20.dp))
                                    
                                    val hasLike = playlist.id != "LM"
                                    val hasPlay = playlist.playEndpoint != null
                                    val hasShuffle = playlist.shuffleEndpoint != null
                                    val hasRadio = playlist.radioEndpoint != null
                                    
                                    // Primary buttons: Save + Play
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (hasLike) {
                                            val isBookmarked = dbPlaylist?.playlist?.bookmarkedAt != null
                                            OutlinedButton(
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
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    painter = painterResource(
                                                        if (isBookmarked) R.drawable.favorite else R.drawable.favorite_border
                                                    ),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = if (isBookmarked) stringResource(R.string.saved) else stringResource(R.string.save),
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                        }
                                        
                                        if (hasPlay) {
                                            Button(
                                                onClick = {
                                                    playerConnection.playQueue(YouTubeQueue(playlist.playEndpoint!!))
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_music),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = stringResource(R.string.play),
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(Modifier.height(10.dp))
                                    
                                    // Secondary buttons: Download + Shuffle + Radio
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                    ) {
                                        // Download button for playlist
                                        val downloadChecked = false // Placeholder for playlist download state
                                        ToggleButton(
                                            checked = downloadChecked,
                                            onCheckedChange = { },
                                            modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                                        ) {
                                            Icon(painterResource(R.drawable.download), null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                            Text(stringResource(R.string.download), style = MaterialTheme.typography.labelMedium)
                                        }
                                        
                                        if (hasShuffle) {
                                            ToggleButton(
                                                checked = false,
                                                onCheckedChange = {
                                                    playerConnection.playQueue(YouTubeQueue(playlist.shuffleEndpoint!!))
                                                },
                                                modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                                shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
                                            ) {
                                                Icon(painterResource(R.drawable.shuffle), null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                                Text(stringResource(R.string.shuffle), style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                        
                                        if (hasRadio) {
                                            ToggleButton(
                                                checked = false,
                                                onCheckedChange = {
                                                    playerConnection.playQueue(YouTubeQueue(playlist.radioEndpoint!!))
                                                },
                                                modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                                            ) {
                                                Icon(painterResource(R.drawable.radio), null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                                Text(stringResource(R.string.start_radio), style = MaterialTheme.typography.labelMedium)
                                            }
                                        } else {
                                            ToggleButton(
                                                checked = false,
                                                onCheckedChange = { },
                                                modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                                            ) {
                                                Icon(painterResource(R.drawable.share), null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                                Text(stringResource(R.string.share), style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "empty") {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.empty_playlist),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.empty_playlist_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    onLongClick = {}
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null,
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
                                            if (!selection) {
                                                selection = true
                                            }
                                            wrappedSongs.forEach { it.isSelected = false }
                                            song.isSelected = true
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

        // Top App Bar
        val topAppBarColors =
            if (transparentAppBar) {
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            } else {
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            }

        TopAppBar(
            colors = topAppBarColors,
            title = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    Text(
                        text = pluralStringResource(R.plurals.n_song, count, count),
                        style = MaterialTheme.typography.titleLarge
                    )
                } else if (isSearching) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search),
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                    )
                } else if (showTopBarTitle) {
                    Text(playlist?.title.orEmpty())
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (isSearching) {
                            isSearching = false
                            query = TextFieldValue()
                        } else if (selection) {
                            selection = false
                        } else {
                            navController.navigateUp()
                        }
                    },
                    onLongClick = {
                        if (!isSearching && !selection) {
                            navController.backToMain()
                        }
                    }
                ) {
                    Icon(
                        painter =
                            painterResource(
                                if (selection) R.drawable.close else R.drawable.arrow_back
                            ),
                        contentDescription = null
                    )
                }
            },
            actions = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
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
                            painter =
                                painterResource(
                                    if (count == wrappedSongs.size) R.drawable.deselect
                                    else R.drawable.select_all
                                ),
                            contentDescription = null
                        )
                    }
                    IconButton(
                        onClick = {
                            menuState.show {
                                SelectionMediaMetadataMenu(
                                    songSelection =
                                        wrappedSongs
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
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                } else if (!isSearching) {
                    IconButton(onClick = { isSearching = true }, onLongClick = {}) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null
                        )
                    }
                }
            }
        )

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

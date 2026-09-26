/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.ui.screens.artist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TopAppBarDefaults
import com.tamed.music.ui.theme.AmbientBackdrop
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.tamed.music.innertube.models.AlbumItem
import com.tamed.music.innertube.models.ArtistItem
import com.tamed.music.utils.navigateToArtist
import com.tamed.music.innertube.models.PlaylistItem
import com.tamed.music.innertube.models.SongItem
import com.tamed.music.innertube.models.WatchEndpoint
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.R
import com.tamed.music.constants.GridThumbnailHeight
import com.tamed.music.extensions.togglePlayPause
import com.tamed.music.models.toMediaMetadata
import com.tamed.music.extensions.toMediaItem
import com.tamed.music.playback.queues.ListQueue
import com.tamed.music.playback.queues.YouTubeQueue
import com.tamed.music.LocalDatabase
import com.tamed.music.ui.component.GlassActionPill
import com.tamed.music.ui.component.GlassIconCircleButton
import com.tamed.music.ui.component.IconButton
import com.tamed.music.ui.component.LocalMenuState
import com.tamed.music.ui.component.YouTubeGridItem
import com.tamed.music.ui.component.YouTubeListItem
import com.tamed.music.ui.component.shimmer.GridItemPlaceHolder
import com.tamed.music.ui.component.shimmer.ListItemPlaceHolder
import com.tamed.music.ui.component.shimmer.ShimmerHost
import com.tamed.music.ui.menu.YouTubeAlbumMenu
import com.tamed.music.ui.menu.YouTubeArtistMenu
import com.tamed.music.ui.menu.YouTubePlaylistMenu
import com.tamed.music.ui.menu.YouTubeSongMenu
import com.tamed.music.ui.utils.backToMain
import com.tamed.music.constants.ArtistBackgroundStyleKey
import com.tamed.music.ui.theme.AmbientBackdrop
import com.tamed.music.viewmodels.ArtistItemsViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistItemsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ArtistItemsViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val playerAwarePadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    val title by viewModel.title.collectAsState()
    val itemsPage by viewModel.itemsPage.collectAsState()
    val artistThumbnail by viewModel.artistThumbnail.collectAsState()
    val dbArtist by database.artist(viewModel.artistId ?: viewModel.browseId).collectAsState(initial = null)
    val backdropThumbnail = dbArtist?.artist?.thumbnailUrl ?: artistThumbnail ?: com.tamed.music.ui.theme.BackdropCache.get(viewModel.artistId ?: viewModel.browseId)
    LaunchedEffect(backdropThumbnail) {
        if (backdropThumbnail != null) {
            com.tamed.music.ui.theme.BackdropCache.put(viewModel.artistId ?: viewModel.browseId, backdropThumbnail)
        }
    }

    val transparentAppBar by remember {
        derivedStateOf {
            val isList = itemsPage?.items?.firstOrNull() is SongItem
            if (isList) {
                lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
            } else {
                lazyGridState.firstVisibleItemIndex == 0 && lazyGridState.firstVisibleItemScrollOffset < 100
            }
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
        }.collect { shouldLoadMore ->
            if (!shouldLoadMore) return@collect
            viewModel.loadMore()
        }
    }

    LaunchedEffect(lazyGridState) {
        snapshotFlow {
            lazyGridState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
        }.collect { shouldLoadMore ->
            if (!shouldLoadMore) return@collect
            viewModel.loadMore()
        }
    }

    AmbientBackdrop(
        modifier = Modifier.fillMaxSize(),
        thumbnailUrl = backdropThumbnail,
        forceStyle = com.tamed.music.constants.HomeBackgroundStyle.BACKDROP
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (itemsPage == null) {
                ShimmerHost(
                    modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current),
                ) {
                    repeat(8) {
                        ListItemPlaceHolder()
                    }
                }
            }
        
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val playerAwarePadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
            val layoutDirection = LocalLayoutDirection.current

            if (itemsPage?.items?.firstOrNull() is SongItem) {
                val contentPadding = remember(playerAwarePadding, statusBarTop, layoutDirection) {
                    PaddingValues(
                        start = playerAwarePadding.calculateStartPadding(layoutDirection),
                        top = statusBarTop + 68.dp,
                        end = playerAwarePadding.calculateEndPadding(layoutDirection),
                        bottom = playerAwarePadding.calculateBottomPadding() + 120.dp
                    )
                }

                LazyColumn(
                    state = lazyListState,
                    contentPadding = contentPadding,
                ) {
                    items(
                        items = itemsPage?.items.orEmpty().distinctBy { it.id },
                        key = { it.id },
                    ) { item ->
                        YouTubeListItem(
                            item = item,
                            isActive =
                            when (item) {
                                is SongItem -> mediaMetadata?.id == item.id
                                is AlbumItem -> mediaMetadata?.album?.id == item.id
                                else -> false
                            },
                            isPlaying = isPlaying,
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            when (item) {
                                                is SongItem ->
                                                    YouTubeSongMenu(
                                                        song = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
        
                                                is AlbumItem ->
                                                    YouTubeAlbumMenu(
                                                        albumItem = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
        
                                                is ArtistItem ->
                                                    YouTubeArtistMenu(
                                                        artist = item,
                                                        onDismiss = menuState::dismiss,
                                                    )
        
                                                is PlaylistItem ->
                                                    YouTubePlaylistMenu(
                                                        playlist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                            }
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier =
                            Modifier
                                .combinedClickable(
                                    onClick = {
                                        when (item) {
                                            is SongItem -> {
                                                if (item.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    val songs = itemsPage?.items
                                                        .orEmpty()
                                                        .filterIsInstance<SongItem>()
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = title,
                                                            items = songs.map { it.toMediaItem() },
                                                            startIndex = songs.indexOfFirst { it.id == item.id }.coerceAtLeast(0),
                                                        ),
                                                    )
                                                }
                                            }

                                            is AlbumItem -> navController.navigate("album/${item.id}")
                                            is ArtistItem -> navController.navigateToArtist(context, item.id, item.title, item.thumbnail)
                                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            when (item) {
                                                is SongItem ->
                                                    YouTubeSongMenu(
                                                        song = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )

                                                is AlbumItem ->
                                                    YouTubeAlbumMenu(
                                                        albumItem = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )

                                                is ArtistItem ->
                                                    YouTubeArtistMenu(
                                                        artist = item,
                                                        onDismiss = menuState::dismiss,
                                                    )

                                                is PlaylistItem ->
                                                    YouTubePlaylistMenu(
                                                        playlist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                            }
                                        }
                                    },
                                )
                                .animateItem(),
                        )
                    }
        
                    if (itemsPage?.continuation != null) {
                        item(key = "loading") {
                            ShimmerHost(Modifier.animateItem()) {
                                repeat(3) {
                                    ListItemPlaceHolder()
                                }
                            }
                        }
                    }
                }
            } else {
                val gridPadding = remember(playerAwarePadding, statusBarTop, layoutDirection) {
                    PaddingValues(
                        start = playerAwarePadding.calculateStartPadding(layoutDirection) + 12.dp,
                        top = statusBarTop + 68.dp,
                        end = playerAwarePadding.calculateEndPadding(layoutDirection) + 12.dp,
                        bottom = playerAwarePadding.calculateBottomPadding() + 120.dp
                    )
                }

                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Adaptive(minSize = GridThumbnailHeight + 24.dp),
                    contentPadding = gridPadding
                ) {
                    items(
                        items = itemsPage?.items.orEmpty().distinctBy { it.id },
                        key = { it.id }
                    ) { item ->
                        YouTubeGridItem(
                            item = item,
                            isActive = when (item) {
                                is SongItem -> mediaMetadata?.id == item.id
                                is AlbumItem -> mediaMetadata?.album?.id == item.id
                                else -> false
                            },
                            isPlaying = isPlaying,
                            fillMaxWidth = true,
                            coroutineScope = coroutineScope,
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = {
                                        when (item) {
                                            is SongItem -> {
                                                if (item.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    val songs = itemsPage?.items
                                                        .orEmpty()
                                                        .filterIsInstance<SongItem>()
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = title,
                                                            items = songs.map { it.toMediaItem() },
                                                            startIndex = songs.indexOfFirst { it.id == item.id }.coerceAtLeast(0),
                                                        ),
                                                    )
                                                }
                                            }

                                            is AlbumItem -> navController.navigate("album/${item.id}")
                                            is ArtistItem -> navController.navigateToArtist(context, item.id, item.title, item.thumbnail)
                                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            when (item) {
                                                is SongItem -> YouTubeSongMenu(
                                                    song = item,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
        
                                                is AlbumItem -> YouTubeAlbumMenu(
                                                    albumItem = item,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
        
                                                is ArtistItem -> YouTubeArtistMenu(
                                                    artist = item,
                                                    onDismiss = menuState::dismiss
                                                )
        
                                                is PlaylistItem -> YouTubePlaylistMenu(
                                                    playlist = item,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    }
                                )
                                .animateItem()
                        )
                    }
        
                    if (itemsPage?.continuation != null) {
                        item(key = "loading") {
                            ShimmerHost(Modifier.animateItem()) {
                                GridItemPlaceHolder(fillMaxWidth = true)
                            }
                        }
                    }
                }
            }
        
            val appBarAlpha by animateFloatAsState(
                targetValue = if (transparentAppBar) 0f else 1f,
                animationSpec = tween(durationMillis = 200),
                label = "appBarAlpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                        top = statusBarTop + 8.dp,
                        bottom = 12.dp
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassIconCircleButton(
                        iconRes = R.drawable.arrow_back,
                        contentDescription = stringResource(R.string.back_button_desc),
                        onClick = navController::navigateUp,
                        solid = false,
                    )

                    val songs = itemsPage?.items.orEmpty().filterIsInstance<SongItem>()
                    if (songs.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GlassIconCircleButton(
                                iconRes = R.drawable.play,
                                contentDescription = "Play",
                                onClick = {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = title,
                                            items = songs.map { it.toMediaItem() },
                                        ),
                                    )
                                },
                                solid = false,
                            )

                            GlassIconCircleButton(
                                iconRes = R.drawable.shuffle,
                                contentDescription = "Shuffle",
                                onClick = {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = title,
                                            items = songs.shuffled().map { it.toMediaItem() },
                                        ),
                                    )
                                },
                                solid = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

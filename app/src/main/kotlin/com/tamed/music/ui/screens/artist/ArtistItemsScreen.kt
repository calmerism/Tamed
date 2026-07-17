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
import androidx.navigation.NavController
import com.tamed.music.innertube.models.AlbumItem
import com.tamed.music.innertube.models.ArtistItem
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistItemsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ArtistItemsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    val title by viewModel.title.collectAsState()
    val itemsPage by viewModel.itemsPage.collectAsState()

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
        styleKey = ArtistBackgroundStyleKey,
        modifier = Modifier.fillMaxSize()
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
        
            if (itemsPage?.items?.firstOrNull() is SongItem) {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
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
                                .clickable {
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
                                        is ArtistItem -> navController.navigate("artist/${item.id}")
                                        is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                    }
                                },
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
            } else {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Adaptive(minSize = GridThumbnailHeight + 24.dp),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
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
                                            is SongItem -> playerConnection.playQueue(
                                                YouTubeQueue(
                                                    item.endpoint ?: WatchEndpoint(videoId = item.id),
                                                    item.toMediaMetadata()
                                                )
                                            )
        
                                            is AlbumItem -> navController.navigate("album/${item.id}")
                                            is ArtistItem -> navController.navigate("artist/${item.id}")
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
                                MaterialTheme.colorScheme.surface.copy(alpha = appBarAlpha * 0.9f),
                                MaterialTheme.colorScheme.surface.copy(alpha = appBarAlpha * 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                TopAppBar(
                    title = { if (!transparentAppBar) Text(title) },
                    navigationIcon = {
                        IconButton(
                            onClick = navController::navigateUp,
                            onLongClick = navController::backToMain,
                        ) {
                            Icon(
                                painterResource(R.drawable.arrow_back),
                                contentDescription = null,
                            )
                        }
                    },
                    actions = {
                        val songs = itemsPage?.items.orEmpty().filterIsInstance<SongItem>()
                        if (songs.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = title,
                                            items = songs.map { it.toMediaItem() },
                                        ),
                                    )
                                },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.play),
                                    contentDescription = null,
                                )
                            }
                            IconButton(
                                onClick = {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = title,
                                            items = songs.shuffled().map { it.toMediaItem() },
                                        ),
                                    )
                                },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.shuffle),
                                    contentDescription = null,
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    }
}

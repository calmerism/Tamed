/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.tamed.music.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import com.tamed.music.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import com.tamed.music.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.tamed.music.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.tamed.music.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.tamed.music.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import com.tamed.music.innertube.models.AlbumItem
import com.tamed.music.innertube.models.ArtistItem
import com.tamed.music.innertube.models.PlaylistItem
import com.tamed.music.innertube.models.SongItem
import com.tamed.music.innertube.models.WatchEndpoint
import com.tamed.music.innertube.models.YTItem
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.R
import com.tamed.music.constants.AppBarHeight
import com.tamed.music.constants.SearchFilterHeight
import com.tamed.music.extensions.togglePlayPause
import com.tamed.music.models.toMediaMetadata
import com.tamed.music.playback.queues.YouTubeQueue
import com.tamed.music.ui.component.ChipsRow
import com.tamed.music.ui.component.EmptyPlaceholder
import com.tamed.music.ui.component.LocalMenuState
import com.tamed.music.ui.component.YouTubeListItem
import com.tamed.music.ui.component.shimmer.ListItemPlaceHolder
import com.tamed.music.ui.component.shimmer.ShimmerHost
import com.tamed.music.ui.menu.YouTubeAlbumMenu
import com.tamed.music.ui.menu.YouTubeArtistMenu
import com.tamed.music.ui.menu.YouTubePlaylistMenu
import com.tamed.music.ui.menu.YouTubeSongMenu
import com.tamed.music.ui.theme.TamedAppleTypography
import com.tamed.music.ui.theme.appleBackgroundColor
import com.tamed.music.ui.theme.appleDividerColor
import com.tamed.music.ui.theme.applePrimaryTextColor
import com.tamed.music.viewmodels.OnlineSearchViewModel
import kotlinx.coroutines.launch
import com.tamed.music.utils.rememberPreference
import com.tamed.music.constants.PureBlackKey
import androidx.compose.foundation.layout.fillMaxSize
import com.tamed.music.ui.theme.AmbientBackdrop

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnlineSearchResult(
    navController: NavController,
    viewModel: OnlineSearchViewModel = hiltViewModel(),
) {
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val searchFilter by viewModel.filter.collectAsState()
    val searchSummary = viewModel.summaryPage
    val itemsPage by remember(searchFilter) {
        derivedStateOf {
            searchFilter?.value?.let {
                viewModel.viewStateMap[it]
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

    val ytItemContent: @Composable LazyItemScope.(YTItem) -> Unit = { item: YTItem ->
        val longClick = {
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
        }
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
                    onClick = longClick,
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
                                    playerConnection.playQueue(
                                        YouTubeQueue(
                                            WatchEndpoint(videoId = item.id),
                                            item.toMediaMetadata()
                                        )
                                    )
                                }
                            }

                            is AlbumItem -> navController.navigate("album/${item.id}")
                            is ArtistItem -> navController.navigate("artist/${item.id}")
                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                        }
                    },
                    onLongClick = longClick,
                )
                .animateItem(),
        )
    }

    AmbientBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .background(if (pureBlack) Color.Black else Color.Transparent)
        ) {
            ChipsRow(
                chips = listOf(
                    null to stringResource(R.string.filter_all),
                    FILTER_SONG to stringResource(R.string.filter_songs),
                    FILTER_VIDEO to stringResource(R.string.filter_videos),
                    FILTER_ALBUM to stringResource(R.string.filter_albums),
                    FILTER_ARTIST to stringResource(R.string.filter_artists),
                    FILTER_COMMUNITY_PLAYLIST to stringResource(R.string.filter_community_playlists),
                    FILTER_FEATURED_PLAYLIST to stringResource(R.string.filter_featured_playlists),
                ),
                currentValue = searchFilter,
                onValueUpdate = {
                    if (viewModel.filter.value != it) {
                        viewModel.filter.value = it
                    }
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(0)
                    }
                },
                icons = mapOf(
                    null to R.drawable.search,
                    FILTER_SONG to R.drawable.music_note,
                    FILTER_VIDEO to R.drawable.slow_motion_video,
                    FILTER_ALBUM to R.drawable.album,
                    FILTER_ARTIST to R.drawable.person,
                    FILTER_COMMUNITY_PLAYLIST to R.drawable.queue_music,
                    FILTER_FEATURED_PLAYLIST to R.drawable.playlist_play,
                ),
                modifier = Modifier.fillMaxWidth()
            )

            val bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(top = 4.dp, bottom = bottomPadding),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (searchFilter == null) {
                    searchSummary?.summaries?.forEachIndexed { index, summary ->
                        if (index > 0) {
                            item(key = "divider_$index") {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                                    thickness = 0.5.dp,
                                    color = appleDividerColor()
                                )
                            }
                        }

                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = summary.title,
                                    style = TamedAppleTypography.sectionTitle(),
                                    fontWeight = FontWeight.SemiBold,
                                    color = applePrimaryTextColor(),
                                )
                            }
                        }

                        items(
                            items = summary.items,
                            key = { "${summary.title}/${it.id}/${summary.items.indexOf(it)}" },
                            itemContent = ytItemContent,
                        )

                        item {
                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    if (searchSummary?.summaries?.isEmpty() == true) {
                        item {
                            EmptyPlaceholder(
                                icon = R.drawable.search,
                                text = stringResource(R.string.no_results_found),
                            )
                        }
                    }
                } else {
                    items(
                        items = itemsPage?.items.orEmpty().distinctBy { it.id },
                        key = { "filtered_${it.id}" },
                        itemContent = ytItemContent,
                    )

                    if (itemsPage?.continuation != null) {
                        item(key = "loading") {
                            ShimmerHost {
                                repeat(3) {
                                    ListItemPlaceHolder()
                                }
                            }
                        }
                    }

                    if (itemsPage?.items?.isEmpty() == true) {
                        item {
                            EmptyPlaceholder(
                                icon = R.drawable.search,
                                text = stringResource(R.string.no_results_found),
                            )
                        }
                    }
                }

                if (searchFilter == null && searchSummary == null || searchFilter != null && itemsPage == null) {
                    item {
                        ShimmerHost {
                            repeat(8) {
                                ListItemPlaceHolder()
                            }
                        }
                    }
                }
            }
        }
    }}

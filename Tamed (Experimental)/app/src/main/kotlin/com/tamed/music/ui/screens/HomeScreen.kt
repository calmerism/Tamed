package com.tamed.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.R
import com.tamed.music.db.entities.Album
import com.tamed.music.db.entities.Artist
import com.tamed.music.db.entities.Song
import com.tamed.music.extensions.toMediaItem
import com.tamed.music.extensions.togglePlayPause
import com.tamed.music.innertube.models.AlbumItem
import com.tamed.music.innertube.models.ArtistItem
import com.tamed.music.innertube.models.SongItem
import com.tamed.music.innertube.models.YTItem
import com.tamed.music.innertube.models.WatchEndpoint
import com.tamed.music.innertube.pages.HomePage
import com.tamed.music.models.toMediaMetadata
import com.tamed.music.playback.queues.ListQueue
import com.tamed.music.playback.queues.YouTubeQueue
import com.tamed.music.ui.component.ChipsRow
import com.tamed.music.ui.component.GlassIconCircleButton
import com.tamed.music.ui.component.RandomizeTile
import com.tamed.music.ui.component.LocalMenuState
import com.tamed.music.ui.component.MediaCard
import com.tamed.music.ui.component.SectionCarousel
import com.tamed.music.ui.component.SectionHeader
import com.tamed.music.ui.component.SongListItem
import com.tamed.music.ui.menu.AlbumMenu
import com.tamed.music.ui.menu.ArtistMenu
import com.tamed.music.ui.menu.SongMenu
import com.tamed.music.ui.menu.YouTubeAlbumMenu
import com.tamed.music.ui.menu.YouTubeArtistMenu
import com.tamed.music.ui.menu.YouTubeSongMenu
import com.tamed.music.ui.theme.TamedAppleTypography
import com.tamed.music.viewmodels.HomeViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val playerInsets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val isRandomizing by viewModel.isRandomizing.collectAsState()
    val quickPicks by viewModel.quickPicks.collectAsState()
    val speedDialSongs by viewModel.speedDialSongs.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()
    val selectedChip by viewModel.selectedChip.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    // Removed unresolved variables
    val pullRefreshState = rememberPullToRefreshState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    val lazyListState = rememberLazyListState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    if (selectedChip != null) {
        BackHandler { viewModel.toggleChip(selectedChip) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh,
            ),
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = playerInsets.calculateTopPadding() + 18.dp,
                bottom = playerInsets.calculateBottomPadding() + 48.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            item(key = "home_header") {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.home),
                                style = TamedAppleTypography.largeTitle(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Top picks, recent favorites, and recommendations shaped by what you play.",
                                style = TamedAppleTypography.cardSubtitle(),
                            )
                        }
                        Row(
                            modifier = Modifier.padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {

                            GlassIconCircleButton(
                                iconRes = R.drawable.history,
                                contentDescription = "History",
                                onClick = { navController.navigate("history") },
                                solid = true,
                            )
                            GlassIconCircleButton(
                                iconRes = R.drawable.settings,
                                contentDescription = "Settings",
                                onClick = { navController.navigate("settings") },
                                solid = true,
                            )
                        }
                    }
                }
            }

            // ─── Chips ───────────────────────────────────────────────────
            if (!homePage?.chips.isNullOrEmpty()) {
                item(key = "home_chips") {
                    ChipsRow(
                        chips = homePage?.chips.orEmpty().map { it to it.title },
                        currentValue = selectedChip,
                        onValueUpdate = { viewModel.toggleChip(it) },
                    )
                }
            }

            // ─── Quick Picks ─────────────────────────────────────────────
            val qp = quickPicks
            if (!qp.isNullOrEmpty()) {
                item(key = "qp_header") {
                    SectionHeader(
                        title = stringResource(R.string.quick_picks),
                        subtitle = "Songs selected based on what you're listening.",
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                item(key = "qp_list") {
                    SectionCarousel(
                        items = qp.take(8),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                    ) { song ->
                        MediaCard(
                            title = song.title,
                            subtitle = song.artists.joinToString { it.name },
                            imageUrl = song.thumbnailUrl?.replace(Regex("=w\\d+-h\\d+"), "=w540-h540"),
                            metadata = null,
                            tall = true,
                            cardSize = 320.dp,
                            onClick = {
                                if (song.id == mediaMetadata?.id) {
                                    playerConnection.player.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = "Quick Picks",
                                            items = qp.map { it.toMediaItem() },
                                            startIndex = qp.indexOf(song),
                                        )
                                    )
                                }
                            },
                        )
                    }
                }
            }

            // ─── Speed Dial ─────────────────────────────────────────────
            val speedDial = speedDialSongs
            if (speedDial.isNotEmpty()) {
                item(key = "speed_dial_header") {
                    SectionHeader(
                        title = "Speed dial",
                        subtitle = "A fast mix of your pinned tracks and fresh favorites.",
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp),
                    )
                }
                item(key = "speed_dial_content") {
                    SpeedDialSection(
                        speedDialSongs = speedDial,
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        navController = navController,
                        playerConnection = playerConnection,
                        menuState = menuState,
                        haptic = haptic,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            // ─── Keep Listening ─────────────────────────────────────────────
            val keepList = keepListening
            if (!keepList.isNullOrEmpty()) {
                item(key = "keep_header") {
                    SectionHeader(
                        title = "Keep listening",
                        subtitle = "Jump back into what you've been playing recently.",
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp),
                    )
                }
                item(key = "keep_content") {
                    SectionCarousel(
                        items = keepList,
                        contentPadding = PaddingValues(horizontal = 20.dp),
                    ) { localItem ->
                        MediaCard(
                            title = localItem.title,
                            subtitle = when (localItem) {
                                is Song -> localItem.artists.joinToString { it.name }
                                is Album -> localItem.artists.joinToString { it.name }
                                is Artist -> "Artist"
                                else -> ""
                            },
                            imageUrl = localItem.thumbnailUrl?.replace(Regex("=w\\d+-h\\d+"), "=w540-h540"),
                            metadata = null,
                            square = true,
                            cardSize = 220.dp,
                            onClick = {
                                when (localItem) {
                                    is Song -> playerConnection.playQueue(ListQueue("Keep Listening", keepList.filterIsInstance<Song>().map { it.toMediaItem() }, keepList.filterIsInstance<Song>().indexOf(localItem)))
                                    is Album -> navController.navigate("album/${localItem.id}")
                                    is Artist -> navController.navigate("artist/${localItem.id}")
                                    else -> {}
                                }
                            },
                        )
                    }
                }
            }

            // ─── Forgotten Favorites ─────────────────────────────────────────
            val forgotten = forgottenFavorites
            if (!forgotten.isNullOrEmpty()) {
                item(key = "forgotten_header") {
                    SectionHeader(
                        title = "Forgotten favorites",
                        subtitle = "Songs you used to play a lot, waiting to be rediscovered.",
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp),
                    )
                }
                item(key = "forgotten_content") {
                    SectionCarousel(
                        items = forgotten,
                        contentPadding = PaddingValues(horizontal = 20.dp),
                    ) { song ->
                        MediaCard(
                            title = song.title,
                            subtitle = song.artists.joinToString { it.name },
                            imageUrl = song.thumbnailUrl?.replace(Regex("=w\\d+-h\\d+"), "=w540-h540"),
                            metadata = null,
                            square = true,
                            cardSize = 220.dp,
                            onClick = {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = "Forgotten Favorites",
                                        items = forgotten.map { it.toMediaItem() },
                                        startIndex = forgotten.indexOf(song),
                                    )
                                )
                            },
                        )
                    }
                }
            }

            // ─── Similar Recommendations ─────────────────────────────────────
            val recommendations = similarRecommendations
            if (!recommendations.isNullOrEmpty()) {
                recommendations.forEachIndexed { idx, rec ->
                    item(key = "rec_header_$idx") {
                        val title = when (val seed = rec.title) {
                            is Song -> "Because you listened to ${seed.title}"
                            is Album -> "Because you listened to ${seed.title}"
                            is Artist -> "More like ${seed.title}"
                            else -> "Recommended for you"
                        }
                        SectionHeader(
                            title = title,
                            subtitle = similarRecommendationSubtitle(rec.title),
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp),
                        )
                    }
                    item(key = "rec_content_$idx") {
                        SectionCarousel(
                            items = rec.items,
                            contentPadding = PaddingValues(horizontal = 20.dp),
                        ) { ytItem ->
                            val highResThumbnail = remember(ytItem.thumbnail) {
                                ytItem.thumbnail?.replace(Regex("=w\\d+-h\\d+.*$"), "=w544-h544-l90-rj")
                                    ?.replace("sqp", "maxresdefault")
                            }
                            
                            MediaCard(
                                title = ytItem.title,
                                subtitle = when (ytItem) {
                                    is SongItem -> ytItem.artists.joinToString { it.name }
                                    is AlbumItem -> ytItem.artists?.joinToString { it.name }.orEmpty()
                                    is ArtistItem -> "Artist"
                                    is com.tamed.music.innertube.models.PlaylistItem -> ytItem.author?.name ?: "Playlist"
                                    else -> ""
                                },
                                imageUrl = highResThumbnail,
                                metadata = when (ytItem) {
                                    is AlbumItem -> ytItem.year?.toString()
                                    is com.tamed.music.innertube.models.PlaylistItem -> ytItem.songCountText
                                    else -> null
                                },
                                square = true,
                                cardSize = 220.dp,
                                onClick = {
                                    when (ytItem) {
                                        is SongItem -> {
                                            val endpoint = ytItem.endpoint ?: WatchEndpoint(videoId = ytItem.id)
                                            playerConnection.playQueue(YouTubeQueue(endpoint, ytItem.toMediaMetadata()))
                                        }
                                        is AlbumItem -> navController.navigate("album/${ytItem.id}")
                                        is ArtistItem -> navController.navigate("artist/${ytItem.id}")
                                        is com.tamed.music.innertube.models.PlaylistItem -> navController.navigate("online_playlist/${ytItem.id}")
                                        else -> {}
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // ─── Dynamic YouTube Music Sections ─────────────────────────────────────
            homePage?.sections.orEmpty()
                .filter { it.items.isNotEmpty() }
                .forEachIndexed { secIdx, section ->
                    item(key = "hp_header_$secIdx") {
                        SectionHeader(
                            title = section.title,
                            subtitle = homeSectionSubtitle(section),
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                    item(key = "hp_content_$secIdx") {
                        SectionCarousel(
                            items = section.items,
                            contentPadding = PaddingValues(horizontal = 20.dp),
                        ) { ytItem ->
                            val highResThumbnail = remember(ytItem.thumbnail) {
                                ytItem.thumbnail?.replace(Regex("=w\\d+-h\\d+.*$"), "=w544-h544-l90-rj")
                                    ?.replace("sqp", "maxresdefault")
                            }
                            
                            MediaCard(
                                title = ytItem.title,
                                subtitle = when (ytItem) {
                                    is SongItem -> ytItem.artists.joinToString { it.name }
                                    is AlbumItem -> ytItem.artists?.joinToString { it.name }.orEmpty()
                                    is ArtistItem -> "Artist"
                                    is com.tamed.music.innertube.models.PlaylistItem -> ytItem.author?.name ?: "Playlist"
                                    else -> ""
                                },
                                imageUrl = highResThumbnail,
                                metadata = when (ytItem) {
                                    is AlbumItem -> ytItem.year?.toString()
                                    is com.tamed.music.innertube.models.PlaylistItem -> ytItem.songCountText
                                    else -> null
                                },
                                square = true,
                                cardSize = 220.dp,
                                onClick = {
                                    when (ytItem) {
                                        is SongItem -> {
                                            val endpoint = ytItem.endpoint ?: WatchEndpoint(videoId = ytItem.id)
                                            playerConnection.playQueue(YouTubeQueue(endpoint, ytItem.toMediaMetadata()))
                                        }
                                        is AlbumItem -> navController.navigate("album/${ytItem.id}")
                                        is ArtistItem -> navController.navigate("artist/${ytItem.id}")
                                        is com.tamed.music.innertube.models.PlaylistItem -> navController.navigate("online_playlist/${ytItem.id}")
                                        else -> {}
                                    }
                                },
                            )
                        }
                    }
                }
        }

        PullToRefreshDefaults.Indicator(
            isRefreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp),
        )
    }
}

private fun similarRecommendationSubtitle(seed: Any): String =
    when (seed) {
        is Song -> "Recommendations that carry a similar mood and sound."
        is Album -> "Albums and artists that fit naturally with this record."
        is Artist -> "More music that lines up with this artist's style."
        else -> "More picks that connect with what you've been into lately."
    }

private fun homeSectionSubtitle(section: HomePage.Section): String {
    val normalizedLabel = section.label.orEmpty().toSentenceCaseOrNull()
    if (normalizedLabel != null && !normalizedLabel.equals(section.title, ignoreCase = true)) {
        return normalizedLabel
    }

    return when {
        section.title.contains("new release", ignoreCase = true) ->
            "Fresh music that just landed."
        section.title.contains("trending", ignoreCase = true) ->
            "What listeners are gravitating toward right now."
        section.title.contains("mix", ignoreCase = true) ->
            "A ready-made run of songs built for easy listening."
        section.title.contains("playlist", ignoreCase = true) ->
            "Curated sets worth dropping into."
        section.title.contains("album", ignoreCase = true) ->
            "Albums that are standing out right now."
        else ->
            "A curated set of picks pulled from your home feed."
    }
}

private fun String.toSentenceCaseOrNull(): String? {
    val cleaned = trim().replace(Regex("\\s+"), " ")
    if (cleaned.isBlank()) return null
    val lowercase = cleaned.lowercase(Locale.getDefault())
    return lowercase.replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
    }
}

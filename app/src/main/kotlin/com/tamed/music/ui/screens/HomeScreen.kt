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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.tamed.music.constants.PureBlackKey
import com.tamed.music.utils.rememberPreference
import coil3.compose.AsyncImage
import com.tamed.music.ui.theme.appleSecondaryTextColor
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
import androidx.compose.ui.platform.LocalContext
import com.tamed.music.utils.navigateToArtist
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
import com.tamed.music.ui.theme.AmbientBackdrop
import com.tamed.music.ui.utils.resize
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
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val isRandomizing by viewModel.isRandomizing.collectAsState()
    val quickPicks by viewModel.quickPicks.collectAsState()
    val speedDialSongs by viewModel.speedDialSongs.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
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

    AmbientBackdrop(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh,
            ),
        useNowPlayingFallback = true,
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = playerInsets.calculateTopPadding() + 6.dp,
                bottom = playerInsets.calculateBottomPadding() + 110.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            val qp = quickPicks
            item(key = "home_header") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.home),
                            style = TamedAppleTypography.largeTitle(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        GlassIconCircleButton(
                            iconRes = R.drawable.history,
                            contentDescription = "History",
                            onClick = { navController.navigate("history") },
                            buttonSize = 36.dp,
                            solid = true,
                        )
                    }

                    // ─── Top Picks for You ─────────────────────────────────────────────
                    if (!qp.isNullOrEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            SectionHeader(
                                title = "Top Picks for You",
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                            SectionCarousel(
                                items = qp.take(10),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalSpacing = 16,
                                key = { it.id },
                            ) { song ->
                                val itemIndex = qp.indexOf(song)
                                val tag = when (itemIndex % 3) {
                                    0 -> "Made for You"
                                    1 -> "Mood for You"
                                    else -> "Listen Again"
                                }
                                val cleanImageUrl = if (song.thumbnailUrl?.contains("i.ytimg.com") == true && song.album?.thumbnailUrl != null) {
                                    song.album?.thumbnailUrl?.replace(Regex("=w\\d+-h\\d+"), "=w360-h360-l90-rj")
                                } else {
                                    song.thumbnailUrl?.replace(Regex("=w\\d+-h\\d+"), "=w360-h360-l90-rj")
                                }
                                MediaCard(
                                    title = song.title,
                                    subtitle = song.artists.joinToString { it.name },
                                    imageUrl = cleanImageUrl,
                                    metadata = tag,
                                    tall = true,
                                    square = false,
                                    cardSize = 232.dp,
                                    textBelow = false,
                                    onClick = {
                                        if (song.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue.radio(song.toMediaMetadata())
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // ─── Recently Played > ─────────────────────────────────────────────
            val recentAlbums: List<Album> = (recentlyPlayed?.filterIsInstance<Album>() ?: emptyList())
                .ifEmpty { (keepListening?.filterIsInstance<Album>() ?: emptyList()) }
            if (recentAlbums.isNotEmpty()) {
                item(key = "recently_played_section") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SectionHeader(
                            title = "Recently Played",
                            onClick = { navController.navigate("history") },
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                        SectionCarousel(
                            items = recentAlbums,
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalSpacing = 16,
                            key = { it.id },
                        ) { album: Album ->
                            MediaCard(
                                title = album.title,
                                subtitle = album.artists.joinToString { it.name },
                                imageUrl = album.thumbnailUrl?.replace(Regex("=w\\d+-h\\d+"), "=w360-h360-l90-rj"),
                                metadata = null,
                                square = true,
                                cardSize = 148.dp,
                                textBelow = true,
                                isExplicit = album.album.explicit,
                                onClick = {
                                    navController.navigate("album/${album.id}")
                                },
                            )
                        }
                    }
                }
            }

            // ─── Forgotten Favorites ─────────────────────────────────────────
            val forgotten = forgottenFavorites
            if (!forgotten.isNullOrEmpty()) {
                item(key = "forgotten_section") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SectionHeader(
                            title = "Forgotten Favorites",
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                        SectionCarousel(
                            items = forgotten,
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalSpacing = 16,
                            key = { it.id },
                        ) { song ->
                            MediaCard(
                                title = song.title,
                                subtitle = song.artists.joinToString { it.name },
                                imageUrl = song.thumbnailUrl?.replace(Regex("=w\\d+-h\\d+"), "=w360-h360-l90-rj"),
                                metadata = null,
                                square = true,
                                cardSize = 148.dp,
                                isExplicit = song.song.explicit,
                                onClick = {
                                    playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                                },
                            )
                        }
                    }
                }
            }

            // ─── Similar Recommendations ─────────────────────────────────────
            val recommendations = similarRecommendations
            if (!recommendations.isNullOrEmpty()) {
                recommendations.forEachIndexed { idx, rec ->
                    item(key = "rec_section_$idx") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            val seed = rec.title
                            val rawThumb = when (seed) {
                                is Artist -> seed.thumbnailUrl ?: rec.items.firstOrNull { it is ArtistItem }?.thumbnail
                                is Album -> seed.thumbnailUrl ?: rec.items.firstOrNull { it is AlbumItem }?.thumbnail
                                is Song -> seed.thumbnailUrl ?: seed.album?.thumbnailUrl
                                else -> seed.thumbnailUrl ?: rec.items.firstOrNull()?.thumbnail
                            }
                            val seedThumbnail = remember(rawThumb) {
                                rawThumb?.replace(Regex("=w\\d+-h\\d+"), "=w240-h240-l90-rj")
                            }
                            val onHeaderClick: () -> Unit = {
                                when (seed) {
                                    is Album -> navController.navigate("album/${seed.id}")
                                    is Artist -> navController.navigateToArtist(context, seed.id, seed.title, seed.thumbnailUrl)
                                    is Song -> playerConnection.playQueue(YouTubeQueue.radio(seed.toMediaMetadata()))
                                    else -> {}
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onHeaderClick)
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (!seedThumbnail.isNullOrBlank()) {
                                    AsyncImage(
                                        model = seedThumbnail,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(if (seed is Artist) CircleShape else RoundedCornerShape(8.dp)),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(1.dp),
                                ) {
                                    Text(
                                        text = "Because You Like",
                                        style = TamedAppleTypography.headerSubtitle(),
                                        color = appleSecondaryTextColor().copy(alpha = 0.7f),
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(
                                            text = seed.title.toTitleCaseWords(),
                                            style = TamedAppleTypography.sectionTitle(),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Icon(
                                            painter = painterResource(R.drawable.navigate_next),
                                            contentDescription = null,
                                            tint = appleSecondaryTextColor().copy(alpha = 0.7f),
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }
                            }
                            SectionCarousel(
                                items = rec.items,
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalSpacing = 16,
                                key = { it.id },
                            ) { ytItem ->
                                val thumbnail = remember(ytItem.thumbnail) {
                                    ytItem.thumbnail?.resize(360, 360) ?: ytItem.thumbnail
                                }
                                val isYtExplicit = when (ytItem) {
                                    is SongItem -> ytItem.explicit
                                    is AlbumItem -> ytItem.explicit
                                    else -> false
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
                                    imageUrl = thumbnail,
                                    metadata = when (ytItem) {
                                        is AlbumItem -> ytItem.year?.toString()
                                        is com.tamed.music.innertube.models.PlaylistItem -> ytItem.songCountText
                                        else -> null
                                    },
                                    square = true,
                                    cardSize = 148.dp,
                                    isExplicit = isYtExplicit,
                                    onClick = {
                                        when (ytItem) {
                                            is SongItem -> {
                                                val endpoint = ytItem.endpoint ?: WatchEndpoint(videoId = ytItem.id)
                                                playerConnection.playQueue(YouTubeQueue(endpoint, ytItem.toMediaMetadata()))
                                            }
                                            is AlbumItem -> {
                                                com.tamed.music.ui.theme.BackdropCache.put(ytItem.id, ytItem.thumbnail)
                                                navController.navigate("album/${ytItem.id}")
                                            }
                                            is ArtistItem -> navController.navigateToArtist(context, ytItem.id, ytItem.title, ytItem.thumbnail)
                                            is com.tamed.music.innertube.models.PlaylistItem -> {
                                                com.tamed.music.ui.theme.BackdropCache.put(ytItem.id, ytItem.thumbnail)
                                                navController.navigate("online_playlist/${ytItem.id}")
                                            }
                                            else -> {}
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // ─── Dynamic YouTube Music Sections ─────────────────────────────────────
            homePage?.sections.orEmpty()
                .filter { it.items.isNotEmpty() }
                .forEachIndexed { secIdx, section ->
                    item(key = "hp_section_$secIdx") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            SectionHeader(
                                title = section.title.toTitleCaseWords(),
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                            SectionCarousel(
                                items = section.items,
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalSpacing = 16,
                                key = { it.id },
                            ) { ytItem ->
                                val thumbnail = remember(ytItem.thumbnail) {
                                    ytItem.thumbnail?.resize(360, 360) ?: ytItem.thumbnail
                                }
                                val isYtExplicit = when (ytItem) {
                                    is SongItem -> ytItem.explicit
                                    is AlbumItem -> ytItem.explicit
                                    else -> false
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
                                    imageUrl = thumbnail,
                                    metadata = when (ytItem) {
                                        is AlbumItem -> ytItem.year?.toString()
                                        is com.tamed.music.innertube.models.PlaylistItem -> ytItem.songCountText
                                        else -> null
                                    },
                                    square = true,
                                    cardSize = 148.dp,
                                    isExplicit = isYtExplicit,
                                    onClick = {
                                        when (ytItem) {
                                            is SongItem -> {
                                                val endpoint = ytItem.endpoint ?: WatchEndpoint(videoId = ytItem.id)
                                                playerConnection.playQueue(YouTubeQueue(endpoint, ytItem.toMediaMetadata()))
                                            }
                                            is AlbumItem -> {
                                                com.tamed.music.ui.theme.BackdropCache.put(ytItem.id, ytItem.thumbnail)
                                                navController.navigate("album/${ytItem.id}")
                                            }
                                            is ArtistItem -> navController.navigateToArtist(context, ytItem.id, ytItem.title, ytItem.thumbnail)
                                            is com.tamed.music.innertube.models.PlaylistItem -> {
                                                com.tamed.music.ui.theme.BackdropCache.put(ytItem.id, ytItem.thumbnail)
                                                navController.navigate("online_playlist/${ytItem.id}")
                                            }
                                            else -> {}
                                        }
                                    },
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
                .padding(top = playerInsets.calculateTopPadding() + 8.dp),
        )
    }
}

private fun String.toTitleCaseWords(): String {
    return split(" ").joinToString(" ") { word ->
        if (word.isEmpty()) word
        else word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}


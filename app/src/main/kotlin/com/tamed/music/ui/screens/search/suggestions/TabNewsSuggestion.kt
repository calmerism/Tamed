/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.tamed.music.ui.screens.search.suggestions

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.tamed.music.R
import com.tamed.music.utils.rememberPreference
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import kotlin.math.abs
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SuggestionsTabContent(
    navController: NavController,
    viewModel: SuggestionsViewModel = hiltViewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val suggestionTracks by viewModel.suggestionTracks.collectAsState()
    val suggestionArtists by viewModel.suggestionArtists.collectAsState()
    val suggestionAlbums by viewModel.suggestionAlbums.collectAsState()
    val suggestionVideos by viewModel.suggestionVideos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isManualLoading by viewModel.isManualLoading.collectAsState()
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val playerConnection = com.tamed.music.LocalPlayerConnection.current
    val context = LocalContext.current
    val (regionCode, _) = rememberPreference(
        key = androidx.datastore.preferences.core.stringPreferencesKey("suggestion_region"),
        defaultValue = "system"
    )

    androidx.compose.runtime.LaunchedEffect(regionCode) {
        viewModel.refresh(regionCode)
    }

    val pullToRefreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = isManualLoading,
        onRefresh = {
            viewModel.refresh(regionCode, force = true)
        },
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullToRefreshState,
                isRefreshing = isManualLoading,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding
        ) {
        if (isLoading && !isManualLoading && suggestionTracks == null && suggestionArtists == null && suggestionAlbums == null && suggestionVideos == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator()
                }
            }
        }

        suggestionTracks?.let { tracks ->
                item {
                    TrendingAppleMusicSection(
                        tracks = suggestionTracks!!,
                        countryCode = regionCode,
                        onTrackClick = { track ->
                            android.widget.Toast.makeText(context, "Loading ${track.title}...", android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.playTrack(track, playerConnection)
                        },
                        onMoreClick = {
                            val code = if (regionCode == "system") java.util.Locale.getDefault().country.lowercase() else regionCode.lowercase()
                            uriHandler.openUri("https://music.apple.com/$code/charts")
                        }
                    )
                }
            }

            suggestionArtists?.let { artists ->
                item {
                    TopArtistsSection(
                        artists = artists,
                        onArtistClick = { artist ->
                            android.widget.Toast.makeText(context, "Loading ${artist.name}...", android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.navigateToArtist(artist, navController)
                        }
                    )
                }
            }

            suggestionAlbums?.let { albums ->
                item {
                    TrendingAlbumsSection(
                        albums = albums,
                        onAlbumClick = { album ->
                            android.widget.Toast.makeText(context, "Loading ${album.title}...", android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.navigateToAlbum(album, navController)
                        },
                        onMoreClick = {
                            val code = if (regionCode == "system") java.util.Locale.getDefault().country.lowercase() else regionCode.lowercase()
                            uriHandler.openUri("https://music.apple.com/$code/charts/albums")
                        }
                    )
                }
            }

            suggestionVideos?.let { videos ->
                item {
                    TrendingVideosSection(
                        videos = videos,
                        onVideoClick = { video ->
                            android.widget.Toast.makeText(context, "Loading video ${video.title}...", android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.playVideo(video, playerConnection)
                        },
                        onMoreClick = {
                            val code = if (regionCode == "system") java.util.Locale.getDefault().country.lowercase() else regionCode.lowercase()
                            uriHandler.openUri("https://music.apple.com/$code/charts/videos")
                        }
                    )
                }
            }

            if (suggestionTracks == null && suggestionArtists == null && suggestionAlbums == null && suggestionVideos == null && !isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No suggestions available at the moment.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(onClick = { viewModel.refresh(regionCode, force = true) }) {
                                Text("Refresh")
                            }
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TrendingAppleMusicSection(
    tracks: List<SuggestionTrack>,
    countryCode: String,
    onTrackClick: (SuggestionTrack) -> Unit,
    onMoreClick: () -> Unit
) {
    if (tracks.isEmpty()) return
    val displayTracks = tracks.take(29)
    val totalItems = displayTracks.size + 1
    val pagerState = rememberPagerState(pageCount = { (totalItems + 4) / 5 })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Apple Music Top 100",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 32.dp)
        )
        Text(
            text = if (countryCode == "system") "Global Charts" else countryCode.uppercase(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        HorizontalPager(
            state = pagerState,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth().animateContentSize(tween(300, easing = FastOutSlowInEasing))
        ) { page ->
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                val startIdx = page * 5
                val endIdx = minOf(startIdx + 5, totalItems)
                for (i in startIdx until endIdx) {
                    val isMoreCard = i == 29
                    if (isMoreCard) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onMoreClick() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(painterResource(R.drawable.search), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("View more on Apple Music", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    } else if (i < displayTracks.size) {
                        val track = displayTracks[i]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTrackClick(track) }
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (track.thumbnailUrl != null) {
                                SubcomposeAsyncImage(
                                    model = track.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    loading = {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = track.title, 
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = track.artist, 
                                    style = MaterialTheme.typography.bodyMedium, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                    maxLines = 1, 
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            Spacer(Modifier.width(12.dp))
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "#${track.rank}", 
                                    style = MaterialTheme.typography.labelLarge, 
                                    color = MaterialTheme.colorScheme.primary, 
                                    fontWeight = FontWeight.Bold
                                )
                                // Mock play count
                                val playCount = remember(track.rank) { 
                                    val base = 2_500_000 / (track.rank + 2)
                                    if (base >= 1_000_000) String.format("%.1fM", base / 1_000_000f)
                                    else String.format("%dk", base / 1_000)
                                }
                                Text(
                                    text = playCount, 
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }, enabled = pagerState.currentPage > 0) {
                    Icon(painterResource(R.drawable.arrow_back), "Previous")
                }
                Text(text = "${pagerState.currentPage + 1} / ${pagerState.pageCount}", style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }, enabled = pagerState.currentPage < pagerState.pageCount - 1) {
                    Icon(painterResource(R.drawable.arrow_forward), "Next")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TopArtistsSection(
    artists: List<SuggestionArtist>,
    onArtistClick: (SuggestionArtist) -> Unit
) {
    if (artists.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Trending Artists",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            items(artists) { artist ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp).clickable { onArtistClick(artist) }) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        SubcomposeAsyncImage(
                            model = artist.thumbnailUrl,
                            contentDescription = artist.name,
                            contentScale = ContentScale.Crop,
                            loading = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            },
                            modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Surface(modifier = Modifier.size(28.dp).offset((-4).dp, (-4).dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary, tonalElevation = 4.dp) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(artist.rank.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(artist.name, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                    val playCount = remember(artist.rank) { 
                        val base = 15_000_000 / (artist.rank + 8)
                        if (base >= 1_000_000) String.format("%.1fM plays", base / 1_000_000f)
                        else String.format("%dk plays", base / 1_000)
                    }
                    Text(playCount, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun TrendingAlbumsSection(
    albums: List<SuggestionAlbum>,
    onAlbumClick: (SuggestionAlbum) -> Unit,
    onMoreClick: () -> Unit
) {
    if (albums.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Trending Albums",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)
        ) {
            items(albums) { album ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(120.dp).clickable { onAlbumClick(album) }) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        SubcomposeAsyncImage(
                            model = album.thumbnailUrl,
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            loading = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            },
                            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Surface(modifier = Modifier.size(28.dp).offset((-4).dp, (-4).dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary, tonalElevation = 4.dp) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(album.rank.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(album.title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                    Text(album.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                }
            }

            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(100.dp)
                        .padding(bottom = 20.dp)
                        .clickable { onMoreClick() }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "More",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendingVideosSection(
    videos: List<SuggestionTrack>,
    onVideoClick: (SuggestionTrack) -> Unit,
    onMoreClick: () -> Unit
) {
    if (videos.isEmpty()) return
    
    val carouselState = rememberCarouselState(itemCount = { videos.size })
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trending Music Videos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "More",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onMoreClick() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = 320.dp,
            itemSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { i ->
            val video = videos[i]
            var isCardFocused by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .onGloballyPositioned { coordinates ->
                        val cardCenter = coordinates.boundsInRoot().center.x
                        val screenWidth = context.resources.displayMetrics.widthPixels
                        val screenCenter = screenWidth / 2f
                        isCardFocused = abs(cardCenter - screenCenter) < 150
                    }
                    .clickable { onVideoClick(video) }
            ) {
                // Background Image (Thumbnail)
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )


                // Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                ),
                                startY = 300f
                            )
                        )
                )

                // Content
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = video.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

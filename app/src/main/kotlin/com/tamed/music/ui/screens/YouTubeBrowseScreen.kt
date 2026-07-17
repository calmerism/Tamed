/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.R
import com.tamed.music.extensions.togglePlayPause
import com.tamed.music.innertube.models.AlbumItem
import com.tamed.music.innertube.models.ArtistItem
import com.tamed.music.innertube.models.PlaylistItem
import com.tamed.music.innertube.models.SongItem
import com.tamed.music.innertube.models.YTItem
import com.tamed.music.models.toMediaMetadata
import com.tamed.music.playback.queues.YouTubeQueue
import com.tamed.music.ui.component.GlassIconCircleButton
import com.tamed.music.ui.component.MediaCard
import com.tamed.music.ui.component.SectionCarousel
import com.tamed.music.ui.component.SectionHeader
import com.tamed.music.ui.component.shimmer.GridItemPlaceHolder
import com.tamed.music.ui.component.shimmer.ShimmerHost
import com.tamed.music.ui.component.shimmer.TextPlaceholder
import com.tamed.music.ui.theme.TamedAppleTypography
import com.tamed.music.ui.theme.appleBackgroundColor
import com.tamed.music.viewmodels.YouTubeBrowseViewModel

private const val GenericMusicSourceLabel = "YouTube Music"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun YouTubeBrowseScreen(
    navController: NavController,
    viewModel: YouTubeBrowseViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val browseResult by viewModel.result.collectAsState()
    val density = LocalDensity.current
    val windowInsets = LocalPlayerAwareWindowInsets.current
    val topPadding = with(density) { windowInsets.getTop(this).toDp() }
    val bottomPadding = with(density) { windowInsets.getBottom(this).toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appleBackgroundColor()),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 0.dp,
                top = topPadding + 14.dp,
                end = 0.dp,
                bottom = bottomPadding + 140.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            item(key = "header") {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GlassIconCircleButton(
                            iconRes = R.drawable.arrow_back,
                            contentDescription = "Back",
                            onClick = navController::navigateUp,
                        )
                    }
                    Text(
                        text = browseResult?.title.orEmpty(),
                        style = TamedAppleTypography.largeTitle(),
                    )
                }
            }

            if (browseResult == null) {
                item(key = "loading") {
                    ShimmerHost {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            modifier = Modifier.padding(horizontal = 20.dp),
                        ) {
                            TextPlaceholder(
                                height = 28.dp,
                                modifier = Modifier.fillMaxWidth(0.55f),
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(3) {
                                    GridItemPlaceHolder()
                                }
                            }
                            TextPlaceholder(
                                height = 24.dp,
                                modifier = Modifier.fillMaxWidth(0.48f),
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(3) {
                                    GridItemPlaceHolder()
                                }
                            }
                        }
                    }
                }
            }

            browseResult?.items?.forEachIndexed { index, section ->
                if (section.items.isNotEmpty()) {
                    item(key = "section_$index") {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            section.title?.let { title ->
                                SectionHeader(
                                    title = title,
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                )
                            }
                            SectionCarousel(
                                items = section.items.take(12),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalSpacing = 14,
                            ) { item ->
                                BrowseShelfCard(
                                    item = item,
                                    isActive = item is SongItem && mediaMetadata?.id == item.id,
                                    onClick = {
                                        when (item) {
                                            is SongItem -> {
                                                if (item.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue.radio(item.toMediaMetadata())
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
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseShelfCard(
    item: YTItem,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val title = when (item) {
        is SongItem -> item.title
        is AlbumItem -> item.title
        is PlaylistItem -> item.title
        is ArtistItem -> item.title
    }
    val subtitle = when (item) {
        is SongItem -> item.artists.joinToString(", ") { it.name }
        is AlbumItem -> item.artists?.joinToString(", ") { it.name }.orEmpty()
        is PlaylistItem -> item.author?.name ?: item.songCountText.orEmpty()
        is ArtistItem -> "Artist"
    }
    val metadata = when (item) {
        is SongItem -> item.album?.name
        is AlbumItem -> item.year?.toString()
        is PlaylistItem -> item.songCountText
        is ArtistItem -> null
    }
    val cleanedSubtitle = subtitle
        .takeUnless { it.isBlank() || it.equals(GenericMusicSourceLabel, ignoreCase = true) }
        ?: " "
    val cleanedMetadata = metadata
        ?.takeUnless { it.isBlank() || it.equals(GenericMusicSourceLabel, ignoreCase = true) }

    MediaCard(
        title = title,
        subtitle = cleanedSubtitle,
        imageUrl = item.thumbnail,
        metadata = cleanedMetadata,
        tall = false,
        onClick = onClick,
        modifier = if (isActive) {
            Modifier.background(color = androidx.compose.ui.graphics.Color.Transparent)
        } else {
            Modifier
        },
    )
}

/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.tamed.music.ui.screens.artist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import coil3.request.crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import com.tamed.music.ui.component.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults

import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.tamed.music.innertube.YouTube
import com.tamed.music.innertube.models.AlbumItem
import com.tamed.music.innertube.models.ArtistItem
import com.tamed.music.innertube.models.PlaylistItem
import com.tamed.music.innertube.models.SongItem
import com.tamed.music.innertube.models.WatchEndpoint
import com.tamed.music.LocalDatabase

import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.R
import com.tamed.music.constants.AppBarHeight
import com.tamed.music.constants.HideExplicitKey
import com.tamed.music.constants.ShowArtistSubscriberCountKey
import com.tamed.music.constants.ShowMonthlyListenersKey
import com.tamed.music.constants.ShowArtistDescriptionKey
import com.tamed.music.constants.ShowArtistBackgroundVideoKey
import com.tamed.music.ui.component.ExpandableText


import com.tamed.music.db.entities.ArtistEntity
import com.tamed.music.extensions.toMediaItem
import com.tamed.music.models.toMediaMetadata
import com.tamed.music.playback.queues.ListQueue
import com.tamed.music.playback.queues.YouTubeQueue
import com.tamed.music.ui.component.AlbumGridItem

import com.tamed.music.ui.component.GlassIconCircleButton
import com.tamed.music.ui.component.GlassActionPill
import com.tamed.music.ui.component.GlassPanel
import com.tamed.music.ui.component.LocalGlassScope
import com.tamed.music.ui.component.appleGlassEffect
import com.tamed.music.ui.component.ExplicitTag
import com.tamed.music.ui.theme.appleGlassColor
import com.tamed.music.ui.theme.appleGlassBorderColor
import com.tamed.music.viewmodels.ArtistBackdropCache
import com.tamed.music.ui.component.MediaCard
import com.tamed.music.ui.component.SectionCarousel
import com.tamed.music.ui.component.SectionHeader
import com.tamed.music.ui.component.HideOnScrollFAB
import com.tamed.music.ui.component.AppleHeroPlayButton
import com.tamed.music.ui.component.LocalMenuState
import com.tamed.music.ui.component.NavigationTitle
import com.tamed.music.ui.component.SongListItem
import com.tamed.music.ui.theme.TamedAppleColors
import com.tamed.music.ui.theme.TamedAppleTypography
import com.tamed.music.ui.theme.SfProFontFamily
import com.tamed.music.db.entities.AlbumEntity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import com.tamed.music.extensions.togglePlayPause
import com.tamed.music.ui.component.YouTubeGridItem
import com.tamed.music.ui.component.YouTubeListItem
import com.tamed.music.ui.component.shimmer.ButtonPlaceholder
import com.tamed.music.ui.component.shimmer.ListItemPlaceHolder
import com.tamed.music.ui.component.shimmer.ShimmerHost
import com.tamed.music.ui.component.shimmer.TextPlaceholder
import com.tamed.music.ui.menu.AlbumMenu
import com.tamed.music.ui.menu.SongMenu
import com.tamed.music.ui.menu.YouTubeAlbumMenu
import com.tamed.music.ui.menu.YouTubeArtistMenu
import com.tamed.music.ui.menu.YouTubePlaylistMenu
import com.tamed.music.ui.menu.YouTubeSongMenu
import com.tamed.music.ui.utils.backToMain
import com.tamed.music.ui.utils.fadingEdge
import com.tamed.music.ui.utils.isScrollingUp
import com.tamed.music.ui.theme.AmbientBackdrop
import com.tamed.music.constants.ArtistBackgroundStyleKey
import com.tamed.music.ui.utils.resize
import com.tamed.music.utils.rememberPreference
import com.tamed.music.utils.navigateToArtist
import com.tamed.music.viewmodels.ArtistViewModel
import com.valentinilk.shimmer.shimmer
import com.tamed.music.ui.component.ArtistVideo
import com.tamed.music.applecanvas.AppleMusicArtistBackgroundProvider
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.tamed.music.ui.theme.LocalBackdropColors
import com.tamed.music.ui.theme.extractGradientColors
import androidx.compose.runtime.CompositionLocalProvider

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArtistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return

    val isGuest = false
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val artistPage = viewModel.artistPage
    val libraryArtist by viewModel.libraryArtist.collectAsState()
    val librarySongs by viewModel.librarySongs.collectAsState()
    val libraryAlbums by viewModel.libraryAlbums.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val showArtistDescription by rememberPreference(key = ShowArtistDescriptionKey, defaultValue = true)
    val showArtistSubscriberCount by rememberPreference(key = ShowArtistSubscriberCountKey, defaultValue = true)
    val showArtistBackgroundVideo by rememberPreference(key = ShowArtistBackgroundVideoKey, defaultValue = true)
    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLocal by rememberSaveable { mutableStateOf(false) }
    var showArtistDescriptionSheet by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val headerOffsetProvider = remember(systemBarsTopPadding, density) {
        {
            val baseOffset = -(systemBarsTopPadding + AppBarHeight).value * density.density
            val scrollOffset = if (lazyListState.firstVisibleItemIndex == 0) {
                lazyListState.firstVisibleItemScrollOffset
            } else {
                350
            }
            (baseOffset + (scrollOffset * 0.45f)).toInt()
        }
    }

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
        }
    }

    LaunchedEffect(libraryArtist) {
        showLocal = libraryArtist?.artist?.isLocal == true
    }

    val featuredAlbum = remember(artistPage, libraryAlbums) {
        val albumSection = artistPage?.sections?.firstOrNull {
            it.title.equals("Latest release", ignoreCase = true)
        } ?: artistPage?.sections?.firstOrNull {
            it.title.equals("Albums", ignoreCase = true) ||
            it.title.equals("Singles and EPs", ignoreCase = true)
        }
        val firstItem = albumSection?.items?.firstOrNull()
        if (firstItem is AlbumItem) {
            FeaturedRelease(
                id = firstItem.id,
                title = firstItem.title,
                thumbnail = firstItem.thumbnail,
                year = firstItem.year?.toString(),
                isExplicit = firstItem.explicit,
            )
        } else if (firstItem is SongItem) {
            FeaturedRelease(
                id = firstItem.album?.id ?: firstItem.id,
                title = firstItem.title,
                thumbnail = firstItem.thumbnail,
                year = null,
                isExplicit = firstItem.explicit,
            )
        } else {
            libraryAlbums.firstOrNull()?.let {
                FeaturedRelease(
                    id = it.id,
                    title = it.title,
                    thumbnail = it.thumbnailUrl,
                    year = it.album.year?.toString(),
                    isExplicit = it.album.explicit,
                )
            }
        }
    }

    val albumYearMap = remember(artistPage, libraryAlbums) {
        val map = mutableMapOf<String, String>()
        libraryAlbums.forEach { album ->
            album.album.year?.let { y -> map[album.title.lowercase().trim()] = y.toString() }
        }
        artistPage?.sections?.forEach { section ->
            section.items.filterIsInstance<com.tamed.music.innertube.models.AlbumItem>().forEach { album ->
                album.year?.let { y -> map[album.title.lowercase().trim()] = y.toString() }
            }
        }
        map
    }

    val currentThumbnail = viewModel.initialThumbnail
        ?: artistPage?.artist?.thumbnail
        ?: libraryArtist?.artist?.thumbnailUrl
        ?: com.tamed.music.ui.theme.BackdropCache.get(viewModel.artistId)
    val stableBackdropThumbnail = remember(viewModel.artistId) {
        viewModel.initialThumbnail
            ?: com.tamed.music.utils.ArtistMemoryCache.getThumbnail(viewModel.artistId)
            ?: com.tamed.music.ui.theme.BackdropCache.get(viewModel.artistId)
    } ?: currentThumbnail
    LaunchedEffect(artistPage?.artist?.thumbnail) {
        artistPage?.artist?.thumbnail?.let {
            com.tamed.music.ui.theme.BackdropCache.put(viewModel.artistId, it)
            com.tamed.music.utils.ArtistMemoryCache.putThumbnail(viewModel.artistId, it)
        }
    }

    AmbientBackdrop(
        modifier = Modifier.fillMaxSize(),
        thumbnailUrl = stableBackdropThumbnail,
        forceStyle = com.tamed.music.constants.HomeBackgroundStyle.BACKDROP
    ) {
        val playerAwarePadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        val layoutDirection = LocalLayoutDirection.current
        val contentPadding = remember(playerAwarePadding, layoutDirection) {
            PaddingValues(
                start = playerAwarePadding.calculateStartPadding(layoutDirection),
                top = playerAwarePadding.calculateTopPadding(),
                end = playerAwarePadding.calculateEndPadding(layoutDirection),
                bottom = playerAwarePadding.calculateBottomPadding() + 110.dp
            )
        }

        LazyColumn(
            state = lazyListState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item(key = "header") {
                val thumbnail = currentThumbnail
                val artistName = viewModel.initialName
                    ?: artistPage?.artist?.title
                    ?: libraryArtist?.artist?.name

                    var backgroundVideoUrl by remember { mutableStateOf<String?>(null) }
                    LaunchedEffect(artistName, showArtistBackgroundVideo) {
                        if (artistName != null && showArtistBackgroundVideo) {
                            withContext(Dispatchers.IO) {
                                backgroundVideoUrl = AppleMusicArtistBackgroundProvider.getByArtistName(artistName)
                            }
                        }
                    }

                    Box {
                        if (thumbnail != null || backgroundVideoUrl != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.05f)
                                    .graphicsLayer {
                                        translationY = headerOffsetProvider().toFloat()
                                        compositingStrategy = CompositingStrategy.Offscreen
                                    }
                                    .drawWithCache {
                                        val maskBrush = Brush.verticalGradient(
                                            0.0f to Color.Black,
                                            0.55f to Color.Black,
                                            0.80f to Color.Black.copy(alpha = 0.5f),
                                            0.93f to Color.Black.copy(alpha = 0.15f),
                                            1.0f to Color.Transparent,
                                        )
                                        onDrawWithContent {
                                            drawContent()
                                            drawRect(
                                                brush = maskBrush,
                                                blendMode = BlendMode.DstIn,
                                            )
                                        }
                                    }
                            ) {
                                if (thumbnail != null) {
                                    AsyncImage(
                                        model = coil3.request.ImageRequest.Builder(LocalContext.current)
                                            .data(thumbnail.resize(800, 800))
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alignment = Alignment.TopCenter
                                    )
                                }
                                if (backgroundVideoUrl != null && showArtistBackgroundVideo) {
                                    ArtistVideo(
                                        videoUrl = backgroundVideoUrl!!,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        val headerTopPadding = LocalResources.current.displayMetrics.widthPixels.let { screenWidth ->
                            with(density) {
                                (screenWidth * 0.58f).toDp()
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = headerTopPadding)
                                .padding(bottom = 0.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ArtistHeaderTitle(
                                    artistName = artistName,
                                    artistId = viewModel.artistId,
                                )

                                Spacer(Modifier.height(18.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val description = artistPage?.description?.replace("From Wikipedia (", "")?.trim()
                                    GlassIconCircleButton(
                                        iconRes = R.drawable.info,
                                        contentDescription = "Artist Info",
                                        onClick = {
                                            if (!description.isNullOrEmpty()) {
                                                showArtistDescriptionSheet = true
                                            }
                                        },
                                        solid = false,
                                    )

                                    Spacer(Modifier.width(18.dp))

                                    val isArtistPlaying = isPlaying && run {
                                        val firstSection = artistPage?.sections?.firstOrNull()
                                        val topSongs = firstSection?.items?.filterIsInstance<SongItem>()
                                        !topSongs.isNullOrEmpty() || librarySongs.isNotEmpty()
                                    }
                                    AppleHeroPlayButton(
                                        isPlaying = isArtistPlaying,
                                        onClick = {
                                            val firstSection = artistPage?.sections?.firstOrNull()
                                            val topSongs = firstSection?.items?.filterIsInstance<SongItem>()
                                            if (isArtistPlaying) {
                                                playerConnection.player.togglePlayPause()
                                            } else if (!topSongs.isNullOrEmpty()) {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = artistName ?: "Artist",
                                                        items = topSongs.map { it.toMediaItem() },
                                                    )
                                                )
                                            } else if (artistPage?.artist?.radioEndpoint != null) {
                                                playerConnection.playQueue(YouTubeQueue(artistPage!!.artist.radioEndpoint!!))
                                            } else if (librarySongs.isNotEmpty()) {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = libraryArtist?.artist?.name ?: "Unknown Artist",
                                                        items = librarySongs.map { it.toMediaItem() },
                                                    )
                                                )
                                            }
                                        },
                                    )

                                    Spacer(Modifier.width(18.dp))

                                    val isSubscribed = libraryArtist?.artist?.bookmarkedAt != null
                                    GlassIconCircleButton(
                                        iconRes = if (isSubscribed) R.drawable.star else R.drawable.star,
                                        contentDescription = "Subscribe",
                                        onClick = {
                                            database.transaction {
                                                val artist = libraryArtist?.artist
                                                if (artist != null) {
                                                    update(artist.toggleLike())
                                                } else {
                                                    artistPage?.artist?.let {
                                                        database.insert(
                                                            ArtistEntity(
                                                                id = it.id,
                                                                name = it.title,
                                                                channelId = it.channelId,
                                                                thumbnailUrl = it.thumbnail,
                                                            ).toggleLike()
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        accent = if (isSubscribed) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                        solid = false,
                                    )
                                }
                            }
                        }
                    }
            }

            if (artistPage == null && !showLocal) {
                item(key = "shimmer_songs") {
                    ShimmerHost(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        repeat(6) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            } else {
                if (featuredAlbum != null) {
                    item(key = "featured_album_card") {
                        Spacer(Modifier.height(18.dp))
                        val releaseCardShape = RoundedCornerShape(24.dp)
                        val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                        val textPrimary = if (isDarkTheme) Color.White else Color(0xFF1C1C1E)
                        val textSecondary = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color(0xFF1C1C1E).copy(alpha = 0.65f)
                        val featuredAlbumEntity by database.album(featuredAlbum.id).collectAsState(initial = null)
                        val isBookmarked = featuredAlbumEntity?.album?.bookmarkedAt != null || featuredAlbumEntity?.album?.inLibrary != null

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .appleGlassEffect(
                                    shape = releaseCardShape,
                                    surfaceOpacity = if (isDarkTheme) 0.16f else 0.72f,
                                    highlightAlpha = if (isDarkTheme) 0.28f else 0.35f,
                                )
                                .clickable {
                                    navController.navigate("album/${featuredAlbum.id}")
                                }
                                .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 22.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(13.dp),
                            ) {
                                AsyncImage(
                                    model = featuredAlbum.thumbnail?.replace(Regex("=w\\d+-h\\d+"), "=w500-h500-l90-rj"),
                                    contentDescription = featuredAlbum.title,
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(13.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(1.dp),
                                ) {
                                    val releaseDateText = featuredAlbum.year ?: "Latest Release"
                                    Text(
                                        text = releaseDateText,
                                        fontFamily = SfProFontFamily,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = TextStyle(
                                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                                            lineHeight = 14.sp,
                                        ),
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    ) {
                                        Text(
                                            text = featuredAlbum.title,
                                            fontFamily = SfProFontFamily,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimary,
                                            letterSpacing = (-0.2).sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = TextStyle(
                                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                                lineHeight = 18.sp,
                                            ),
                                            modifier = Modifier.weight(1f, fill = false),
                                        )
                                        if (featuredAlbum.isExplicit) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(13.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(
                                                        if (isDarkTheme) Color.White.copy(alpha = 0.28f)
                                                        else Color.Black.copy(alpha = 0.12f)
                                                    )
                                            ) {
                                                Text(
                                                    text = "E",
                                                    color = textPrimary,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = SfProFontFamily,
                                                    style = TextStyle(
                                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                                        lineHeight = 8.sp,
                                                    ),
                                                    textAlign = TextAlign.Center,
                                                )
                                            }
                                        }
                                    }
                                    val songsCount = featuredAlbumEntity?.album?.songCount
                                    val subtitleText = when {
                                        songsCount != null && songsCount > 0 -> pluralStringResource(R.plurals.n_song, songsCount, songsCount)
                                        featuredAlbum.title.contains("Single", ignoreCase = true) -> "1 Song"
                                        else -> "Album"
                                    }
                                    Text(
                                        text = subtitleText,
                                        fontFamily = SfProFontFamily,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = TextStyle(
                                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                                            lineHeight = 14.sp,
                                        ),
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDarkTheme) Color.White.copy(alpha = 0.20f)
                                            else Color.Black.copy(alpha = 0.08f)
                                        )
                                        .clickable {
                                            database.query {
                                                val albumObj = featuredAlbumEntity
                                                if (albumObj != null) {
                                                    update(albumObj.album.toggleLike())
                                                } else {
                                                    insert(
                                                        AlbumEntity(
                                                            id = featuredAlbum.id,
                                                            title = featuredAlbum.title,
                                                            thumbnailUrl = featuredAlbum.thumbnail,
                                                            year = featuredAlbum.year?.toIntOrNull(),
                                                            songCount = 0,
                                                            duration = 0,
                                                            explicit = featuredAlbum.isExplicit,
                                                        ).toggleLike()
                                                    )
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(if (isBookmarked) R.drawable.check else R.drawable.add),
                                        contentDescription = if (isBookmarked) "In Library" else "Add to Library",
                                        tint = textPrimary,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                if (showLocal) {
                    if (librarySongs.isNotEmpty()) {
                        item(key = "local_songs_section") {
                            Spacer(Modifier.height(16.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                SectionHeader(
                                    title = stringResource(R.string.songs),
                                    onClick = {
                                        navController.navigate("artist/${viewModel.artistId}/songs")
                                    },
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                )

                                val filteredLibrarySongs = if (hideExplicit) {
                                    librarySongs.filter { !it.song.explicit }
                                } else {
                                    librarySongs
                                }
                                val primaryColor = MaterialTheme.colorScheme.primary
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    filteredLibrarySongs.forEachIndexed { index, song ->
                                        val isItemActive = song.id == mediaMetadata?.id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isItemActive) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                                .combinedClickable(
                                                    onClick = {
                                                        if (isItemActive) {
                                                            playerConnection.player.togglePlayPause()
                                                        } else {
                                                            playerConnection.playQueue(
                                                                ListQueue(
                                                                    title = libraryArtist?.artist?.name ?: "Unknown Artist",
                                                                    items = librarySongs.map { it.toMediaItem() },
                                                                    startIndex = index
                                                                )
                                                            )
                                                        }
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        menuState.show {
                                                            com.tamed.music.ui.menu.SongMenu(
                                                                originalSong = song,
                                                                navController = navController,
                                                                onDismiss = menuState::dismiss,
                                                            )
                                                        }
                                                    },
                                                )
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                            ) {
                                                AsyncImage(
                                                    model = song.song.thumbnailUrl?.replace(Regex("=w\\d+-h\\d+"), "=w240-h240-l90-rj"),
                                                    contentDescription = song.title,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop,
                                                )
                                                if (isItemActive && isPlaying) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color.Black.copy(alpha = 0.45f)),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.volume_up),
                                                            contentDescription = "Playing",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(20.dp),
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(Modifier.width(14.dp))

                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = song.title,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontSize = 15.sp,
                                                            fontWeight = if (isItemActive) FontWeight.SemiBold else FontWeight.Normal,
                                                            color = if (isItemActive) primaryColor else Color.White,
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false),
                                                    )
                                                    if (song.song.explicit) {
                                                        ExplicitTag(
                                                            color = TamedAppleColors.SecondaryText,
                                                            size = 13.dp,
                                                        )
                                                    }
                                                }

                                                val albumName = song.album?.title?.ifEmpty { null }
                                                val albumYear = song.song.year?.toString() ?: albumName?.let { albumYearMap[it.lowercase().trim()] }

                                                val subtitle = buildString {
                                                    if (albumName != null) {
                                                        append(albumName)
                                                        if (albumYear != null) {
                                                            append(" · ")
                                                            append(albumYear)
                                                        }
                                                    } else if (albumYear != null) {
                                                        append(albumYear)
                                                    } else {
                                                        append("Song")
                                                    }
                                                }
                                                Text(
                                                    text = subtitle,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 13.sp,
                                                        color = TamedAppleColors.SecondaryText,
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    menuState.show {
                                                        com.tamed.music.ui.menu.SongMenu(
                                                            originalSong = song,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp),
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.more_horiz),
                                                    contentDescription = "More",
                                                    tint = TamedAppleColors.SecondaryText,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (libraryAlbums.isNotEmpty()) {
                        item(key = "local_albums_section") {
                            Spacer(Modifier.height(28.dp))
                            val filteredLibraryAlbums = if (hideExplicit) {
                                libraryAlbums.filter { !it.album.explicit }
                            } else {
                                libraryAlbums
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                SectionHeader(
                                    title = stringResource(R.string.albums),
                                    onClick = {
                                        com.tamed.music.ui.theme.BackdropCache.put(viewModel.artistId, currentThumbnail)
                                        navController.navigate("artist/${viewModel.artistId}/albums")
                                    },
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                )
                                SectionCarousel(
                                    items = filteredLibraryAlbums,
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                ) { album ->
                                    MediaCard(
                                        title = album.title,
                                        subtitle = album.album.year?.toString().orEmpty(),
                                        imageUrl = album.thumbnailUrl?.replace(Regex("=w\\d+-h\\d+"), "=w360-h360-l90-rj"),
                                        square = true,
                                        cardSize = 160.dp,
                                        textBelow = true,
                                        isExplicit = album.album.explicit,
                                        onClick = {
                                            com.tamed.music.ui.theme.BackdropCache.put(album.id, album.thumbnailUrl)
                                            navController.navigate("album/${album.id}")
                                        },
                                    )
                                }
                            }
                        }
                    }
                } else {
                    artistPage?.sections?.fastForEach { section ->
                        if (section.title.equals("Live performances", ignoreCase = true)) return@fastForEach
                        if (section.items.isEmpty()) return@fastForEach

                        val isVideoSection = section.title.contains("video", ignoreCase = true)
                        val isSongSection = !isVideoSection && (section.title.contains("song", ignoreCase = true) || (section.items.all { it is SongItem } && (section.items.firstOrNull() as? SongItem)?.album != null))
                        if (isSongSection) {
                            item(key = "section_songs_${section.title}") {
                                Spacer(Modifier.height(16.dp))
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    SectionHeader(
                                        title = section.title,
                                        onClick = section.moreEndpoint?.let {
                                            {
                                                com.tamed.music.ui.theme.BackdropCache.put(viewModel.artistId, currentThumbnail)
                                                val encodedBrowseId = Uri.encode(it.browseId)
                                                val paramsQuery = it.params?.let { params ->
                                                    "&params=${Uri.encode(params)}"
                                                }.orEmpty()
                                                navController.navigate(
                                                    "artist/${viewModel.artistId}/items?browseId=$encodedBrowseId$paramsQuery",
                                                )
                                            }
                                        },
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                    )

                                    val songItems = section.items.filterIsInstance<SongItem>().distinctBy { it.id }
                                    val primaryColor = MaterialTheme.colorScheme.primary
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        songItems.forEachIndexed { _, song ->
                                            val isItemActive = mediaMetadata?.id == song.id
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isItemActive) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                                    .combinedClickable(
                                                        onClick = {
                                                            if (isItemActive) {
                                                                playerConnection.player.togglePlayPause()
                                                            } else {
                                                                playerConnection.playQueue(
                                                                    YouTubeQueue(
                                                                        WatchEndpoint(videoId = song.id),
                                                                        song.toMediaMetadata()
                                                                    ),
                                                                )
                                                            }
                                                        },
                                                        onLongClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            menuState.show {
                                                                YouTubeSongMenu(
                                                                    song = song,
                                                                    navController = navController,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }
                                                        },
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .clip(RoundedCornerShape(8.dp)),
                                                ) {
                                                    AsyncImage(
                                                        model = song.thumbnail?.replace(Regex("=w\\d+-h\\d+"), "=w240-h240-l90-rj"),
                                                        contentDescription = song.title,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop,
                                                    )
                                                    if (isItemActive && isPlaying) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(Color.Black.copy(alpha = 0.45f)),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(R.drawable.volume_up),
                                                                contentDescription = "Playing",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(20.dp),
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(Modifier.width(14.dp))
                                                val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                                                val textPrimary = if (isDarkTheme) Color.White else Color(0xFF1C1C1E)

                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = song.title,
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontSize = 15.sp,
                                                                fontWeight = if (isItemActive) FontWeight.Bold else FontWeight.Normal,
                                                                color = textPrimary,
                                                            ),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f, fill = false),
                                                        )
                                                        if (song.explicit) {
                                                            ExplicitTag(
                                                                color = TamedAppleColors.SecondaryText,
                                                                size = 13.dp,
                                                            )
                                                        }
                                                    }

                                                    val albumName = song.album?.name?.ifEmpty { null }
                                                    val albumYear = albumName?.let { albumYearMap[it.lowercase().trim()] }

                                                    val subtitle = buildString {
                                                        if (albumName != null) {
                                                            append(albumName)
                                                            if (albumYear != null) {
                                                                append(" · ")
                                                                append(albumYear)
                                                            }
                                                        } else if (albumYear != null) {
                                                            append(albumYear)
                                                        } else {
                                                            append("Song")
                                                        }
                                                    }
                                                    Text(
                                                        text = subtitle,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontSize = 13.sp,
                                                            color = TamedAppleColors.SecondaryText,
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        menuState.show {
                                                            YouTubeSongMenu(
                                                                song = song,
                                                                navController = navController,
                                                                onDismiss = menuState::dismiss,
                                                            )
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp),
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.more_horiz),
                                                        contentDescription = "More",
                                                        tint = TamedAppleColors.SecondaryText,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            item(key = "section_${section.title}") {
                                Spacer(Modifier.height(28.dp))
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    SectionHeader(
                                        title = section.title,
                                        onClick = section.moreEndpoint?.let {
                                            {
                                                com.tamed.music.ui.theme.BackdropCache.put(viewModel.artistId, currentThumbnail)
                                                val encodedBrowseId = Uri.encode(it.browseId)
                                                val paramsQuery = it.params?.let { params ->
                                                    "&params=${Uri.encode(params)}"
                                                }.orEmpty()
                                                navController.navigate(
                                                    "artist/${viewModel.artistId}/items?browseId=$encodedBrowseId$paramsQuery",
                                                )
                                            }
                                        },
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                    )

                                    SectionCarousel(
                                        items = section.items.distinctBy { it.id },
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                    ) { item ->
                                        val isItemExplicit = when (item) {
                                            is SongItem -> item.explicit
                                            is AlbumItem -> item.explicit
                                            else -> false
                                        }
                                        MediaCard(
                                            title = item.title,
                                            subtitle = when (item) {
                                                is AlbumItem -> item.year?.toString().orEmpty()
                                                is PlaylistItem -> item.author?.name.orEmpty()
                                                is SongItem -> if (isVideoSection) item.artists.filter { !it.name.matches(Regex("""^\d+:\d+.*""")) }.joinToString { it.name } else ""
                                                else -> ""
                                            },
                                            imageUrl = item.thumbnail?.replace(Regex("=w\\d+-h\\d+"), if (isVideoSection) "=w720-h405-l90-rj" else "=w360-h360-l90-rj"),
                                            square = !isVideoSection,
                                            video = isVideoSection,
                                            cardSize = if (isVideoSection) 220.dp else 160.dp,
                                            textBelow = true,
                                            isExplicit = isItemExplicit,
                                            onClick = {
                                                when (item) {
                                                    is SongItem -> playerConnection.playQueue(
                                                        YouTubeQueue(WatchEndpoint(videoId = item.id), item.toMediaMetadata())
                                                    )
                                                    is AlbumItem -> {
                                                        com.tamed.music.ui.theme.BackdropCache.put(item.id, item.thumbnail)
                                                        navController.navigate("album/${item.id}")
                                                    }
                                                    is ArtistItem -> {
                                                        navController.navigateToArtist(context, item.id, item.title, item.thumbnail)
                                                    }
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

        if (showArtistDescriptionSheet) {
            val description = artistPage?.description?.replace("From Wikipedia (", "")?.trim()
            if (!description.isNullOrEmpty()) {
                ModalBottomSheet(
                    onDismissRequest = { showArtistDescriptionSheet = false },
                    containerColor = TamedAppleColors.Surface,
                    contentColor = Color.White,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "About ${artistPage?.artist?.title ?: libraryArtist?.artist?.name ?: "Artist"}",
                            style = TamedAppleTypography.sectionTitle(),
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TamedAppleColors.SecondaryText,
                            lineHeight = 24.sp,
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .align(Alignment.BottomCenter)
        )
    }

    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val appBarAlpha by animateFloatAsState(
        targetValue = if (transparentAppBar) 0f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "appBarAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val alpha = appBarAlpha
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F0F11).copy(alpha = alpha * 0.85f),
                            Color(0xFF0F0F11).copy(alpha = alpha * 0.40f),
                            Color.Transparent
                        )
                    )
                )
            }
            .padding(horizontal = 20.dp)
            .padding(
                top = systemBarsTopPadding + 8.dp,
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
                onPhoto = true,
                buttonSize = 36.dp,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlassIconCircleButton(
                    iconRes = R.drawable.share,
                    contentDescription = "Share",
                    onClick = {
                        viewModel.artistPage?.artist?.shareLink?.let { link ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Artist Link", link)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onPhoto = true,
                    buttonSize = 36.dp,
                )

                val artistItem = artistPage?.artist?.let {
                    com.tamed.music.innertube.models.ArtistItem(
                        id = it.id,
                        title = it.title,
                        thumbnail = it.thumbnail,
                        shuffleEndpoint = it.shuffleEndpoint,
                        radioEndpoint = it.radioEndpoint,
                    )
                }
                GlassIconCircleButton(
                    iconRes = R.drawable.more_horiz,
                    contentDescription = "More options",
                    onClick = {
                        if (artistItem != null) {
                            menuState.show {
                                YouTubeArtistMenu(
                                    artist = artistItem,
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

private data class FeaturedRelease(
    val id: String,
    val title: String,
    val thumbnail: String?,
    val year: String?,
    val isExplicit: Boolean = false,
)

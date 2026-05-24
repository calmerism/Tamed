/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.tamed.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import com.tamed.music.ui.screens.rememberAlbumCanvas
import com.tamed.music.ui.player.CanvasArtworkPlayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ContainedLoadingIndicator
import com.tamed.music.ui.component.shimmer.ShimmerHost
import com.tamed.music.ui.component.shimmer.ButtonPlaceholder
import com.tamed.music.ui.component.shimmer.ListItemPlaceHolder
import com.tamed.music.ui.component.shimmer.TextPlaceholder
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.tamed.music.extensions.togglePlayPause
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.tamed.music.constants.AppBarHeight
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastForEachReversed
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.tamed.music.LocalDatabase
import com.tamed.music.LocalDownloadUtil
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.R
import com.tamed.music.constants.HideExplicitKey
import com.tamed.music.db.entities.Album
import com.tamed.music.playback.ExoDownloadService
import com.tamed.music.playback.queues.LocalAlbumRadio
import com.tamed.music.ui.component.AlbumGradient

import com.tamed.music.ui.component.IconButton

import com.tamed.music.ui.component.LocalMenuState
import com.tamed.music.ui.component.NavigationTitle
import com.tamed.music.ui.component.SongListItem
import com.tamed.music.ui.component.YouTubeGridItem
import com.tamed.music.ui.menu.AlbumMenu
import com.tamed.music.ui.menu.SelectionSongMenu
import com.tamed.music.ui.menu.SongMenu
import com.tamed.music.ui.menu.YouTubeAlbumMenu
import com.tamed.music.ui.utils.backToMain

import com.tamed.music.utils.rememberPreference
import com.tamed.music.viewmodels.AlbumViewModel
import com.tamed.music.ui.component.LinkSegment
import com.tamed.music.ui.component.ExpandableText
import com.tamed.music.ui.component.LosslessDownloadSheet
import com.tamed.music.spotiflac.SpotiFlacDownloader
import com.tamed.music.spotiflac.SpotiFlacHandoff
import com.tamed.music.App
import com.tamed.music.utils.LocalFlacLibraryImporter
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlbumScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return

    val scope = rememberCoroutineScope()

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlistId by viewModel.playlistId.collectAsState()
    val albumWithSongs by viewModel.albumWithSongs.collectAsState()
    val otherVersions by viewModel.otherVersions.collectAsState()

    val description by viewModel.albumDescription.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val filteredSongs = remember(albumWithSongs, hideExplicit) {
        var songs = albumWithSongs?.songs ?: emptyList()
        if (hideExplicit) {
            songs = songs.filter { !it.song.explicit }
        }
        
        songs
    }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }
    if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    LaunchedEffect(filteredSongs) {
        selection.fastForEachReversed { songId ->
            if (filteredSongs.find { it.id == songId } == null) {
                selection.remove(songId)
            }
        }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    var showLosslessDownloadSheet by remember { mutableStateOf(false) }

    LaunchedEffect(albumWithSongs) {
        if (albumWithSongs?.album?.isLocal == true) {
            downloadState = Download.STATE_COMPLETED
            return@LaunchedEffect
        }
        val songs = albumWithSongs?.songs?.map { it.id }
        if (songs.isNullOrEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it]?.state == Download.STATE_QUEUED ||
                                downloads[it]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    val hasExplicitContent = remember(albumWithSongs) {
        albumWithSongs?.album?.explicit == true
    }
    val albumArtists = remember(albumWithSongs) {
        albumWithSongs?.artists?.takeIf { it.isNotEmpty() }
            ?: albumWithSongs?.songs?.firstOrNull()?.artists.orEmpty()
    }
    val albumArtistNames = remember(albumArtists) { albumArtists.joinToString { it.name } }

    val lazyListState = rememberLazyListState()

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
        }
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        val albumWithSongs = albumWithSongs
        if (albumWithSongs != null && albumWithSongs.songs.isNotEmpty()) {
            item(key = "album_header") {
                val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
                val headerOffset = with(LocalDensity.current) {
                    -(systemBarsTopPadding + AppBarHeight).roundToPx()
                }

                val firstSongTitle = albumWithSongs.songs.firstOrNull()?.title
                val canvasArtwork = rememberAlbumCanvas(
                    albumTitle = albumWithSongs.album.title,
                    artistName = albumArtists.firstOrNull()?.name,
                    firstSongTitle = firstSongTitle
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    if (canvasArtwork != null) {
                        CanvasArtworkPlayer(
                            primaryUrl = canvasArtwork.preferredAnimationUrl,
                            fallbackUrl = canvasArtwork.videoUrl,
                            isPlaying = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .offset { IntOffset(x = 0, y = headerOffset) }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .offset { IntOffset(x = 0, y = headerOffset) }
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background
                                        )
                                    )
                                )
                        )
                    } else {
                        // Gradient background
                        AlbumGradient(
                            thumbnailUrl = albumWithSongs.album.thumbnailUrl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .offset { IntOffset(x = 0, y = headerOffset) }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(20.dp))

                        // Album Artwork
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 56.dp)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = albumWithSongs.album.thumbnailUrl,
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
                                text = albumWithSongs.album.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(6.dp))

                            if (albumArtists.size == 1 && albumWithSongs.artists.isNotEmpty()) {
                                val artist = albumArtists.first()
                                Text(
                                    text = artist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clickable { navController.navigate("artist/${artist.id}") }
                                )
                            } else if (albumArtistNames.isNotBlank()) {
                                Text(
                                    text = if (albumWithSongs.artists.isNotEmpty()) {
                                        buildAnnotatedString {
                                            albumWithSongs.artists.fastForEachIndexed { idx, artist ->
                                                val link = LinkAnnotation.Clickable(artist.id) {
                                                    navController.navigate("artist/${artist.id}")
                                                }
                                                withLink(link) { append(artist.name) }
                                                if (idx != albumWithSongs.artists.lastIndex) append(", ")
                                            }
                                        }
                                    } else {
                                        buildAnnotatedString { append(albumArtistNames) }
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (albumWithSongs.artists.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (hasExplicitContent) {
                                    Icon(
                                        painter = painterResource(R.drawable.explicit),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = buildString {
                                        append(stringResource(R.string.album_text))
                                        albumWithSongs.album.year?.let { append(" \u2022 $it") }
                                        append(" \u2022 ${albumWithSongs.songs.size} tracks")
                                        val totalSec = albumWithSongs.songs.sumOf { it.song.duration }
                                        val h = totalSec / 3600
                                        val m = (totalSec % 3600) / 60
                                        if (h > 0) append(" \u2022 ${h}h ${m}m") else append(" \u2022 ${m}m")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Primary buttons: Save + Play
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.OutlinedButton(
                                onClick = { database.query { update(albumWithSongs.album.toggleLike()) } },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (albumWithSongs.album.bookmarkedAt != null) R.drawable.favorite
                                        else R.drawable.favorite_border
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (albumWithSongs.album.bookmarkedAt != null)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (albumWithSongs.album.bookmarkedAt != null)
                                        stringResource(R.string.saved) else stringResource(R.string.save),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            androidx.compose.material3.Button(
                                onClick = {
                                    if (isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id) {
                                        playerConnection.player.pause()
                                    } else if (mediaMetadata?.album?.id == albumWithSongs.album.id) {
                                        playerConnection.player.play()
                                    } else {
                                        playerConnection.playQueue(LocalAlbumRadio(albumWithSongs, startIndex = 0))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id)
                                            R.drawable.pause else R.drawable.play
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id)
                                        stringResource(R.string.pause) else stringResource(R.string.play),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Secondary buttons: Download + Shuffle + Share
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        ) {
                            ToggleButton(
                                checked = downloadState == Download.STATE_COMPLETED || downloadState == Download.STATE_DOWNLOADING,
                                onCheckedChange = {
                                    when (downloadState) {
                                        Download.STATE_COMPLETED, Download.STATE_DOWNLOADING ->
                                            albumWithSongs.songs.forEach { song ->
                                                DownloadService.sendRemoveDownload(
                                                    context, ExoDownloadService::class.java, song.id, false
                                                )
                                            }
                                        else ->
                                            showLosslessDownloadSheet = true
                                    }
                                },
                                modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                            ) {
                                when (downloadState) {
                                    Download.STATE_COMPLETED -> Icon(painterResource(R.drawable.offline), null, modifier = Modifier.size(18.dp))
                                    Download.STATE_DOWNLOADING -> CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    else -> Icon(painterResource(R.drawable.download), null, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                Text(
                                    text = when (downloadState) {
                                        Download.STATE_COMPLETED -> stringResource(R.string.saved)
                                        Download.STATE_DOWNLOADING -> stringResource(R.string.saving)
                                        else -> stringResource(R.string.download)
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            ToggleButton(
                                checked = false,
                                onCheckedChange = {
                                    playerConnection.playQueue(
                                        LocalAlbumRadio(albumWithSongs.copy(songs = albumWithSongs.songs.shuffled()))
                                    )
                                },
                                modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
                            ) {
                                Icon(painterResource(R.drawable.shuffle), null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.shuffle), style = MaterialTheme.typography.labelMedium)
                            }

                            ToggleButton(
                                checked = false,
                                onCheckedChange = {
                                    val intent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(
                                            android.content.Intent.EXTRA_TEXT,
                                            "https://music.youtube.com/playlist?list=${albumWithSongs.album.playlistId}"
                                        )
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, null))
                                },
                                modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                            ) {
                                Icon(painterResource(R.drawable.share), null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.share), style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // About section
                        if (!description.isNullOrBlank()) {
                            Spacer(Modifier.height(16.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.about_album),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                ExpandableText(text = description.orEmpty(), collapsedMaxLines = 3)
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }


            if (filteredSongs.isNotEmpty()) {
                itemsIndexed(
                    items = filteredSongs,
                    key = { _, song -> song.id },
                ) { index, song ->
                    val onCheckedChange: (Boolean) -> Unit = {
                        if (it) {
                            selection.add(song.id)
                        } else {
                            selection.remove(song.id)
                        }
                    }

                    SongListItem(
                        song = song,
//                        albumIndex = index + 1,
                        isActive = song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        showInLibraryIcon = true,
                        trailingContent = {
                            if (inSelectMode) {
                                Checkbox(
                                    checked = song.id in selection,
                                    onCheckedChange = onCheckedChange
                                )
                            } else {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = song,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                    )
                                }
                            }
                        },
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .combinedClickable(
                                onClick = {
                                    if (inSelectMode) {
                                        onCheckedChange(song.id !in selection)
                                    } else if (song.id == mediaMetadata?.id) {
                                        if (playerConnection.player.playWhenReady) {
                                            playerConnection.player.pause()
                                        } else {
                                            playerConnection.player.play()
                                        }
                                    } else {
                                        playerConnection.playQueue(
                                            LocalAlbumRadio(albumWithSongs, startIndex = index),
                                        )
                                    }
                                },
                                onLongClick = {
                                    if (!inSelectMode) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        inSelectMode = true
                                        onCheckedChange(true)
                                    }
                                },
                            ),
                    )
                }
            }

            if (otherVersions.isNotEmpty()) {
                item(key = "other_versions_title") {
                    NavigationTitle(
                        title = stringResource(R.string.other_versions),
                        modifier = Modifier.animateItem()
                    )
                }
                item(key = "other_versions_list") {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                    ) {
                        items(
                            items = otherVersions.distinctBy { it.id },
                            key = { it.id },
                        ) { item ->
                            YouTubeGridItem(
                                item = item,
                                isActive = mediaMetadata?.album?.id == item.id,
                                isPlaying = isPlaying,
                                coroutineScope = scope,
                                modifier =
                                Modifier
                                    .combinedClickable(
                                        onClick = { navController.navigate("album/${item.id}") },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                YouTubeAlbumMenu(
                                                    albumItem = item,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(Modifier.height(50.dp))
            }
        } else {
            item(key = "loading") {
                ShimmerHost(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(80.dp))
                    
                    // Album Cover Placeholder
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 56.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // Title Placeholder
                    TextPlaceholder(modifier = Modifier.width(200.dp))
                    Spacer(Modifier.height(8.dp))
                    
                    // Artist Placeholder
                    TextPlaceholder(modifier = Modifier.width(120.dp))
                    Spacer(Modifier.height(16.dp))
                    
                    // Metadata Placeholder
                    TextPlaceholder(modifier = Modifier.width(160.dp))
                    Spacer(Modifier.height(24.dp))
                    
                    // Buttons Placeholder
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ButtonPlaceholder(modifier = Modifier.weight(1f))
                        ButtonPlaceholder(modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        ButtonPlaceholder(modifier = Modifier.weight(1f))
                        ButtonPlaceholder(modifier = Modifier.weight(1f))
                        ButtonPlaceholder(modifier = Modifier.weight(1f))
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    
                    // Songs Placeholder
                    repeat(6) {
                        ListItemPlaceHolder()
                    }
                }
            }
        }
    }

    if (showLosslessDownloadSheet && albumWithSongs != null && !albumWithSongs!!.album.isLocal) {
        val album = albumWithSongs!!
        val songs = album.songs
        LosslessDownloadSheet(
            title = album.album.title,
            subtitle = albumArtists.joinToString(", ") { it.name },
            trackCount = songs.size,
            onDismiss = { showLosslessDownloadSheet = false },
            enableYouTubeDownload = true,
            onStartYouTube = {
                showLosslessDownloadSheet = false
                songs.forEach { song ->
                    val request = DownloadRequest.Builder(song.id, song.id.toUri())
                        .setCustomCacheKey(song.id)
                        .setData(song.song.title.toByteArray())
                        .build()
                    DownloadService.sendAddDownload(
                        context,
                        ExoDownloadService::class.java,
                        request,
                        false,
                    )
                }
            },
            onStart = { provider, quality ->
                showLosslessDownloadSheet = false
                Toast.makeText(context, context.getString(R.string.starting_lossless_download), Toast.LENGTH_SHORT).show()
                App.launchOnAppScope {
                    val imported =
                        LocalFlacLibraryImporter.importMatchingAlbum(
                            context = context.applicationContext,
                            database = database,
                            albumWithSongs = album,
                        )

                    if (imported.imported > 0) {
                        Toast
                            .makeText(
                                context,
                                context.getString(R.string.flac_imported_tracks, imported.imported, imported.total),
                                Toast.LENGTH_SHORT,
                            ).show()
                    }

                    if (imported.imported < imported.total) {
                        val results =
                            SpotiFlacDownloader.downloadAlbum(
                                context = context.applicationContext,
                                database = database,
                                albumId = album.album.id,
                                albumTitle = album.album.title,
                                albumArtistNames = album.artists.map { it.name },
                                thumbnailUrl = album.album.thumbnailUrl,
                                year = album.album.year,
                                songs = album.songs.filterNot { it.song.isLocal },
                                provider = provider,
                                quality = quality,
                            )
                        val successCount = results.count { res -> res is SpotiFlacDownloader.Result.Success }
                        val backendMissing = results.any { res -> res is SpotiFlacDownloader.Result.BackendMissing }
                        
                        if (successCount > 0) {
                            Toast.makeText(context, context.getString(R.string.flac_downloaded_tracks, successCount, results.size), Toast.LENGTH_LONG).show()
                        }
                        val firstFailure = results.firstOrNull { res -> res is SpotiFlacDownloader.Result.Failed } as? SpotiFlacDownloader.Result.Failed
                        when {
                            backendMissing ->
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(
                                            SpotiFlacHandoff.openForAlbum(
                                                context = context.applicationContext,
                                                albumTitle = album.album.title,
                                                artists = album.artists.map { it.name },
                                            ),
                                        ),
                                        Toast.LENGTH_LONG,
                                    )
                                    .show()
                            firstFailure != null ->
                                Toast
                                    .makeText(context, firstFailure.message, Toast.LENGTH_LONG)
                                    .show()
                        }
                    }
                }
            },
        )
    }

    TopAppBar(
        title = {
            if (inSelectMode) {
                Text(pluralStringResource(R.plurals.n_selected, selection.size, selection.size))
            } else {
                Text(
                    text = albumWithSongs?.album?.title.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            if (inSelectMode) {
                IconButton(onClick = onExitSelectionMode) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                    )
                }
            } else {
                IconButton(
                    onClick = { navController.navigateUp() },
                    onLongClick = { navController.backToMain() }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null
                    )
                }
            }
        },
        actions = {
            if (inSelectMode) {
                Checkbox(
                    checked = selection.size == filteredSongs.size && selection.isNotEmpty(),
                    onCheckedChange = {
                        if (selection.size == filteredSongs.size) {
                            selection.clear()
                        } else {
                            selection.clear()
                            selection.addAll(filteredSongs.map { it.id })
                        }
                    }
                )
                IconButton(
                    enabled = selection.isNotEmpty(),
                    onClick = {
                        menuState.show {
                            SelectionSongMenu(
                                songSelection = selection.mapNotNull { songId ->
                                    filteredSongs.find { it.id == songId }
                                },
                                onDismiss = menuState::dismiss,
                                clearAction = onExitSelectionMode
                            )
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null
                    )
                }
            }
        },
        colors = if (transparentAppBar) {
            androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        } else {
            androidx.compose.material3.TopAppBarDefaults.topAppBarColors()
        }
    )
}

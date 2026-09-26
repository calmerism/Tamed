@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.ui.menu

import com.tamed.music.App
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import com.tamed.music.utils.navigateToArtist
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.tamed.music.innertube.YouTube
import com.tamed.music.LocalDatabase
import com.tamed.music.LocalDownloadUtil
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.LocalSyncUtils
import com.tamed.music.R
import com.tamed.music.constants.ArtistSeparatorsKey
import com.tamed.music.constants.ExternalDownloaderEnabledKey
import com.tamed.music.constants.ExternalDownloaderPackageKey
import com.tamed.music.constants.ListItemHeight
import com.tamed.music.constants.ListThumbnailSize
import com.tamed.music.constants.SpeedDialSongIdsKey
import com.tamed.music.db.entities.ArtistEntity
import com.tamed.music.db.entities.Event
import com.tamed.music.db.entities.PlaylistSong
import com.tamed.music.db.entities.Song
import com.tamed.music.db.entities.SongArtistMap
import com.tamed.music.db.MusicDatabase
import com.tamed.music.extensions.toMediaItem
import com.tamed.music.models.isLikelyLosslessAudio
import com.tamed.music.models.toMediaMetadata
import com.tamed.music.playback.ExoDownloadService
import com.tamed.music.playback.queues.YouTubeQueue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.layout.ContentScale
import com.tamed.music.ui.component.AppleMenuItem
import com.tamed.music.ui.component.ListDialog
import com.tamed.music.ui.component.LocalBottomSheetPageState
import com.tamed.music.ui.component.SongListItem
import com.tamed.music.ui.component.TextFieldDialog
import com.tamed.music.ui.utils.ShowMediaInfo
import com.tamed.music.utils.makeTimeString
import com.tamed.music.utils.rememberPreference
import com.tamed.music.viewmodels.CachePlaylistViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SongMenu(
    originalSong: Song,
    event: Event? = null,
    navController: NavController,
    playlistSong: PlaylistSong? = null,
    playlistBrowseId: String? = null,
    onDismiss: () -> Unit,
    isFromCache: Boolean = false,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val songState = database.song(originalSong.id).collectAsState(initial = originalSong)
    val song = songState.value ?: originalSong
    val download by LocalDownloadUtil.current.getDownload(originalSong.id)
        .collectAsState(initial = null)
    val isLocalSong = song.song.isLocal && !song.song.localPath.isNullOrBlank()
    val localSongStatusText =
        if (isLikelyLosslessAudio(song.song.localMimeType, song.song.localPath ?: song.id)) {
            R.string.flac_already_in_library
        } else {
            R.string.local_file_in_library
        }
    val coroutineScope = rememberCoroutineScope()
    val syncUtils = LocalSyncUtils.current
    var refetchIconDegree by remember { mutableFloatStateOf(0f) }

    val cacheViewModel = hiltViewModel<CachePlaylistViewModel>()

    val rotationAnimation by animateFloatAsState(
        targetValue = refetchIconDegree,
        animationSpec = tween(durationMillis = 800),
        label = "",
    )

    // Artist separators for splitting artist names
    val (artistSeparators) = rememberPreference(ArtistSeparatorsKey, defaultValue = ",;/&")
    val (externalDownloaderEnabled) = rememberPreference(ExternalDownloaderEnabledKey, defaultValue = false)
    val (externalDownloaderPackage) = rememberPreference(ExternalDownloaderPackageKey, defaultValue = "")
    val (speedDialSongIds, onSpeedDialSongIdsChange) = rememberPreference(SpeedDialSongIdsKey, "")
    val speedDialSongs = remember(speedDialSongIds) {
        speedDialSongIds
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(24)
    }
    val isInSpeedDial = remember(speedDialSongs, song.id) { song.id in speedDialSongs }

    val orderedArtists by produceState(initialValue = emptyList<ArtistEntity>(), song) {
        withContext(Dispatchers.IO) {
            val artistMaps = database.songArtistMap(song.id).sortedBy { it.position }
            val sorted = artistMaps.mapNotNull { map ->
                song.artists.firstOrNull { it.id == map.artistId }
            }
            value = sorted
        }
    }

    // Split artists by configured separators
    data class SplitArtist(
        val name: String,
        val originalArtist: ArtistEntity?
    )

    val splitArtists = remember(orderedArtists, artistSeparators) {
        if (artistSeparators.isEmpty()) {
            orderedArtists.map { SplitArtist(it.name, it) }
        } else {
            val separatorRegex = "[${Regex.escape(artistSeparators)}]".toRegex()
            orderedArtists.flatMap { artist ->
                val parts = artist.name.split(separatorRegex).map { it.trim() }.filter { it.isNotEmpty() }
                if (parts.size > 1) {
                    // If the name contains separators, create split artists
                    // The first part keeps the original artist reference for navigation
                    parts.mapIndexed { index, name ->
                        SplitArtist(name, if (index == 0) artist else null)
                    }
                } else {
                    listOf(SplitArtist(artist.name, artist))
                }
            }
        }
    }

    var showEditDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val TextFieldValueSaver: Saver<TextFieldValue, *> = Saver(
        save = { it.text },
        restore = { text -> TextFieldValue(text, TextRange(text.length)) }
    )

    var titleField by rememberSaveable(stateSaver = TextFieldValueSaver) {
        mutableStateOf(TextFieldValue(song.song.title))
    }

    var artistField by rememberSaveable(stateSaver = TextFieldValueSaver) {
        mutableStateOf(TextFieldValue(song.artists.firstOrNull()?.name.orEmpty()))
    }

    if (showEditDialog) {
        TextFieldDialog(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = null
                )
            },
            title = {
                Text(text = stringResource(R.string.edit_song))
            },
            textFields = listOf(
                stringResource(R.string.song_title) to titleField,
                stringResource(R.string.artist_name) to artistField
            ),
            onTextFieldsChange = { index, newValue ->
                if (index == 0) titleField = newValue
                else artistField = newValue
            },
            onDoneMultiple = { values ->
                val newTitle = values[0]
                val newArtist = values[1]

                coroutineScope.launch {
                    database.query {
                        update(song.song.copy(title = newTitle))
                        val artist = song.artists.firstOrNull()
                        if (artist != null) {
                            update(artist.copy(name = newArtist))
                        }
                    }

                    showEditDialog = false
                    onDismiss()
                }
            },
            onDismiss = { showEditDialog = false }
        )
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = {
            listOf(song.id)
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        },
        onAddComplete = { songCount, playlistNames ->
            val message = when {
                playlistNames.size == 1 -> context.getString(R.string.added_to_playlist, playlistNames.first())
                else -> context.getString(R.string.added_to_n_playlists, playlistNames.size)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            },
        ) {
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.already_in_playlist)) },
                    leadingContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier = Modifier.clickable { showErrorPlaylistAddDialog = false },
                )
            }

            items(listOf(song)) { song ->
                SongListItem(song = song)
            }
        }
    }

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(
                items = splitArtists.distinctBy { it.name },
                key = { it.name },
            ) { splitArtist ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = splitArtist.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = {
                        AsyncImage(
                            model = splitArtist.originalArtist?.thumbnailUrl,
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                        )
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                splitArtist.originalArtist?.let { artist ->
                                    navController.navigateToArtist(context, artist.id, artist.name, artist.thumbnailUrl)
                                    showSelectArtistDialog = false
                                    onDismiss()
                                }
                            },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        AsyncImage(
            model = song.song.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.song.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(),
            )
            if (orderedArtists.isNotEmpty()) {
                Text(
                    text = orderedArtists.joinToString { it.name },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(),
                )
            }
            val meta = listOfNotNull(
                song.song.albumName,
                song.song.duration.takeIf { it > 0 }?.let { makeTimeString(it * 1000L) },
            ).joinToString(" • ")
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        IconButton(
            onClick = {
                val s = song.song.toggleLike()
                database.query {
                    update(s)
                }
                syncUtils.likeSong(s)
            },
        ) {
            Icon(
                painter = painterResource(if (song.song.liked) R.drawable.favorite else R.drawable.favorite_border),
                tint = if (song.song.liked) MaterialTheme.colorScheme.error else LocalContentColor.current,
                contentDescription = null,
            )
        }
    }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(bottom = 4.dp),
    )

    val bottomSheetPageState = LocalBottomSheetPageState.current

    LazyColumn(
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item {
            AppleMenuItem(
                title = stringResource(if (song.song.inLibrary == null) R.string.add_to_library else R.string.remove_from_library),
                icon = if (song.song.inLibrary == null) R.drawable.library_add else R.drawable.library_add_check,
                onClick = {
                    onDismiss()
                    database.query {
                        update(song.song.toggleLibrary())
                    }
                }
            )
        }
        item {
            AppleMenuItem(
                title = stringResource(R.string.add_to_playlist),
                icon = R.drawable.playlist_add,
                onClick = {
                    showChoosePlaylistDialog = true
                }
            )
        }
        item {
            AppleMenuItem(
                title = stringResource(R.string.play_next),
                icon = R.drawable.playlist_play,
                onClick = {
                    onDismiss()
                    playerConnection.playNext(song.toMediaItem())
                }
            )
        }
        item {
            AppleMenuItem(
                title = stringResource(R.string.add_to_queue),
                icon = R.drawable.queue_music,
                onClick = {
                    onDismiss()
                    playerConnection.addToQueue(song.toMediaItem())
                }
            )
        }
        item {
            AppleMenuItem(
                title = stringResource(R.string.start_radio),
                icon = R.drawable.radio,
                onClick = {
                    onDismiss()
                    playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                }
            )
        }
        item {
            AppleMenuItem(
                title = stringResource(R.string.share),
                icon = R.drawable.share,
                onClick = {
                    onDismiss()
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${song.id}")
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                }
            )
        }
        item {
            AppleMenuItem(
                title = stringResource(if (isInSpeedDial) R.string.remove_from_speed_dial else R.string.pin_to_speed_dial),
                icon = if (isInSpeedDial) R.drawable.bookmark_filled else R.drawable.bookmark,
                onClick = {
                    val updatedIds = if (isInSpeedDial) {
                        speedDialSongs.filterNot { it == song.id }
                    } else {
                        (speedDialSongs + song.id).distinct().take(24)
                    }
                    onSpeedDialSongIdsChange(updatedIds.joinToString(","))
                    onDismiss()
                }
            )
        }
        if (event != null) {
            item {
                AppleMenuItem(
                    title = stringResource(R.string.remove_from_history),
                    icon = R.drawable.delete,
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        onDismiss()
                        database.query {
                            delete(event)
                        }
                    }
                )
            }
        }
        if (playlistSong != null) {
            item {
                AppleMenuItem(
                    title = stringResource(R.string.remove_from_playlist),
                    icon = R.drawable.delete,
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        val map = playlistSong.map
                        coroutineScope.launch(Dispatchers.IO) {
                            database.withTransaction {
                                val maxPosition = maxPlaylistSongPosition(map.playlistId) ?: map.position
                                if (map.position < maxPosition) {
                                    move(map.playlistId, map.position, maxPosition)
                                }
                                delete(map)
                            }
                            val browseId = playlistBrowseId
                            if (browseId != null) {
                                removeSongFromRemotePlaylist(browseId, map)
                            }
                            withContext(Dispatchers.Main) {
                                onDismiss()
                            }
                        }
                    }
                )
            }
        }
        if (isFromCache) {
            item {
                AppleMenuItem(
                    title = stringResource(R.string.remove_from_cache),
                    icon = R.drawable.delete,
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        onDismiss()
                        cacheViewModel.removeSongFromCache(song.id)
                    }
                )
            }
        }
        item {
            when {
                isLocalSong -> {
                    AppleMenuItem(
                        title = stringResource(localSongStatusText),
                        icon = R.drawable.graphic_eq,
                        onClick = {}
                    )
                }
                download?.state == Download.STATE_COMPLETED -> {
                    AppleMenuItem(
                        title = stringResource(R.string.remove_download),
                        icon = R.drawable.offline,
                        tint = MaterialTheme.colorScheme.error,
                        onClick = {
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false,
                            )
                        }
                    )
                }
                download?.state == Download.STATE_QUEUED || download?.state == Download.STATE_DOWNLOADING -> {
                    AppleMenuItem(
                        title = stringResource(R.string.downloading),
                        iconContent = {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        onClick = {
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false,
                            )
                        }
                    )
                }
                else -> {
                    AppleMenuItem(
                        title = stringResource(R.string.action_download),
                        icon = R.drawable.download,
                        onClick = {
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
                            onDismiss()
                        }
                    )
                }
            }
        }
        if (externalDownloaderEnabled) {
            item {
                AppleMenuItem(
                    title = stringResource(R.string.open_with_downloader),
                    icon = R.drawable.download,
                    onClick = {
                        onDismiss()
                        val url = "https://music.youtube.com/watch?v=${song.id}"
                        if (externalDownloaderPackage.isBlank()) {
                            Toast.makeText(context, context.getString(R.string.external_downloader_not_configured), Toast.LENGTH_LONG).show()
                            return@AppleMenuItem
                        }
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setPackage(externalDownloaderPackage)
                            data = android.net.Uri.parse(url)
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: android.content.ActivityNotFoundException) {
                            Toast.makeText(context, context.getString(R.string.external_downloader_not_installed), Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
        item {
            AppleMenuItem(
                title = stringResource(R.string.view_artist),
                icon = R.drawable.artist,
                onClick = {
                    if (splitArtists.size == 1 && splitArtists[0].originalArtist != null) {
                        val singleArtist = splitArtists[0].originalArtist!!
                        navController.navigateToArtist(context, singleArtist.id, singleArtist.name, singleArtist.thumbnailUrl)
                        onDismiss()
                    } else {
                        showSelectArtistDialog = true
                    }
                }
            )
        }
        if (song.song.albumId != null) {
            item {
                AppleMenuItem(
                    title = stringResource(R.string.view_album),
                    icon = R.drawable.album,
                    onClick = {
                        onDismiss()
                        navController.navigate("album/${song.song.albumId}")
                    }
                )
            }
        }
        item {
            AppleMenuItem(
                title = stringResource(R.string.edit),
                icon = R.drawable.edit,
                onClick = {
                    showEditDialog = true
                }
            )
        }
        item {
            AppleMenuItem(
                title = stringResource(R.string.refetch),
                iconContent = {
                    Icon(
                        painter = painterResource(R.drawable.sync),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp).graphicsLayer(rotationZ = rotationAnimation),
                    )
                },
                onClick = {
                    refetchIconDegree -= 360
                    coroutineScope.launch(Dispatchers.IO) {
                        YouTube.queue(listOf(song.id)).onSuccess {
                            val newSong = it.firstOrNull()
                            if (newSong != null) {
                                database.transaction {
                                    update(song, newSong.toMediaMetadata())
                                }
                            }
                        }
                    }
                }
            )
        }
        item {
            AppleMenuItem(
                title = stringResource(R.string.details),
                icon = R.drawable.info,
                onClick = {
                    onDismiss()
                    bottomSheetPageState.show {
                        ShowMediaInfo(song.id)
                    }
                }
            )
        }
    }
}

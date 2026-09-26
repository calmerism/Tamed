@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.tamed.music.ui.component.AppleMenuItem
import com.tamed.music.innertube.models.WatchEndpoint
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.ListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import coil3.compose.AsyncImage
import com.tamed.music.innertube.YouTube
import com.tamed.music.innertube.models.PlaylistItem
import com.tamed.music.innertube.models.SongItem
import com.tamed.music.innertube.utils.completed
import com.tamed.music.LocalDatabase
import com.tamed.music.LocalDownloadUtil
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.LocalSyncUtils
import com.tamed.music.R
import com.tamed.music.constants.ListThumbnailSize
import com.tamed.music.constants.ThumbnailCornerRadius
import com.tamed.music.db.entities.PlaylistEntity
import com.tamed.music.db.entities.PlaylistSongMap
import com.tamed.music.extensions.toMediaItem
import com.tamed.music.models.MediaMetadata
import com.tamed.music.models.toMediaMetadata
import com.tamed.music.playback.ExoDownloadService
import com.tamed.music.playback.queues.YouTubeQueue
import com.tamed.music.ui.component.DefaultDialog
import com.tamed.music.ui.component.ListDialog
import com.tamed.music.ui.component.MenuSurfaceSection
import com.tamed.music.ui.component.NewAction
import com.tamed.music.ui.component.NewActionGrid
import com.tamed.music.ui.component.YouTubeListItem
import com.tamed.music.utils.joinByBullet
import com.tamed.music.utils.makeTimeString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun YouTubePlaylistMenu(
    playlist: PlaylistItem,
    songs: List<SongItem> = emptyList(),
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
    selectAction: () -> Unit = {},
    canSelect: Boolean = false,
    snackbarHostState: androidx.compose.material3.SnackbarHostState? = null,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val syncUtils = LocalSyncUtils.current
    val dbPlaylist by database.playlistByBrowseId(playlist.id).collectAsState(initial = null)

    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showImportPlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showErrorPlaylistAddDialog by rememberSaveable { mutableStateOf(false) }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<MediaMetadata>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = {
            val allSongs = songs
                .ifEmpty {
                    YouTube.playlist(playlist.id).completed().getOrNull()?.songs.orEmpty()
                }.map {
                    it.toMediaMetadata()
                }
            database.transaction {
                allSongs.forEach(::insert)
            }
            allSongs.map { it.id }
        },
        onDismiss = { showChoosePlaylistDialog = false },
        onAddComplete = { songCount, playlistNames ->
            val message = when {
                songCount == 1 && playlistNames.size == 1 -> context.getString(R.string.added_to_playlist, playlistNames.first())
                songCount > 1 && playlistNames.size == 1 -> context.getString(R.string.added_n_songs_to_playlist, songCount, playlistNames.first())
                songCount == 1 -> context.getString(R.string.added_to_n_playlists, playlistNames.size)
                else -> context.getString(R.string.added_n_songs_to_n_playlists, songCount, playlistNames.size)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        AsyncImage(
            model = playlist.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val author = playlist.author?.name
            if (!author.isNullOrBlank()) {
                Text(
                    text = author,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val songCount = playlist.songCountText ?: if (songs.isNotEmpty()) "${songs.size} songs" else null
            if (!songCount.isNullOrBlank()) {
                Text(
                    text = songCount,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (playlist.id != "LM" && !playlist.isEditable) {
            IconButton(
                onClick = {
                    if (dbPlaylist?.playlist == null) {
                        database.transaction {
                            val playlistEntity =
                                PlaylistEntity(
                                    name = playlist.title,
                                    browseId = playlist.id,
                                    thumbnailUrl = playlist.thumbnail,
                                    isEditable = false,
                                    remoteSongCount = playlist.songCountText?.let {
                                        Regex("""\d+""").find(it)?.value?.toIntOrNull()
                                    },
                                    playEndpointParams = playlist.playEndpoint?.params,
                                    shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                    radioEndpointParams = playlist.radioEndpoint?.params,
                                ).toggleLike()
                            insert(playlistEntity)
                            coroutineScope.launch(Dispatchers.IO) {
                                songs.ifEmpty {
                                    YouTube.playlist(playlist.id).completed().getOrNull()?.songs.orEmpty()
                                }.onEach { song -> insert(song.toMediaMetadata()) }
                                    .mapIndexed { index, song ->
                                        PlaylistSongMap(
                                            songId = song.id,
                                            playlistId = playlistEntity.id,
                                            position = index,
                                            setVideoId = song.setVideoId,
                                        )
                                    }
                                    .forEach(::insert)
                            }
                        }
                    } else {
                        database.transaction {
                            val currentPlaylist = dbPlaylist!!.playlist
                            update(currentPlaylist, playlist)
                            update(currentPlaylist.toggleLike())
                        }
                    }
                },
            ) {
                Icon(
                    painter = painterResource(if (dbPlaylist?.playlist?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border),
                    tint = if (dbPlaylist?.playlist?.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current,
                    contentDescription = null,
                )
            }
        }
    }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(bottom = 4.dp),
    )

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }
    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == Download.STATE_COMPLETED })
                    Download.STATE_COMPLETED
                else if (songs.all {
                        downloads[it.id]?.state == Download.STATE_QUEUED
                                || downloads[it.id]?.state == Download.STATE_DOWNLOADING
                                || downloads[it.id]?.state == Download.STATE_COMPLETED
                    })
                    Download.STATE_DOWNLOADING
                else
                    Download.STATE_STOPPED
        }
    }
    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }
    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(
                        R.string.remove_download_playlist_confirm,
                        playlist.title
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = { showRemoveDownloadDialog = false },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false
                            )
                        }
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    ImportPlaylistDialog(
        isVisible = showImportPlaylistDialog,
        onGetSong = {
            val allSongs = songs
                .ifEmpty {
                    YouTube.playlist(playlist.id).completed().getOrNull()?.songs.orEmpty()
                }.map {
                    it.toMediaMetadata()
                }
            database.transaction {
                allSongs.forEach(::insert)
            }
            allSongs.map { it.id }
        },
        playlistTitle = playlist.title,
        browseId = playlist.id,
        snackbarHostState = snackbarHostState,
        onDismiss = { showImportPlaylistDialog = false }
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

            items(notAddedList) { song ->
                ListItem(
                    headlineContent = { Text(text = song.title) },
                    leadingContent = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(ListThumbnailSize),
                        ) {
                            AsyncImage(
                                model = song.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                            )
                        }
                    },
                    supportingContent = {
                        Text(
                            text = joinByBullet(
                                song.artists.joinToString { it.name },
                                makeTimeString(song.duration * 1000L),
                            )
                        )
                    },
                )
            }
        }
    }

    val toggleAutoSync: (Boolean) -> Unit = { newValue ->
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val currentDbPlaylist = dbPlaylist
                if (currentDbPlaylist?.playlist == null) {
                    val playlistPage = YouTube.playlist(playlist.id).completed().getOrNull()
                    val fetchedSongs = playlistPage?.songs.orEmpty()

                    if (fetchedSongs.isEmpty() && newValue) {
                        withContext(Dispatchers.Main) {
                            if (snackbarHostState != null) {
                                snackbarHostState.showSnackbar(context.getString(R.string.import_failed))
                            } else {
                                Toast.makeText(context, context.getString(R.string.import_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                        return@launch
                    }

                    database.transaction {
                        val playlistEntity =
                            PlaylistEntity(
                                name = playlist.title,
                                browseId = playlist.id,
                                thumbnailUrl = playlist.thumbnail,
                                isEditable = false,
                                isAutoSync = newValue,
                                remoteSongCount = playlist.songCountText?.let {
                                    Regex("""\d+""").find(it)?.value?.toIntOrNull()
                                },
                                playEndpointParams = playlist.playEndpoint?.params,
                                shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                radioEndpointParams = playlist.radioEndpoint?.params,
                            )
                        insert(playlistEntity)
                        fetchedSongs.forEach { song -> insert(song.toMediaMetadata()) }
                        fetchedSongs.mapIndexed { index, song ->
                            PlaylistSongMap(
                                songId = song.id,
                                playlistId = playlistEntity.id,
                                position = index,
                                setVideoId = song.setVideoId,
                            )
                        }.forEach(::insert)
                    }

                    if (newValue) {
                        withContext(Dispatchers.Main) {
                            if (snackbarHostState != null) {
                                snackbarHostState.showSnackbar(context.getString(R.string.playlist_synced))
                            } else {
                                Toast.makeText(context, context.getString(R.string.playlist_synced), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    val existing = currentDbPlaylist.playlist
                    database.query {
                        update(existing.copy(isAutoSync = newValue))
                    }

                    if (newValue) {
                        syncUtils.syncAutoSyncPlaylists()
                        withContext(Dispatchers.Main) {
                            if (snackbarHostState != null) {
                                snackbarHostState.showSnackbar(context.getString(R.string.playlist_synced))
                            } else {
                                Toast.makeText(context, context.getString(R.string.playlist_synced), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val errorMsg = context.getString(R.string.import_failed) + ": ${e.message ?: "Unknown error"}"
                    if (snackbarHostState != null) {
                        snackbarHostState.showSnackbar(errorMsg)
                    } else {
                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    LazyColumn(
        userScrollEnabled = !isPortrait,
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        // 1. Play
        val hasPlay = playlist.playEndpoint != null || songs.isNotEmpty()
        if (hasPlay) {
            item {
                AppleMenuItem(
                    title = stringResource(R.string.play),
                    icon = R.drawable.play,
                    onClick = {
                        if (playlist.playEndpoint != null) {
                            playerConnection.playQueue(YouTubeQueue(playlist.playEndpoint!!))
                        } else if (songs.isNotEmpty()) {
                            playerConnection.playQueue(
                                YouTubeQueue(
                                    endpoint = WatchEndpoint(videoId = songs.first().id),
                                    preloadItem = songs.first().toMediaMetadata(),
                                )
                            )
                        }
                        onDismiss()
                    },
                )
            }
        }

        // 2. Shuffle
        val hasShuffle = playlist.shuffleEndpoint != null || songs.isNotEmpty()
        if (hasShuffle) {
            item {
                AppleMenuItem(
                    title = stringResource(R.string.shuffle),
                    icon = R.drawable.shuffle,
                    onClick = {
                        if (playlist.shuffleEndpoint != null) {
                            playerConnection.playQueue(YouTubeQueue(playlist.shuffleEndpoint!!))
                        } else if (songs.isNotEmpty()) {
                            val shuffled = songs.shuffled()
                            playerConnection.playQueue(
                                YouTubeQueue(
                                    endpoint = WatchEndpoint(videoId = shuffled.first().id),
                                    preloadItem = shuffled.first().toMediaMetadata(),
                                )
                            )
                        }
                        onDismiss()
                    },
                )
            }
        }

        // 3. Start Radio
        playlist.radioEndpoint?.let { radioEndpoint ->
            item {
                AppleMenuItem(
                    title = stringResource(R.string.start_radio),
                    icon = R.drawable.radio,
                    onClick = {
                        playerConnection.playQueue(YouTubeQueue(radioEndpoint))
                        onDismiss()
                    },
                )
            }
        }

        // 4. Play next
        item {
            AppleMenuItem(
                title = stringResource(R.string.play_next),
                icon = R.drawable.playlist_play,
                onClick = {
                    coroutineScope.launch {
                        songs
                            .ifEmpty {
                                withContext(Dispatchers.IO) {
                                    YouTube.playlist(playlist.id).completed().getOrNull()?.songs.orEmpty()
                                }
                            }.let { list ->
                                playerConnection.playNext(list.map { it.toMediaItem() })
                            }
                    }
                    onDismiss()
                },
            )
        }

        // 5. Add to queue
        item {
            AppleMenuItem(
                title = stringResource(R.string.add_to_queue),
                icon = R.drawable.queue_music,
                onClick = {
                    coroutineScope.launch {
                        songs
                            .ifEmpty {
                                withContext(Dispatchers.IO) {
                                    YouTube.playlist(playlist.id).completed().getOrNull()?.songs.orEmpty()
                                }
                            }.let { list ->
                                playerConnection.addToQueue(list.map { it.toMediaItem() })
                            }
                    }
                    onDismiss()
                },
            )
        }

        // 6. Add to playlist
        item {
            AppleMenuItem(
                title = stringResource(R.string.add_to_playlist),
                icon = R.drawable.playlist_add,
                onClick = {
                    showChoosePlaylistDialog = true
                },
            )
        }

        // 7. Download
        item {
            when (downloadState) {
                Download.STATE_COMPLETED -> {
                    AppleMenuItem(
                        title = stringResource(R.string.remove_download),
                        icon = R.drawable.offline,
                        tint = MaterialTheme.colorScheme.error,
                        onClick = {
                            showRemoveDownloadDialog = true
                        },
                    )
                }

                Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                    AppleMenuItem(
                        title = stringResource(R.string.downloading),
                        iconContent = {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        onClick = {
                            showRemoveDownloadDialog = true
                        },
                    )
                }

                else -> {
                    AppleMenuItem(
                        title = stringResource(R.string.action_download),
                        icon = R.drawable.download,
                        onClick = {
                            coroutineScope.launch {
                                songs
                                    .ifEmpty {
                                        withContext(Dispatchers.IO) {
                                            YouTube.playlist(playlist.id).completed().getOrNull()?.songs.orEmpty()
                                        }
                                    }.forEach { song ->
                                        val downloadRequest =
                                            DownloadRequest.Builder(song.id, song.id.toUri())
                                                .setCustomCacheKey(song.id)
                                                .setData(song.title.toByteArray())
                                                .build()
                                        DownloadService.sendAddDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            downloadRequest,
                                            false,
                                        )
                                    }
                            }
                            onDismiss()
                        },
                    )
                }
            }
        }

        // 8. Share
        item {
            AppleMenuItem(
                title = stringResource(R.string.share),
                icon = R.drawable.share,
                onClick = {
                    val intent =
                        Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, playlist.shareLink)
                        }
                    context.startActivity(Intent.createChooser(intent, null))
                    onDismiss()
                },
            )
        }

        // 9. Import playlist
        item {
            AppleMenuItem(
                title = stringResource(R.string.import_playlist),
                icon = R.drawable.add,
                onClick = {
                    showImportPlaylistDialog = true
                },
            )
        }

        // 10. Auto sync with account
        item {
            val checked = dbPlaylist?.playlist?.isAutoSync ?: false
            AppleMenuItem(
                title = stringResource(R.string.yt_sync),
                icon = R.drawable.sync,
                trailingContent = {
                    Switch(
                        checked = checked,
                        onCheckedChange = { toggleAutoSync(it) }
                    )
                },
                onClick = {
                    toggleAutoSync(!checked)
                }
            )
        }

        // 11. Select (if canSelect)
        if (canSelect) {
            item {
                AppleMenuItem(
                    title = stringResource(R.string.select),
                    icon = R.drawable.select_all,
                    onClick = {
                        onDismiss()
                        selectAction()
                    },
                )
            }
        }
    }
}

/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.tamed.music.ui.menu

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.tamed.music.ui.component.AppleMenuItem
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.ListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.tamed.music.innertube.YouTube
import com.tamed.music.LocalDatabase
import com.tamed.music.LocalDownloadUtil
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.R
import com.tamed.music.db.entities.Playlist
import com.tamed.music.db.entities.PlaylistSong
import com.tamed.music.db.entities.Song
import com.tamed.music.extensions.toMediaItem
import com.tamed.music.playback.ExoDownloadService
import com.tamed.music.playback.queues.ListQueue
import com.tamed.music.playback.queues.YouTubeQueue
import com.tamed.music.ui.component.DefaultDialog
import com.tamed.music.ui.component.AssignTagsDialog
import com.tamed.music.ui.component.EditPlaylistDialog
import com.tamed.music.ui.component.MenuSurfaceSection
import com.tamed.music.ui.component.NewAction
import com.tamed.music.ui.component.NewActionGrid
import com.tamed.music.ui.component.PlaylistListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

@Composable
fun PlaylistMenu(
    playlist: Playlist,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
    autoPlaylist: Boolean? = false,
    downloadPlaylist: Boolean? = false,
    songList: List<Song>? = emptyList(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val dbPlaylist by database.playlist(playlist.id).collectAsState(initial = playlist)
    var songs by remember {
        mutableStateOf(emptyList<Song>())
    }

    LaunchedEffect(Unit) {
        if (autoPlaylist == false) {
            database.playlistSongs(playlist.id).collect {
                songs = it.map(PlaylistSong::song)
            }
        } else {
            if (songList != null) {
                songs = songList
            }
        }
    }

    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    val editable: Boolean = playlist.playlist.isEditable == true

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it.id]?.state == Download.STATE_QUEUED ||
                                downloads[it.id]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it.id]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    var showEditDialog by remember {
        mutableStateOf(false)
    }

    if (showEditDialog) {
        EditPlaylistDialog(
            initialName = playlist.playlist.name,
            initialThumbnailUrl = playlist.playlist.thumbnailUrl,
            fallbackThumbnails = playlist.songThumbnails.filterNotNull(),
            onDismiss = { showEditDialog = false },
            onSave = { name, thumbnailUrl ->
                onDismiss()
                database.query {
                    update(
                        playlist.playlist.copy(
                            name = name,
                            thumbnailUrl = thumbnailUrl,
                            lastUpdateTime = LocalDateTime.now(),
                        )
                    )
                }
                coroutineScope.launch(Dispatchers.IO) {
                    playlist.playlist.browseId?.let { YouTube.renamePlaylist(it, name) }
                }
            },
        )
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
                        playlist.playlist.name
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                    },
                    shapes = ButtonDefaults.shapes(),
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
                                false,
                            )
                        }
                    },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    var showDeletePlaylistDialog by remember {
        mutableStateOf(false)
    }

    var showAssignTagsDialog by remember {
        mutableStateOf(false)
    }

    if (showAssignTagsDialog) {
        AssignTagsDialog(
            database = database,
            playlistId = playlist.id,
            onDismiss = { 
                showAssignTagsDialog = false
                onDismiss()
            }
        )
    }

    if (showDeletePlaylistDialog) {
        DefaultDialog(
            onDismiss = { showDeletePlaylistDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.delete_playlist_confirm, playlist.playlist.name),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                    },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                        onDismiss()
                        database.transaction {
                            if (playlist.playlist.bookmarkedAt != null) {
                                update(playlist.playlist.toggleLike())
                            }
                            delete(playlist.playlist)
                        }

                        coroutineScope.launch(Dispatchers.IO) {
                            playlist.playlist.browseId?.let { YouTube.deletePlaylist(it) }
                        }
                    },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        val thumbnail = playlist.thumbnails.firstOrNull()
        if (thumbnail != null) {
            AsyncImage(
                model = thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.playlist.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val songCountText = pluralStringResource(R.plurals.n_song, playlist.songCount, playlist.songCount)
            Text(
                text = songCountText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (playlist.playlist.isEditable != true) {
            IconButton(
                onClick = {
                    database.query {
                        dbPlaylist?.playlist?.toggleLike()?.let { update(it) }
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
        item {
            AppleMenuItem(
                title = stringResource(R.string.play),
                icon = R.drawable.play,
                onClick = {
                    onDismiss()
                    if (songs.isNotEmpty()) {
                        playerConnection.playQueue(
                            ListQueue(
                                title = playlist.playlist.name,
                                items = songs.map(Song::toMediaItem),
                            ),
                        )
                    }
                },
            )
        }

        // 2. Shuffle
        item {
            AppleMenuItem(
                title = stringResource(R.string.shuffle),
                icon = R.drawable.shuffle,
                onClick = {
                    onDismiss()
                    if (songs.isNotEmpty()) {
                        playerConnection.playQueue(
                            ListQueue(
                                title = playlist.playlist.name,
                                items = songs.shuffled().map(Song::toMediaItem),
                            ),
                        )
                    }
                },
            )
        }

        // 3. Start Radio
        playlist.playlist.browseId?.let { browseId ->
            item {
                AppleMenuItem(
                    title = stringResource(R.string.start_radio),
                    icon = R.drawable.radio,
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            YouTube.playlist(browseId).getOrNull()?.playlist?.let { playlistItem ->
                                playlistItem.radioEndpoint?.let { radioEndpoint ->
                                    withContext(Dispatchers.Main) {
                                        playerConnection.playQueue(YouTubeQueue(radioEndpoint))
                                    }
                                }
                            }
                        }
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
                        playerConnection.playNext(songs.map { it.toMediaItem() })
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
                    onDismiss()
                    playerConnection.addToQueue(songs.map { it.toMediaItem() })
                },
            )
        }

        // 6. Edit
        if (editable && autoPlaylist != true) {
            item {
                AppleMenuItem(
                    title = stringResource(R.string.edit),
                    icon = R.drawable.edit,
                    onClick = {
                        showEditDialog = true
                    },
                )
            }
        }

        // 7. Manage tags
        if (autoPlaylist != true && downloadPlaylist != true) {
            item {
                AppleMenuItem(
                    title = stringResource(R.string.manage_tags),
                    icon = R.drawable.style,
                    onClick = {
                        showAssignTagsDialog = true
                    },
                )
            }
        }

        // 8. Download
        if (downloadPlaylist != true) {
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
                                songs.forEach { song ->
                                    val downloadRequest =
                                        DownloadRequest
                                            .Builder(song.id, song.id.toUri())
                                            .setCustomCacheKey(song.id)
                                            .setData(song.song.title.toByteArray())
                                            .build()
                                    DownloadService.sendAddDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        downloadRequest,
                                        false,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }

        // 9. Share
        val shareLink = playlist.playlist.shareLink ?: dbPlaylist?.playlist?.browseId?.let { "https://music.youtube.com/playlist?list=$it" }
        if (shareLink != null) {
            item {
                AppleMenuItem(
                    title = stringResource(R.string.share),
                    icon = R.drawable.share,
                    onClick = {
                        val intent =
                            Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareLink)
                            }
                        context.startActivity(Intent.createChooser(intent, null))
                        onDismiss()
                    },
                )
            }
        }

        // 10. Delete
        if (autoPlaylist != true) {
            item {
                AppleMenuItem(
                    title = stringResource(R.string.delete),
                    icon = R.drawable.delete,
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        showDeletePlaylistDialog = true
                    },
                )
            }
        }
    }
}

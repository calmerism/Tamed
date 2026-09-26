@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.ui.menu

import com.tamed.music.App
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.tamed.music.utils.navigateToArtist
import androidx.compose.material3.ListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import com.tamed.music.innertube.YouTube
import com.tamed.music.innertube.models.AlbumItem
import com.tamed.music.LocalDatabase
import com.tamed.music.LocalDownloadUtil
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.R
import com.tamed.music.constants.ArtistSeparatorsKey
import com.tamed.music.constants.ListItemHeight
import com.tamed.music.constants.ListThumbnailSize
import com.tamed.music.db.entities.Song
import com.tamed.music.extensions.toMediaItem
import com.tamed.music.playback.ExoDownloadService
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.tamed.music.ui.component.AppleMenuItem
import com.tamed.music.ui.component.ListDialog
import com.tamed.music.ui.component.LocalBottomSheetPageState
import com.tamed.music.ui.component.SongListItem
import com.tamed.music.utils.rememberPreference
import com.tamed.music.utils.reportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun YouTubeAlbumMenu(
    albumItem: AlbumItem,
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val album by database.albumWithSongs(albumItem.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        database.album(albumItem.id).collect { album ->
            if (album == null) {
                YouTube
                    .album(albumItem.id)
                    .onSuccess { albumPage ->
                        database.transaction {
                            insert(albumPage)
                        }
                    }.onFailure {
                        reportException(it)
                    }
            }
        }
    }

    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(album) {
        val songs = album?.songs?.map { it.id } ?: return@LaunchedEffect
        if (album?.album?.isLocal == true) {
            downloadState = Download.STATE_COMPLETED
            return@LaunchedEffect
        }
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

    // Artist separators for splitting artist names
    val (artistSeparators) = rememberPreference(ArtistSeparatorsKey, defaultValue = ",;/&")

    // Split artists by configured separators
    data class SplitArtist(
        val name: String,
        val originalArtist: com.tamed.music.db.entities.ArtistEntity?
    )

    val splitArtists = remember(album?.artists, artistSeparators) {
        val artists = album?.artists ?: emptyList()
        if (artistSeparators.isEmpty()) {
            artists.map { SplitArtist(it.name, it) }
        } else {
            val separatorRegex = "[${Regex.escape(artistSeparators)}]".toRegex()
            artists.flatMap { artist ->
                val parts = artist.name.split(separatorRegex).map { it.trim() }.filter { it.isNotEmpty() }
                if (parts.size > 1) {
                    parts.mapIndexed { index, name ->
                        SplitArtist(name, if (index == 0) artist else null)
                    }
                } else {
                    listOf(SplitArtist(artist.name, artist))
                }
            }
        }
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<Song>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = {
            album?.songs?.map { it.id }.orEmpty()
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(ListItemHeight)
                        .clickable {
                            splitArtist.originalArtist?.let { artist ->
                                navController.navigateToArtist(context, artist.id, artist.name, null)
                                showSelectArtistDialog = false
                                onDismiss()
                            }
                        }
                        .padding(horizontal = 12.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .height(ListItemHeight)
                            .padding(horizontal = 24.dp),
                    ) {
                        Text(
                            text = splitArtist.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
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
            model = albumItem.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = albumItem.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val artistStr = albumItem.artists?.joinToString { it.name }
            if (!artistStr.isNullOrBlank()) {
                Text(
                    text = artistStr,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val meta = listOfNotNull(
                albumItem.year?.toString()
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
                database.query {
                    album?.album?.toggleLike()?.let(::update)
                }
            },
        ) {
            Icon(
                painter = painterResource(if (album?.album?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border),
                tint = if (album?.album?.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current,
                contentDescription = null,
            )
        }
    }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(bottom = 4.dp),
    )

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val bottomSheetPageState = LocalBottomSheetPageState.current

    LazyColumn(
        userScrollEnabled = !isPortrait,
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item {
            AppleMenuItem(
                title = stringResource(R.string.play),
                icon = R.drawable.play,
                onClick = {
                    onDismiss()
                    album?.songs?.let { songs ->
                        if (songs.isNotEmpty()) {
                            playerConnection.playQueue(com.tamed.music.playback.queues.YouTubeAlbumRadio(albumItem.playlistId))
                        }
                    }
                }
            )
        }
        item {
            AppleMenuItem(
                title = stringResource(R.string.shuffle),
                icon = R.drawable.shuffle,
                onClick = {
                    onDismiss()
                    album?.songs?.let { songs ->
                        if (songs.isNotEmpty()) {
                            playerConnection.playQueue(com.tamed.music.playback.queues.YouTubeAlbumRadio(albumItem.playlistId))
                        }
                    }
                }
            )
        }
        item {
            AppleMenuItem(
                title = stringResource(R.string.play_next),
                icon = R.drawable.playlist_play,
                onClick = {
                    album
                        ?.songs
                        ?.map { it.toMediaItem() }
                        ?.let(playerConnection::playNext)
                    onDismiss()
                }
            )
        }
        item {
            AppleMenuItem(
                title = stringResource(R.string.add_to_queue),
                icon = R.drawable.queue_music,
                onClick = {
                    album
                        ?.songs
                        ?.map { it.toMediaItem() }
                        ?.let(playerConnection::addToQueue)
                    onDismiss()
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
            when {
                album?.album?.isLocal == true -> {
                    AppleMenuItem(
                        title = stringResource(R.string.flac_already_in_library),
                        icon = R.drawable.graphic_eq,
                        onClick = {}
                    )
                }
                downloadState == Download.STATE_COMPLETED -> {
                    AppleMenuItem(
                        title = stringResource(R.string.remove_download),
                        icon = R.drawable.offline,
                        tint = MaterialTheme.colorScheme.error,
                        onClick = {
                            album?.songs?.forEach { song ->
                                DownloadService.sendRemoveDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    song.id,
                                    false,
                                )
                            }
                        }
                    )
                }
                downloadState == Download.STATE_QUEUED || downloadState == Download.STATE_DOWNLOADING -> {
                    AppleMenuItem(
                        title = stringResource(R.string.downloading),
                        iconContent = {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        onClick = {
                            album?.songs?.forEach { song ->
                                DownloadService.sendRemoveDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    song.id,
                                    false,
                                )
                            }
                        }
                    )
                }
                else -> {
                    AppleMenuItem(
                        title = stringResource(R.string.action_download),
                        icon = R.drawable.download,
                        onClick = {
                            album?.songs?.forEach { song ->
                                val req = DownloadRequest.Builder(song.id, song.id.toUri())
                                    .setCustomCacheKey(song.id)
                                    .setData(song.song.title.toByteArray())
                                    .build()
                                DownloadService.sendAddDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    req,
                                    false,
                                )
                            }
                            onDismiss()
                        }
                    )
                }
            }
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
                        putExtra(Intent.EXTRA_TEXT, albumItem.shareLink)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                }
            )
        }
        albumItem.artists?.let { artists ->
            item {
                AppleMenuItem(
                    title = stringResource(R.string.view_artist),
                    icon = R.drawable.artist,
                    onClick = {
                        if (splitArtists.size == 1 && splitArtists[0].originalArtist != null) {
                            val artist = splitArtists[0].originalArtist!!
                            navController.navigateToArtist(context, artist.id, artist.name, null)
                            onDismiss()
                        } else {
                            showSelectArtistDialog = true
                        }
                    }
                )
            }
        }
    }
}

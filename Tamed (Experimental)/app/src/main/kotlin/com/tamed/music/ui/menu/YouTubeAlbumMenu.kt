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
import com.tamed.music.playback.queues.YouTubeAlbumRadio
import com.tamed.music.ui.component.ListDialog
import com.tamed.music.ui.component.LosslessDownloadSheet
import com.tamed.music.ui.component.NewAction
import com.tamed.music.ui.component.NewActionGrid
import com.tamed.music.ui.component.SongListItem
import com.tamed.music.ui.component.YouTubeListItem
import com.tamed.music.spotiflac.SpotiFlacDownloader
import com.tamed.music.spotiflac.SpotiFlacHandoff
import com.tamed.music.utils.LocalFlacLibraryImporter
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

    var showLosslessDownloadSheet by remember { mutableStateOf(false) }

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

    if (showLosslessDownloadSheet && album != null) {
        LosslessDownloadSheet(
            title = album?.album?.title ?: albumItem.title,
            subtitle = album?.artists?.joinToString(", ") { it.name }.orEmpty(),
            trackCount = album?.songs?.size ?: 0,
            onDismiss = { showLosslessDownloadSheet = false },
            enableYouTubeDownload = true,
            onStartYouTube = {
                showLosslessDownloadSheet = false
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
            },
            onStart = { provider, quality ->
                showLosslessDownloadSheet = false
                Toast.makeText(context, context.getString(R.string.starting_lossless_download), Toast.LENGTH_SHORT).show()
                App.launchOnAppScope {
                    val currentAlbum = album ?: return@launchOnAppScope
                    val imported =
                        LocalFlacLibraryImporter.importMatchingAlbum(
                            context = context.applicationContext,
                            database = database,
                            albumWithSongs = currentAlbum,
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
                                albumId = currentAlbum.album.id,
                                albumTitle = currentAlbum.album.title,
                                albumArtistNames = currentAlbum.artists.map { it.name },
                                thumbnailUrl = currentAlbum.album.thumbnailUrl,
                                year = currentAlbum.album.year,
                                songs = currentAlbum.songs.filterNot { it.song.isLocal },
                                provider = provider,
                                quality = quality,
                            )
                        val successCount = results.count { it is SpotiFlacDownloader.Result.Success }
                        val backendMissing = results.any { it is SpotiFlacDownloader.Result.BackendMissing }
                        val firstFailure = results.firstOrNull { it is SpotiFlacDownloader.Result.Failed } as? SpotiFlacDownloader.Result.Failed
                        when {
                            successCount > 0 ->
                                Toast
                                    .makeText(context, context.getString(R.string.flac_downloaded_tracks, successCount, results.size), Toast.LENGTH_LONG)
                                    .show()
                            backendMissing ->
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(
                                            SpotiFlacHandoff.openForAlbum(
                                                context = context.applicationContext,
                                                albumTitle = currentAlbum.album.title,
                                                artists = currentAlbum.artists.map { it.name },
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
                                navController.navigate("artist/${artist.id}")
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

    YouTubeListItem(
        item = albumItem,
        badges = {},
        trailingContent = {
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
        },
    )

    HorizontalDivider()

    Spacer(modifier = Modifier.height(12.dp))

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
        item {
            // Enhanced Action Grid using NewMenuComponents
            NewActionGrid(
                actions = listOf(
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.play),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.play),
                        onClick = {
                            onDismiss()
                            album?.songs?.let { songs ->
                                if (songs.isNotEmpty()) {
                                    playerConnection.playQueue(YouTubeAlbumRadio(albumItem.playlistId))
                                }
                            }
                        }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.shuffle),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.shuffle),
                        onClick = {
                            onDismiss()
                            album?.songs?.let { songs ->
                                if (songs.isNotEmpty()) {
                                    playerConnection.playQueue(YouTubeAlbumRadio(albumItem.playlistId))
                                }
                            }
                        }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.share),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.share),
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
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
            )
        }
        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.play_next)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.playlist_play),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    album
                        ?.songs
                        ?.map { it.toMediaItem() }
                        ?.let(playerConnection::playNext)
                    onDismiss()
                }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.add_to_queue)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    album
                        ?.songs
                        ?.map { it.toMediaItem() }
                        ?.let(playerConnection::addToQueue)
                    onDismiss()
                }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.add_to_playlist)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.playlist_add),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    showChoosePlaylistDialog = true
                }
            )
        }
        item {
            when {
                album?.album?.isLocal == true -> {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.flac_already_in_library)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.graphic_eq),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                    )
                }
                downloadState == Download.STATE_COMPLETED -> {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.remove_download),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.offline),
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.clickable {
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
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.downloading)) },
                        leadingContent = {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        modifier = Modifier.clickable {
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
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.action_download)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.download),
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.clickable {
                            showLosslessDownloadSheet = true
                        }
                    )
                }
            }
        }
        albumItem.artists?.let { artists ->
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.view_artist)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.artist),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        if (splitArtists.size == 1 && splitArtists[0].originalArtist != null) {
                            navController.navigate("artist/${splitArtists[0].originalArtist!!.id}")
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

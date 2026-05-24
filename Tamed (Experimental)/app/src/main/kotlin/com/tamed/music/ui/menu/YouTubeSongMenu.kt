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
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
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
import coil3.compose.AsyncImage
import com.tamed.music.innertube.YouTube
import com.tamed.music.innertube.models.SongItem
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
import com.tamed.music.constants.ThumbnailCornerRadius
import com.tamed.music.db.entities.SongEntity
import com.tamed.music.extensions.toMediaItem
import com.tamed.music.models.MediaMetadata
import com.tamed.music.models.toMediaMetadata
import com.tamed.music.playback.ExoDownloadService
import com.tamed.music.playback.queues.YouTubeQueue
import com.tamed.music.spotiflac.SpotiFlacDownloader
import com.tamed.music.spotiflac.SpotiFlacHandoff
import com.tamed.music.ui.component.LosslessDownloadSheet
import com.tamed.music.ui.component.ListDialog
import com.tamed.music.ui.component.ListItem
import com.tamed.music.ui.component.LocalBottomSheetPageState
import com.tamed.music.ui.component.NewAction
import com.tamed.music.ui.component.NewActionGrid
import com.tamed.music.ui.utils.ShowMediaInfo
import com.tamed.music.utils.joinByBullet
import com.tamed.music.utils.makeTimeString
import com.tamed.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@SuppressLint("MutableCollectionMutableState")
@Composable
fun YouTubeSongMenu(
    song: SongItem,
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val librarySong by database.song(song.id).collectAsState(initial = null)
    val download by LocalDownloadUtil.current.getDownload(song.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val syncUtils = LocalSyncUtils.current
    val artists = remember {
        song.artists.mapNotNull {
            it.id?.let { artistId ->
                MediaMetadata.Artist(id = artistId, name = it.name)
            }
        }
    }

    // Artist separators for splitting artist names
    val (artistSeparators) = rememberPreference(ArtistSeparatorsKey, defaultValue = ",;/&")
    val (externalDownloaderEnabled) = rememberPreference(ExternalDownloaderEnabledKey, defaultValue = false)
    val (externalDownloaderPackage) = rememberPreference(ExternalDownloaderPackageKey, defaultValue = "")
    var showLosslessDownloadSheet by remember { mutableStateOf(false) }

    if (showLosslessDownloadSheet) {
        LosslessDownloadSheet(
            title = song.title,
            subtitle = song.artists.joinToString(", ") { it.name },
            trackCount = 1,
            onDismiss = { showLosslessDownloadSheet = false },
            enableYouTubeDownload = true,
            onStartYouTube = {
                showLosslessDownloadSheet = false
                val req = DownloadRequest.Builder(song.id, song.id.toUri())
                    .setCustomCacheKey(song.id)
                    .setData(song.title.toByteArray())
                    .build()
                DownloadService.sendAddDownload(
                    context,
                    ExoDownloadService::class.java,
                    req,
                    false,
                )
                onDismiss()
            },
            onStart = { provider, quality ->
                showLosslessDownloadSheet = false
                Toast.makeText(context, context.getString(R.string.starting_lossless_download), Toast.LENGTH_SHORT).show()
                App.launchOnAppScope {
                    database.transaction {
                        insert(song.toMediaMetadata())
                    }
                    when (
                        val result =
                            SpotiFlacDownloader.downloadSongItem(
                                context = context.applicationContext,
                                database = database,
                                song = song,
                                provider = provider,
                                quality = quality,
                            )
                    ) {
                        is SpotiFlacDownloader.Result.Success ->
                            Toast.makeText(context, context.getString(R.string.flac_downloaded_tracks, 1, 1), Toast.LENGTH_LONG).show()
                        SpotiFlacDownloader.Result.BackendMissing ->
                            Toast
                                .makeText(
                                    context,
                                    context.getString(
                                        SpotiFlacHandoff.openForTrack(
                                            context = context.applicationContext,
                                            title = song.title,
                                            artists = song.artists.map { it.name },
                                        ),
                                    ),
                                    Toast.LENGTH_LONG,
                                ).show()
                        is SpotiFlacDownloader.Result.Failed ->
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    }
                }
            },
        )
    }

    // Split artists by configured separators
    data class SplitArtist(
        val name: String,
        val originalArtist: MediaMetadata.Artist?
    )

    val splitArtists = remember(artists, artistSeparators) {
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

    AddToPlaylistDialog(  
        isVisible = showChoosePlaylistDialog,  
        onGetSong = {  
            database.transaction {  
                insert(song.toMediaMetadata())  
            }  
            listOf(song.id)  
        },  
        onDismiss = { showChoosePlaylistDialog = false },
        onAddComplete = { _, playlistNames ->
            val message = when {
                playlistNames.size == 1 -> context.getString(R.string.added_to_playlist, playlistNames.first())
                else -> context.getString(R.string.added_to_n_playlists, playlistNames.size)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
    )

    var showSelectArtistDialog by rememberSaveable {  
        mutableStateOf(false)  
    }  

    if (showSelectArtistDialog) {  
        ListDialog(  
            onDismiss = { showSelectArtistDialog = false },  
        ) {  
            items(splitArtists.distinctBy { it.name }) { splitArtist ->  
                Row(  
                    verticalAlignment = Alignment.CenterVertically,  
                    modifier =  
                    Modifier  
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
                        modifier =  
                        Modifier  
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

    ListItem(  
        headlineContent = {
            Text(
                text = song.title,
                modifier = Modifier.basicMarquee(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },  
        supportingContent = {  
            Text(  
                text = joinByBullet(
                    song.artists.joinToString { it.name },
                    song.duration?.let { makeTimeString(it * 1000L) },
                )
            )  
        },  
        leadingContent = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(ListThumbnailSize)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
            ) {
                AsyncImage(
                    model = song.thumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                )
            }
        },
        trailingContent = {  
            IconButton(  
                onClick = {  
                    database.transaction {  
                        librarySong.let { librarySong ->  
                            val s: SongEntity  
                            if (librarySong == null) {  
                                insert(song.toMediaMetadata(), SongEntity::toggleLike)  
                                s = song.toMediaMetadata().toSongEntity().let(SongEntity::toggleLike)  
                            } else {  
                                s = librarySong.song.toggleLike()  
                                update(s)  
                            }  
                            syncUtils.likeSong(s)  
                        }  
                    }  
                },  
            ) {  
                Icon(  
                    painter = painterResource(if (librarySong?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border),  
                    tint = if (librarySong?.song?.liked == true) MaterialTheme.colorScheme.error else LocalContentColor.current,  
                    contentDescription = null,  
                )  
            }  
        },  
    )  

    HorizontalDivider()

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
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            // Row for "Play next", "Add to playlist", and "Share" buttons with grid-like background
            // Enhanced Action Grid using NewMenuComponents
            NewActionGrid(
                actions = listOf(
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.playlist_play),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.play_next),
                        onClick = {
                            playerConnection.playNext(song.toMediaItem())
                            onDismiss()
                        }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.playlist_add),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.add_to_playlist),
                        onClick = {
                            showChoosePlaylistDialog = true
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
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, song.shareLink)
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                            onDismiss()
                        }
                    )
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
            )
        }
        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.start_radio)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.radio),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
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
                    playerConnection.addToQueue(song.toMediaItem())
                    onDismiss()
                }
            )
        }
        item {
            ListItem(
                headlineContent = { 
                    Text(text = if (librarySong?.song?.inLibrary != null) stringResource(R.string.remove_from_library) else stringResource(R.string.add_to_library))
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(if (librarySong?.song?.inLibrary != null) R.drawable.library_add_check else R.drawable.library_add),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    if (librarySong?.song?.inLibrary != null) {
                        database.query {
                            inLibrary(song.id, null)
                        }
                    } else {
                        database.transaction {
                            insert(song.toMediaMetadata())
                            inLibrary(song.id, LocalDateTime.now())
                        }
                    }
                }
            )
        }
        item {
            when (download?.state) {
                Download.STATE_COMPLETED -> {
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
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false,
                            )
                        }
                    )
                }
                Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.downloading)) },
                        leadingContent = {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        modifier = Modifier.clickable {
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
        if (splitArtists.isNotEmpty()) {
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
        song.album?.let { album ->
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.view_album)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.album),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        navController.navigate("album/${album.id}")
                        onDismiss()
                    }
                )
            }
        }
        item {
             ListItem(
                 headlineContent = { Text(text = stringResource(R.string.details)) },
                 leadingContent = {
                     Icon(
                         painter = painterResource(R.drawable.info),
                         contentDescription = null,
                     )
                 },
                 modifier = Modifier.clickable {
                      onDismiss()
                      bottomSheetPageState.show {
                          ShowMediaInfo(song.id)
                      }
                 }
             )
        }
        if (externalDownloaderEnabled) {
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.open_with_downloader)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.download),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        onDismiss()
                        val url = "https://music.youtube.com/watch?v=${song.id}"
                        if (externalDownloaderPackage.isBlank()) {
                            Toast.makeText(context, context.getString(R.string.external_downloader_not_configured), Toast.LENGTH_LONG).show()
                            return@clickable
                        }
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setPackage(externalDownloaderPackage)
                            data = android.net.Uri.parse(url)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
    }
}

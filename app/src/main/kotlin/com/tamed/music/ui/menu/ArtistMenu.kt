/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.ui.menu

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.tamed.music.LocalDatabase
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.R
import com.tamed.music.constants.ArtistSongSortType
import com.tamed.music.db.entities.Artist
import com.tamed.music.extensions.toMediaItem
import com.tamed.music.playback.queues.ListQueue
import com.tamed.music.ui.component.AppleMenuItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ArtistMenu(
    originalArtist: Artist,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val artistState = database.artist(originalArtist.id).collectAsState(initial = originalArtist)
    val artist = artistState.value ?: originalArtist

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        if (!artist.artist.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = artist.artist.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.artist),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.artist.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = if (artist.songCount > 0) {
                pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount)
            } else {
                stringResource(R.string.artist)
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(
            onClick = {
                database.transaction {
                    update(artist.artist.toggleLike())
                }
            },
        ) {
            val isBookmarked = artist.artist.bookmarkedAt != null
            Icon(
                painter = painterResource(if (isBookmarked) R.drawable.favorite else R.drawable.favorite_border),
                tint = if (isBookmarked) MaterialTheme.colorScheme.error else LocalContentColor.current,
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

    LazyColumn(
        userScrollEnabled = !isPortrait,
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        if (artist.songCount > 0) {
            item {
                AppleMenuItem(
                    title = stringResource(R.string.play),
                    icon = R.drawable.play,
                    onClick = {
                        coroutineScope.launch {
                            val songs = withContext(Dispatchers.IO) {
                                database
                                    .artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                    .first()
                                    .map { it.toMediaItem() }
                            }
                            playerConnection.playQueue(
                                ListQueue(
                                    title = artist.artist.name,
                                    items = songs,
                                ),
                            )
                        }
                        onDismiss()
                    },
                )
            }

            item {
                AppleMenuItem(
                    title = stringResource(R.string.shuffle),
                    icon = R.drawable.shuffle,
                    onClick = {
                        coroutineScope.launch {
                            val songs = withContext(Dispatchers.IO) {
                                database
                                    .artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                    .first()
                                    .map { it.toMediaItem() }
                                    .shuffled()
                            }
                            playerConnection.playQueue(
                                ListQueue(
                                    title = artist.artist.name,
                                    items = songs,
                                ),
                            )
                        }
                        onDismiss()
                    },
                )
            }
        }

        if (artist.artist.isYouTubeArtist) {
            item {
                AppleMenuItem(
                    title = stringResource(R.string.share),
                    icon = R.drawable.share,
                    onClick = {
                        onDismiss()
                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "https://music.youtube.com/channel/${artist.id}"
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    },
                )
            }
        }

        item {
            val isSubscribed = artist.artist.bookmarkedAt != null
            AppleMenuItem(
                title = stringResource(if (isSubscribed) R.string.subscribed else R.string.subscribe),
                icon = if (isSubscribed) R.drawable.subscribed else R.drawable.subscribe,
                onClick = {
                    database.transaction {
                        update(artist.artist.toggleLike())
                    }
                },
            )
        }
    }
}

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.tamed.music.LocalDatabase
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.R
import com.tamed.music.db.entities.ArtistEntity
import com.tamed.music.innertube.models.ArtistItem
import com.tamed.music.playback.queues.YouTubeQueue
import com.tamed.music.ui.component.AppleMenuItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeArtistMenu(
    artist: ArtistItem,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val libraryArtist by database.artist(artist.id).collectAsState(initial = null)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        if (!artist.thumbnail.isNullOrBlank()) {
            AsyncImage(
                model = artist.thumbnail,
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
                text = artist.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.artist),
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
                database.query {
                    val libArtist = libraryArtist
                    if (libArtist != null) {
                        update(libArtist.artist.toggleLike())
                    } else {
                        insert(
                            ArtistEntity(
                                id = artist.id,
                                name = artist.title,
                                channelId = artist.channelId,
                                thumbnailUrl = artist.thumbnail,
                            ).toggleLike()
                        )
                    }
                }
            },
        ) {
            val isBookmarked = libraryArtist?.artist?.bookmarkedAt != null
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
        artist.radioEndpoint?.let { watchEndpoint ->
            item {
                AppleMenuItem(
                    title = stringResource(R.string.start_radio),
                    icon = R.drawable.radio,
                    onClick = {
                        playerConnection.playQueue(YouTubeQueue(watchEndpoint))
                        onDismiss()
                    },
                )
            }
        }

        artist.shuffleEndpoint?.let { watchEndpoint ->
            item {
                AppleMenuItem(
                    title = stringResource(R.string.shuffle),
                    icon = R.drawable.shuffle,
                    onClick = {
                        playerConnection.playQueue(YouTubeQueue(watchEndpoint))
                        onDismiss()
                    },
                )
            }
        }

        item {
            AppleMenuItem(
                title = stringResource(R.string.share),
                icon = R.drawable.share,
                onClick = {
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, artist.shareLink)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                    onDismiss()
                },
            )
        }

        item {
            val isSubscribed = libraryArtist?.artist?.bookmarkedAt != null
            AppleMenuItem(
                title = stringResource(if (isSubscribed) R.string.subscribed else R.string.subscribe),
                icon = if (isSubscribed) R.drawable.subscribed else R.drawable.subscribe,
                onClick = {
                    database.query {
                        val libArtist = libraryArtist
                        if (libArtist != null) {
                            update(libArtist.artist.toggleLike())
                        } else {
                            insert(
                                ArtistEntity(
                                    id = artist.id,
                                    name = artist.title,
                                    channelId = artist.channelId,
                                    thumbnailUrl = artist.thumbnail,
                                ).toggleLike()
                            )
                        }
                    }
                },
            )
        }
    }
}

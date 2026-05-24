/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.R
import com.tamed.music.LocalDatabase
import com.tamed.music.constants.ChipSortTypeKey
import com.tamed.music.constants.LibraryFilter
import com.tamed.music.ui.component.CreatePlaylistDialog
import com.tamed.music.ui.component.GlassIconCircleButton
import com.tamed.music.ui.component.LibraryEntryRow
import com.tamed.music.ui.component.MediaCard
import com.tamed.music.ui.theme.TamedAppleTypography
import com.tamed.music.ui.theme.appleBackgroundColor
import com.tamed.music.utils.rememberEnumPreference
import com.tamed.music.viewmodels.LibraryMixViewModel

@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryMixViewModel = hiltViewModel(),
) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)

    when (filterType) {
        LibraryFilter.LIBRARY -> LibraryOverviewScreen(
            navController = navController,
            onNavigate = { filterType = it },
            viewModel = viewModel,
        )
        LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(
            navController = navController,
            filterContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
                    FilterChip(
                        label = { Text("Playlists") },
                        selected = true,
                        onClick = { filterType = LibraryFilter.LIBRARY },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                        leadingIcon = {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(R.drawable.close),
                                contentDescription = null,
                            )
                        },
                    )
                }
            },
        )
        LibraryFilter.SONGS -> LibrarySongsScreen(
            navController = navController,
            onDeselect = { filterType = LibraryFilter.LIBRARY },
        )
        LibraryFilter.ALBUMS -> LibraryAlbumsScreen(
            navController = navController,
            onDeselect = { filterType = LibraryFilter.LIBRARY },
        )
        LibraryFilter.ARTISTS -> LibraryArtistsScreen(
            navController = navController,
            onDeselect = { filterType = LibraryFilter.LIBRARY },
        )
    }
}

@Composable
private fun LibraryOverviewScreen(
    navController: NavController,
    onNavigate: (LibraryFilter) -> Unit,
    viewModel: LibraryMixViewModel,
) {
    val albums by viewModel.albums.collectAsState()
    val playerInsets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(onDismiss = { showCreatePlaylistDialog = false })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appleBackgroundColor()),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = playerInsets.calculateTopPadding() + 18.dp,
                bottom = playerInsets.calculateBottomPadding() + 140.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).padding(end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "Library",
                                style = TamedAppleTypography.largeTitle(),
                            )
                            Text(
                                text = "Your playlists, artists, albums, and songs in one airy view.",
                                style = TamedAppleTypography.cardSubtitle(),
                            )
                        }
                        Row(
                            modifier = Modifier.padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            GlassIconCircleButton(
                                iconRes = R.drawable.add,
                                contentDescription = "Create playlist",
                                onClick = { showCreatePlaylistDialog = true },
                            )
                            GlassIconCircleButton(
                                iconRes = R.drawable.settings,
                                contentDescription = "Settings",
                                onClick = { navController.navigate("settings") },
                            )
                        }
                    }

                    Column {
                        LibraryEntryRow(
                            title = "Playlists",
                            iconRes = R.drawable.queue_music,
                            onClick = { onNavigate(LibraryFilter.PLAYLISTS) },
                        )
                        LibraryEntryRow(
                            title = "Artists",
                            iconRes = R.drawable.person,
                            onClick = { onNavigate(LibraryFilter.ARTISTS) },
                        )
                        LibraryEntryRow(
                            title = "Albums",
                            iconRes = R.drawable.album,
                            onClick = { onNavigate(LibraryFilter.ALBUMS) },
                        )
                        LibraryEntryRow(
                            title = "Songs",
                            iconRes = R.drawable.music_note,
                            onClick = { onNavigate(LibraryFilter.SONGS) },
                        )
                    }

                    Text(
                        text = "Recently Added",
                        style = TamedAppleTypography.sectionTitle(),
                    )
                }
            }

            items(albums.take(8), key = { it.id }) { album ->
                val hasFlac = album.title.endsWith(" (FLAC)", ignoreCase = true)
                val displayTitle = if (hasFlac) album.title.substringBeforeLast(" (FLAC)") else album.title
                
                val albumWithSongs by LocalDatabase.current.albumWithSongs(album.id).collectAsState(initial = null)
                val displayArtists = album.artists.ifEmpty {
                    albumWithSongs?.songs?.flatMap { it.artists }?.distinctBy { it.id }.orEmpty()
                }
                
                MediaCard(
                    title = displayTitle,
                    subtitle = displayArtists.joinToString { it.name }.ifBlank { "Unknown Artist" },
                    imageUrl = album.thumbnailUrl,
                    onClick = { navController.navigate("album/${album.id}") },
                    metadata = album.album.year?.toString(),
                    badge = if (hasFlac) stringResource(R.string.lossless_badge) else null,
                    square = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

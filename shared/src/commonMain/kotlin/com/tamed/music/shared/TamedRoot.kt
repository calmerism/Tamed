package com.tamed.music.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TamedBackgroundTop = Color(0xFF102826)
private val TamedBackgroundBottom = Color(0xFFF0E2C8)
private val TamedPanel = Color(0xFFF8F1E3)
private val TamedPanelStrong = Color(0xFF1C3B37)
private val TamedPanelDark = Color(0xFF294D48)
private val TamedAccent = Color(0xFFD7672C)
private val TamedAccentSoft = Color(0xFFF7B28D)
private val TamedSuccess = Color(0xFF2E7D5B)
private val TamedText = Color(0xFF1C2321)
private val TamedSubtle = Color(0xFF5A625D)

@Composable
fun TamedRoot() {
    val controller = remember { TamedShellController() }
    val snapshot = controller.snapshot
    val scope = rememberCoroutineScope()

    LaunchedEffect(controller.activeHomeChipKey()) {
        controller.refreshLiveHome()
    }

    LaunchedEffect(controller.searchQuery) {
        delay(350)
        controller.refreshLiveSearch()
        controller.refreshLyricsProbe()
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(TamedBackgroundTop, TamedBackgroundBottom),
                        ),
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AppHeader(snapshot)
                    when (controller.activeDetailSurface) {
                        TamedDetailSurface.ALBUM -> AlbumDetailTab(
                            state = controller.albumDetail,
                            onBack = controller::closeDetail,
                            onOpenTarget = { target ->
                                scope.launch {
                                    controller.openTarget(target)
                                }
                            },
                        )
                        TamedDetailSurface.ARTIST -> ArtistDetailTab(
                            state = controller.artistDetail,
                            onBack = controller::closeDetail,
                            onOpenTarget = { target ->
                                scope.launch {
                                    controller.openTarget(target)
                                }
                            },
                        )
                        TamedDetailSurface.PLAYLIST -> PlaylistDetailTab(
                            state = controller.playlistDetail,
                            onBack = controller::closeDetail,
                            onOpenTarget = { target ->
                                scope.launch {
                                    controller.openTarget(target)
                                }
                            },
                        )
                        TamedDetailSurface.NONE -> {
                        TabRow(selectedTab = controller.selectedTab, onSelected = controller::selectTab)

                        when (controller.selectedTab) {
                            TamedTab.HOME -> HomeTab(
                                controller = controller,
                                snapshot = snapshot,
                                onOpenPlayer = controller::openPlayer,
                                onOpenSearch = controller::openSearch,
                                onOpenAlbum = { browseId ->
                                    scope.launch {
                                        controller.openAlbumDetail(browseId)
                                    }
                                },
                                onOpenTarget = { target ->
                                    scope.launch {
                                        controller.openTarget(target)
                                    }
                                },
                            )
                            TamedTab.SEARCH -> SearchTab(
                                controller = controller,
                                onOpenTarget = { target ->
                                    scope.launch {
                                        controller.openTarget(target)
                                    }
                                },
                            )
                            TamedTab.LIBRARY -> LibraryTab(snapshot)
                            TamedTab.PLAYER -> PlayerTab(snapshot)
                        }
                    }
                    }

                    MiniPlayer(snapshot.nowPlaying, onOpen = controller::openPlayer)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun AppHeader(snapshot: TamedAppSnapshot) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TamedPanelStrong),
        shape = RoundedCornerShape(30.dp),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(TamedAccent)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = snapshot.platform,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "Client ${snapshot.preferredClientName}",
                    color = TamedAccentSoft,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                text = snapshot.appName,
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "A real shared app shell for iPhone: home, search, library, and player surfaces already wired into the exported Kotlin framework.",
                color = Color(0xFFF7EEDF),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun TabRow(selectedTab: TamedTab, onSelected: (TamedTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        listOf(
            TamedTab.HOME to "Home",
            TamedTab.SEARCH to "Search",
            TamedTab.LIBRARY to "Library",
            TamedTab.PLAYER to "Player",
        ).forEach { (tab, title) ->
            val selected = tab == selectedTab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) TamedAccent else Color.White.copy(alpha = 0.35f))
                    .clickable { onSelected(tab) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = title,
                    color = if (selected) Color.White else TamedText,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun HomeTab(
    controller: TamedShellController,
    snapshot: TamedAppSnapshot,
    onOpenPlayer: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenTarget: (TamedNavTarget) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        QuickActions(onOpenPlayer = onOpenPlayer, onOpenSearch = onOpenSearch)
        LiveHomeCard(
            state = controller.liveHome,
            onSelectChip = controller::selectHomeChip,
            onOpenAlbum = onOpenAlbum,
            onOpenTarget = onOpenTarget,
        )
        SectionTitle("For you")
        snapshot.mixes.forEach { mix ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(mix.accentHex).copy(alpha = 0.92f)),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = mix.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = mix.caption,
                        color = Color.White.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        SectionTitle("Recent")
        snapshot.recentTracks.forEach { track ->
            TrackRow(track)
        }
    }
}

@Composable
private fun SearchTab(
    controller: TamedShellController,
    onOpenTarget: (TamedNavTarget) -> Unit,
) {
    val snapshot = controller.snapshot
    val normalizedQuery = controller.searchQuery.trim().lowercase()
    val matchedTracks = controller.filteredTracks()
    val matchedLibrary = controller.filteredLibraryTracks()
    val matchedModules = controller.filteredModuleTracks()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = TamedPanel),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Search is wired for the shared providers",
                    color = TamedText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    value = controller.searchQuery,
                    onValueChange = controller::updateSearchQuery,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search tracks, library, or shared modules") },
                    singleLine = true,
                )
                Text(
                    text = "Local filtering already works here, and the lyrics probe below hits the shared LrcLib network module when you search in the format `Artist - Song`.",
                    color = TamedSubtle,
                )
            }
        }

        LyricsProbeCard(controller.lyricsProbe)
        LiveSearchCard(
            state = controller.liveSearch,
            onSelectSuggestion = controller::applySearchSuggestion,
            onOpenTarget = onOpenTarget,
        )

        if (normalizedQuery.isBlank()) {
            snapshot.searchClusters.forEach { cluster ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = cluster.title,
                            color = TamedText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            cluster.items.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(TamedPanel)
                                        .border(1.dp, TamedAccent.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                ) {
                                    Text(text = item, color = TamedText, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            SearchResultsCard(
                title = "Tracks",
                emptyLabel = "No track matches yet",
                items = matchedTracks,
            )
            SearchResultsCard(
                title = "Library",
                emptyLabel = "No library matches yet",
                items = matchedLibrary,
            )
            SearchResultsCard(
                title = "Shared modules",
                emptyLabel = "No shared-module matches yet",
                items = matchedModules,
            )
        }
    }
}

@Composable
private fun LyricsProbeCard(state: TamedLyricsProbeState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                state.isLoading -> TamedPanel
                state.isError -> Color(0xFFFFEFE7)
                state.preview != null -> Color.White.copy(alpha = 0.9f)
                else -> TamedPanel
            },
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = state.title,
                color = TamedText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = state.message,
                color = TamedSubtle,
                style = MaterialTheme.typography.bodyMedium,
            )
            state.preview?.let { preview ->
                Text(
                    text = preview,
                    color = TamedAccent,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveHomeCard(
    state: TamedLiveHomeState,
    onSelectChip: (TamedHomeChip?) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenTarget: (TamedNavTarget) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                state.isError -> Color(0xFFFFEFE7)
                state.shelves.isNotEmpty() -> Color.White.copy(alpha = 0.9f)
                else -> TamedPanel
            },
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = state.title,
                color = TamedText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = state.message,
                color = TamedSubtle,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (state.chips.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.chips.forEach { chip ->
                        SuggestionChip(
                            text = chip.title,
                            selected = state.selectedChipTitle == chip.title,
                            onClick = { onSelectChip(chip) },
                        )
                    }
                }
            }
            if (state.newReleases.isNotEmpty()) {
                Text(
                    text = "New releases",
                    color = TamedText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                state.newReleases.forEach { album ->
                    AlbumCard(
                        album = album,
                        onClick = { onOpenAlbum(album.browseId) },
                    )
                }
            }
            state.shelves.forEach { shelf ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = shelf.title,
                        color = TamedText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    shelf.subtitle?.let { subtitle ->
                        Text(
                            text = subtitle,
                            color = TamedSubtle,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    shelf.tracks.forEach { track ->
                        TrackRow(track, onOpenTarget = onOpenTarget)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(
    album: TamedAlbumCard,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = album.title,
                    color = TamedText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = album.subtitle,
                    color = TamedSubtle,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = album.detail,
                color = TamedAccent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveSearchCard(
    state: TamedLiveSearchState,
    onSelectSuggestion: (String) -> Unit,
    onOpenTarget: (TamedNavTarget) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                state.isLoading -> TamedPanel
                state.isError -> Color(0xFFFFEFE7)
                state.tracks.isNotEmpty() -> Color.White.copy(alpha = 0.9f)
                else -> TamedPanel
            },
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = state.title,
                color = TamedText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = state.message,
                color = TamedSubtle,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (state.suggestionQueries.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.suggestionQueries.forEach { suggestion ->
                        SuggestionChip(
                            text = suggestion,
                            selected = false,
                            onClick = { onSelectSuggestion(suggestion) },
                        )
                    }
                }
            }
            if (state.suggestionTracks.isNotEmpty()) {
                Text(
                    text = "Suggested",
                    color = TamedText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                state.suggestionTracks.forEach { track ->
                    TrackRow(track, onOpenTarget = onOpenTarget)
                }
            }
            state.sections.forEach { section ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = section.title,
                        color = TamedText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    section.tracks.forEach { track ->
                        TrackRow(track, onOpenTarget = onOpenTarget)
                    }
                }
            }
            state.tracks.forEach { track ->
                TrackRow(track, onOpenTarget = onOpenTarget)
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) TamedAccent else TamedPanel)
            .border(
                width = 1.dp,
                color = if (selected) TamedAccent else TamedAccent.copy(alpha = 0.25f),
                shape = RoundedCornerShape(18.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else TamedText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AlbumDetailTab(
    state: TamedAlbumDetailState,
    onBack: () -> Unit,
    onOpenTarget: (TamedNavTarget) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SuggestionChip(
                text = "Back",
                selected = true,
                onClick = onBack,
            )
            Text(
                text = state.title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(58.dp))
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (state.isError) Color(0xFFFFEFE7) else Color.White.copy(alpha = 0.9f),
            ),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.album?.let { album ->
                    Text(
                        text = album.title,
                        color = TamedText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${album.subtitle} · ${album.detail}",
                        color = TamedSubtle,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = state.message,
                    color = TamedSubtle,
                    style = MaterialTheme.typography.bodyMedium,
                )
                state.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        color = TamedText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        if (state.songs.isNotEmpty()) {
            SectionTitle("Tracks")
            state.songs.forEach { track ->
                TrackRow(track, onOpenTarget = onOpenTarget)
            }
        }
    }
}

@Composable
private fun ArtistDetailTab(
    state: TamedArtistDetailState,
    onBack: () -> Unit,
    onOpenTarget: (TamedNavTarget) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        DetailHeader(
            title = state.title,
            onBack = onBack,
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (state.isError) Color(0xFFFFEFE7) else Color.White.copy(alpha = 0.9f),
            ),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.artistName?.let { artistName ->
                    Text(
                        text = artistName,
                        color = TamedText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                state.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        color = TamedSubtle,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = state.message,
                    color = TamedSubtle,
                    style = MaterialTheme.typography.bodyMedium,
                )
                state.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        color = TamedText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        state.sections.forEach { section ->
            SectionTitle(section.title)
            section.tracks.forEach { track ->
                TrackRow(track, onOpenTarget = onOpenTarget)
            }
        }
    }
}

@Composable
private fun PlaylistDetailTab(
    state: TamedPlaylistDetailState,
    onBack: () -> Unit,
    onOpenTarget: (TamedNavTarget) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        DetailHeader(
            title = state.title,
            onBack = onBack,
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (state.isError) Color(0xFFFFEFE7) else Color.White.copy(alpha = 0.9f),
            ),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.playlistName?.let { playlistName ->
                    Text(
                        text = playlistName,
                        color = TamedText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                state.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                    Text(
                        text = subtitle,
                        color = TamedSubtle,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = state.message,
                    color = TamedSubtle,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (state.songs.isNotEmpty()) {
            SectionTitle("Tracks")
            state.songs.forEach { track ->
                TrackRow(track, onOpenTarget = onOpenTarget)
            }
        }
    }
}

@Composable
private fun DetailHeader(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SuggestionChip(
            text = "Back",
            selected = true,
            onClick = onBack,
        )
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(58.dp))
    }
}

@Composable
private fun QuickActions(
    onOpenPlayer: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ActionCard(
            title = "Resume",
            subtitle = "Jump back into the player shell",
            accent = TamedAccent,
            onClick = onOpenPlayer,
        )
        ActionCard(
            title = "Find",
            subtitle = "Open the new shared search surface",
            accent = TamedSuccess,
            onClick = onOpenSearch,
        )
    }
}

@Composable
private fun RowScope.ActionCard(
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accent)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = subtitle,
                color = TamedText,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SearchResultsCard(
    title: String,
    emptyLabel: String,
    items: List<TamedTrack>,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                color = TamedText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (items.isEmpty()) {
                Text(
                    text = emptyLabel,
                    color = TamedSubtle,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                items.forEach { item -> TrackRow(item) }
            }
        }
    }
}

@Composable
private fun LibraryTab(snapshot: TamedAppSnapshot) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        snapshot.libraryItems.forEach { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
                shape = RoundedCornerShape(24.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = item.title,
                            color = TamedText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = item.subtitle,
                            color = TamedSubtle,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(TamedPanelDark)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = item.itemCount.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        StatusPanel(snapshot.features)
    }
}

@Composable
private fun PlayerTab(snapshot: TamedAppSnapshot) {
    val nowPlaying = snapshot.nowPlaying

    Card(
        colors = CardDefaults.cardColors(containerColor = TamedPanelStrong),
        shape = RoundedCornerShape(30.dp),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(TamedAccent, Color(0xFF7A4A2A), TamedPanelDark),
                        ),
                    ),
            )
            Text(
                text = nowPlaying.title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${nowPlaying.artist} • ${nowPlaying.album}",
                color = Color(0xFFF5DFC7),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = nowPlaying.progressLabel,
                color = TamedAccentSoft,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }

    SectionTitle("Queue")
    nowPlaying.queue.forEach { track ->
        TrackRow(track)
    }
}

@Composable
private fun MiniPlayer(nowPlaying: TamedNowPlaying, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TamedPanelDark),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpen() }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = nowPlaying.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = nowPlaying.artist,
                    color = Color(0xFFE8D5C5),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(TamedAccent)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Open",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: TamedTrack,
    onOpenTarget: ((TamedNavTarget) -> Unit)? = null,
) {
    val navTarget = track.navTarget
    val clickableModifier = if (navTarget != null && onOpenTarget != null) {
        Modifier.clickable { onOpenTarget(navTarget) }
    } else {
        Modifier
    }
    val canOpen = navTarget != null && onOpenTarget != null
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = track.title,
                    color = TamedText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = track.artist,
                    color = TamedSubtle,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = if (canOpen) "${track.detail}  >" else track.detail,
                color = TamedAccent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun StatusPanel(features: List<TamedFeature>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TamedPanel),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "iOS build status",
                color = TamedText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            features.forEach { feature ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(text = feature.title, color = TamedText, fontWeight = FontWeight.SemiBold)
                        Text(text = feature.subtitle, color = TamedSubtle, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (feature.readyForIos) TamedSuccess else TamedAccent)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = if (feature.readyForIos) "Ready" else "Pending",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 2.dp),
    )
}

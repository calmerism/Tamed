package com.tamed.music.shared

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tamed.music.innertube.PortableInnertube
import com.tamed.music.innertube.models.AlbumItem
import com.tamed.music.innertube.models.ArtistItem
import com.tamed.music.innertube.models.BrowseEndpoint
import com.tamed.music.innertube.models.PlaylistItem
import com.tamed.music.innertube.models.SearchSuggestions
import com.tamed.music.innertube.models.SongItem
import com.tamed.music.innertube.models.YTItem
import com.tamed.music.innertube.pages.AlbumPage
import com.tamed.music.innertube.pages.ArtistPage
import com.tamed.music.innertube.pages.HomePage
import com.tamed.music.innertube.pages.PlaylistPage
import com.tamed.music.innertube.pages.SearchSummaryPage
import com.tamed.music.lrclib.LrcLib

data class TamedLyricsProbeState(
    val title: String,
    val message: String,
    val preview: String? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
) {
    companion object {
        fun idle(): TamedLyricsProbeState = TamedLyricsProbeState(
            title = "Live lyrics probe",
            message = "Type a query like `Artist - Song` to probe the shared LrcLib service from the iOS shell.",
        )
    }
}

data class TamedLiveShelf(
    val title: String,
    val subtitle: String? = null,
    val tracks: List<TamedTrack>,
)

data class TamedHomeChip(
    val title: String,
    val params: String? = null,
)

data class TamedLiveHomeState(
    val title: String,
    val message: String,
    val chips: List<TamedHomeChip> = emptyList(),
    val newReleases: List<TamedAlbumCard> = emptyList(),
    val selectedChipTitle: String? = null,
    val shelves: List<TamedLiveShelf> = emptyList(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
) {
    companion object {
        fun idle(): TamedLiveHomeState = TamedLiveHomeState(
            title = "Live home feed",
            message = "The shared shell can now request real Home shelves through portable innertube.",
        )
    }
}

data class TamedLiveSearchState(
    val title: String,
    val message: String,
    val suggestionQueries: List<String> = emptyList(),
    val suggestionTracks: List<TamedTrack> = emptyList(),
    val sections: List<TamedLiveShelf> = emptyList(),
    val tracks: List<TamedTrack> = emptyList(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
) {
    companion object {
        fun idle(): TamedLiveSearchState = TamedLiveSearchState(
            title = "Live music search",
            message = "Search here to run a real shared innertube request from the app shell.",
        )
    }
}

data class TamedAlbumDetailState(
    val title: String,
    val message: String,
    val album: TamedAlbumCard? = null,
    val description: String? = null,
    val songs: List<TamedTrack> = emptyList(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
) {
    companion object {
        fun hidden(): TamedAlbumDetailState = TamedAlbumDetailState(
            title = "Album",
            message = "",
        )
    }
}

enum class TamedDetailSurface {
    NONE,
    ALBUM,
    ARTIST,
    PLAYLIST,
}

data class TamedArtistDetailState(
    val title: String,
    val message: String,
    val artistName: String? = null,
    val subtitle: String? = null,
    val description: String? = null,
    val sections: List<TamedLiveShelf> = emptyList(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
) {
    companion object {
        fun hidden(): TamedArtistDetailState = TamedArtistDetailState(
            title = "Artist",
            message = "",
        )
    }
}

data class TamedPlaylistDetailState(
    val title: String,
    val message: String,
    val playlistName: String? = null,
    val subtitle: String? = null,
    val songs: List<TamedTrack> = emptyList(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
) {
    companion object {
        fun hidden(): TamedPlaylistDetailState = TamedPlaylistDetailState(
            title = "Playlist",
            message = "",
        )
    }
}

class TamedShellController(
    val snapshot: TamedAppSnapshot = TamedAppModel.snapshot(),
    private val portableInnertube: PortableInnertube = PortableInnertube(),
) {
    var selectedTab by mutableStateOf(TamedTab.HOME)
        private set

    var searchQuery by mutableStateOf("")
        private set

    var lyricsProbe by mutableStateOf(TamedLyricsProbeState.idle())
        private set

    var liveHome by mutableStateOf(TamedLiveHomeState.idle())
        private set

    var liveSearch by mutableStateOf(TamedLiveSearchState.idle())
        private set

    var albumDetail by mutableStateOf(TamedAlbumDetailState.hidden())
        private set

    var artistDetail by mutableStateOf(TamedArtistDetailState.hidden())
        private set

    var playlistDetail by mutableStateOf(TamedPlaylistDetailState.hidden())
        private set

    var activeDetailSurface by mutableStateOf(TamedDetailSurface.NONE)
        private set

    private var activeHomeChip by mutableStateOf<TamedHomeChip?>(null)

    fun selectTab(tab: TamedTab) {
        selectedTab = tab
    }

    fun openPlayer() {
        selectedTab = TamedTab.PLAYER
    }

    fun openSearch() {
        selectedTab = TamedTab.SEARCH
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun applySearchSuggestion(query: String) {
        searchQuery = query
    }

    fun selectHomeChip(chip: TamedHomeChip?) {
        activeHomeChip = if (activeHomeChip?.title == chip?.title) null else chip
    }

    fun activeHomeChipKey(): String = activeHomeChip?.title.orEmpty()

    fun closeDetail() {
        activeDetailSurface = TamedDetailSurface.NONE
        albumDetail = TamedAlbumDetailState.hidden()
        artistDetail = TamedArtistDetailState.hidden()
        playlistDetail = TamedPlaylistDetailState.hidden()
    }

    fun filteredTracks(): List<TamedTrack> {
        val normalizedQuery = searchQuery.trim().lowercase()
        return snapshot.recentTracks.filter {
            normalizedQuery.isBlank() ||
                it.title.lowercase().contains(normalizedQuery) ||
                it.artist.lowercase().contains(normalizedQuery)
        }
    }

    fun filteredLibraryTracks(): List<TamedTrack> {
        val normalizedQuery = searchQuery.trim().lowercase()
        return snapshot.libraryItems
            .filter {
                normalizedQuery.isBlank() ||
                    it.title.lowercase().contains(normalizedQuery) ||
                    it.subtitle.lowercase().contains(normalizedQuery)
            }
            .map { item ->
                TamedTrack(
                    title = item.title,
                    artist = item.subtitle,
                    detail = "${item.itemCount} items",
                )
            }
    }

    fun filteredModuleTracks(): List<TamedTrack> {
        val normalizedQuery = searchQuery.trim().lowercase()
        return snapshot.sharedModules
            .filter { normalizedQuery.isBlank() || it.lowercase().contains(normalizedQuery) }
            .map { module ->
                TamedTrack(
                    title = module,
                    artist = "Portable service module",
                    detail = "Shared",
                )
            }
    }

    suspend fun refreshLiveHome() {
        liveHome = TamedLiveHomeState(
            title = "Live home feed",
            message = "Loading Home shelves from the shared innertube transport...",
            chips = liveHome.chips,
            newReleases = liveHome.newReleases,
            selectedChipTitle = activeHomeChip?.title,
            isLoading = true,
        )

        val result = portableInnertube.home(params = activeHomeChip?.params)
        val newReleases = portableInnertube.newReleaseAlbums().getOrDefault(emptyList())
        liveHome = result.fold(
            onSuccess = { homePage ->
                toLiveHomeState(homePage, activeHomeChip?.title, newReleases)
            },
            onFailure = { error ->
                TamedLiveHomeState(
                    title = "Live home feed",
                    message = error.message ?: "Unable to load Home shelves right now.",
                    chips = liveHome.chips,
                    newReleases = newReleases.map(AlbumItem::toAlbumCard),
                    selectedChipTitle = activeHomeChip?.title,
                    isError = true,
                )
            },
        )
    }

    suspend fun openAlbumDetail(browseId: String) {
        val cached = liveHome.newReleases.firstOrNull { it.browseId == browseId }
        activeDetailSurface = TamedDetailSurface.ALBUM
        albumDetail = TamedAlbumDetailState(
            title = cached?.title ?: "Album",
            message = "Loading album details from shared innertube...",
            album = cached,
            isLoading = true,
        )

        val result = portableInnertube.album(browseId)
        albumDetail = result.fold(
            onSuccess = ::toAlbumDetailState,
            onFailure = { error ->
                TamedAlbumDetailState(
                    title = cached?.title ?: "Album",
                    message = error.message ?: "Unable to load this album right now.",
                    album = cached,
                    isError = true,
                )
            },
        )
    }

    suspend fun openArtistDetail(browseId: String) {
        activeDetailSurface = TamedDetailSurface.ARTIST
        artistDetail = TamedArtistDetailState(
            title = "Artist",
            message = "Loading artist details from shared innertube...",
            artistName = liveSearch.tracks.firstOrNull { it.navTarget?.id == browseId }?.title,
            isLoading = true,
        )

        val result = portableInnertube.artist(browseId)
        artistDetail = result.fold(
            onSuccess = ::toArtistDetailState,
            onFailure = { error ->
                TamedArtistDetailState(
                    title = "Artist",
                    message = error.message ?: "Unable to load this artist right now.",
                    isError = true,
                )
            },
        )
    }

    suspend fun openPlaylistDetail(playlistId: String) {
        activeDetailSurface = TamedDetailSurface.PLAYLIST
        playlistDetail = TamedPlaylistDetailState(
            title = "Playlist",
            message = "Loading playlist details from shared innertube...",
            playlistName = liveSearch.tracks.firstOrNull { it.navTarget?.id == playlistId }?.title,
            isLoading = true,
        )

        val result = portableInnertube.playlist(playlistId)
        playlistDetail = result.fold(
            onSuccess = ::toPlaylistDetailState,
            onFailure = { error ->
                TamedPlaylistDetailState(
                    title = "Playlist",
                    message = error.message ?: "Unable to load this playlist right now.",
                    isError = true,
                )
            },
        )
    }

    suspend fun openTarget(target: TamedNavTarget) {
        when (target.kind) {
            TamedNavKind.ALBUM -> openAlbumDetail(target.id)
            TamedNavKind.ARTIST -> openArtistDetail(target.id)
            TamedNavKind.PLAYLIST -> openPlaylistDetail(target.id)
        }
    }

    suspend fun refreshLiveSearch() {
        if (searchQuery.isBlank()) {
            liveSearch = TamedLiveSearchState.idle()
            return
        }

        liveSearch = TamedLiveSearchState(
            title = "Live music search",
            message = "Searching YouTube Music through the shared innertube stack...",
            isLoading = true,
        )

        val query = searchQuery.trim()
        val searchResult = portableInnertube.search(query)
        val suggestionsResult = portableInnertube.searchSuggestions(query)
        val summaryResult = portableInnertube.searchSummary(query)

        liveSearch = buildLiveSearchState(
            query = query,
            searchResult = searchResult,
            suggestionsResult = suggestionsResult,
            summaryResult = summaryResult,
        )
    }

    suspend fun refreshLyricsProbe() {
        val parsed = parseArtistAndTitle(searchQuery)
        if (searchQuery.isBlank()) {
            lyricsProbe = TamedLyricsProbeState.idle()
            return
        }
        if (parsed == null) {
            lyricsProbe = TamedLyricsProbeState(
                title = "Live lyrics probe",
                message = "Use the format `Artist - Song` to run a real shared lyrics lookup.",
            )
            return
        }

        lyricsProbe = TamedLyricsProbeState(
            title = "Live lyrics probe",
            message = "Checking synced lyrics for ${parsed.artist} - ${parsed.title}...",
            isLoading = true,
        )

        val lyricsResult = LrcLib.getLyrics(
            title = parsed.title,
            artist = parsed.artist,
            duration = -1,
        )
        val matchesResult = LrcLib.lyrics(
            artist = parsed.artist,
            title = parsed.title,
        )

        val lyrics = lyricsResult.getOrNull()
        val matches = matchesResult.getOrDefault(emptyList())

        lyricsProbe = if (lyrics != null) {
            TamedLyricsProbeState(
                title = "Live lyrics probe",
                message = "Found ${matches.size} synced lyric match${if (matches.size == 1) "" else "es"} in LrcLib.",
                preview = lyrics.firstMeaningfulLyricLine(),
            )
        } else {
            TamedLyricsProbeState(
                title = "Live lyrics probe",
                message = "No synced lyrics found for ${parsed.artist} - ${parsed.title} yet.",
                isError = true,
            )
        }
    }

    private fun parseArtistAndTitle(query: String): ParsedLyricsQuery? {
        val delimiter = query.indexOf(" - ")
        if (delimiter == -1) return null

        val artist = query.substring(0, delimiter).trim()
        val title = query.substring(delimiter + 3).trim()
        if (artist.isBlank() || title.isBlank()) return null

        return ParsedLyricsQuery(artist = artist, title = title)
    }
}

private data class ParsedLyricsQuery(
    val artist: String,
    val title: String,
)

private fun String.firstMeaningfulLyricLine(): String {
    return lineSequence()
        .map { it.trim() }
        .map { line ->
            if (line.startsWith("[") && line.length > 10 && "]" in line) {
                line.substringAfter("]").trim()
            } else {
                line
            }
        }
        .firstOrNull { it.isNotBlank() }
        ?: "Synced lyrics loaded"
}

private fun toLiveHomeState(
    homePage: HomePage,
    selectedChipTitle: String?,
    newReleases: List<AlbumItem>,
): TamedLiveHomeState {
    val shelves = homePage.sections
        .mapNotNull { section ->
            val tracks = section.items.mapNotNull(YTItem::toShellTrack).take(6)
            tracks.takeIf { it.isNotEmpty() }?.let {
                TamedLiveShelf(
                    title = section.title,
                    subtitle = section.label,
                    tracks = it,
                )
            }
        }
        .take(3)

    return TamedLiveHomeState(
        title = "Live home feed",
        message = if (shelves.isEmpty()) {
            "The request completed, but there were no portable Home shelves to show yet."
        } else {
            "Loaded ${shelves.size} live Home ${if (shelves.size == 1) "shelf" else "shelves"} through shared innertube."
        },
        chips = homePage.chips.orEmpty().mapNotNull(HomePage.Chip::toShellChip),
        newReleases = newReleases.map(AlbumItem::toAlbumCard).take(6),
        selectedChipTitle = selectedChipTitle,
        shelves = shelves,
        isError = shelves.isEmpty(),
    )
}

private fun toAlbumDetailState(page: AlbumPage): TamedAlbumDetailState {
    return TamedAlbumDetailState(
        title = page.album.title,
        message = "Loaded ${page.songs.size} track${if (page.songs.size == 1) "" else "s"} from shared innertube.",
        album = page.album.toAlbumCard(),
        description = page.description,
        songs = page.songs.mapNotNull(YTItem::toShellTrack),
    )
}

private fun toArtistDetailState(page: ArtistPage): TamedArtistDetailState {
    val sections = page.sections
        .mapNotNull { section ->
            val tracks = section.items.mapNotNull(YTItem::toShellTrack).take(6)
            tracks.takeIf { it.isNotEmpty() }?.let {
                TamedLiveShelf(
                    title = section.title,
                    tracks = it,
                )
            }
        }
        .take(4)

    return TamedArtistDetailState(
        title = page.artist.title,
        message = if (sections.isEmpty()) {
            "Loaded the artist profile, but there were no portable sections to show yet."
        } else {
            "Loaded ${sections.size} artist ${if (sections.size == 1) "section" else "sections"} through shared innertube."
        },
        artistName = page.artist.title,
        subtitle = "Artist profile",
        description = page.description,
        sections = sections,
        isError = sections.isEmpty(),
    )
}

private fun toPlaylistDetailState(page: PlaylistPage): TamedPlaylistDetailState {
    val songs = page.songs.mapNotNull(YTItem::toShellTrack)
    return TamedPlaylistDetailState(
        title = page.playlist.title,
        message = if (songs.isEmpty()) {
            "Loaded the playlist header, but there were no portable tracks to show yet."
        } else {
            "Loaded ${songs.size} playlist track${if (songs.size == 1) "" else "s"} through shared innertube."
        },
        playlistName = page.playlist.title,
        subtitle = listOfNotNull(page.playlist.author?.name, page.playlist.songCountText).joinToString(" · "),
        songs = songs,
        isError = songs.isEmpty(),
    )
}

private fun buildLiveSearchState(
    query: String,
    searchResult: Result<com.tamed.music.innertube.pages.SearchResult>,
    suggestionsResult: Result<SearchSuggestions>,
    summaryResult: Result<SearchSummaryPage>,
): TamedLiveSearchState {
    val tracks = searchResult.getOrNull()?.items.orEmpty().mapNotNull(YTItem::toShellTrack).take(8)
    val suggestions = suggestionsResult.getOrNull()
    val sections = summaryResult.getOrNull()?.summaries.orEmpty()
        .mapNotNull { summary ->
            val summaryTracks = summary.items.mapNotNull(YTItem::toShellTrack).take(5)
            summaryTracks.takeIf { it.isNotEmpty() }?.let {
                TamedLiveShelf(
                    title = summary.title,
                    tracks = it,
                )
            }
        }
        .take(3)
    val suggestionTracks = suggestions?.recommendedItems.orEmpty().mapNotNull(YTItem::toShellTrack).take(4)
    val messages = listOfNotNull(
        searchResult.exceptionOrNull()?.message,
        suggestionsResult.exceptionOrNull()?.message,
        summaryResult.exceptionOrNull()?.message,
    )

    val hasLiveContent = tracks.isNotEmpty() || suggestionTracks.isNotEmpty() || sections.isNotEmpty() || suggestions?.queries?.isNotEmpty() == true

    return TamedLiveSearchState(
        title = "Live music search",
        message = when {
            hasLiveContent -> "Loaded live search content for $query from shared innertube."
            messages.isNotEmpty() -> messages.first()
            else -> "No shared innertube results came back for this query yet."
        },
        suggestionQueries = suggestions?.queries.orEmpty().take(6),
        suggestionTracks = suggestionTracks,
        sections = sections,
        tracks = tracks,
        isError = !hasLiveContent,
    )
}

private fun HomePage.Chip.toShellChip(): TamedHomeChip? {
    return TamedHomeChip(
        title = title,
        params = endpoint?.extractParams(),
    )
}

private fun BrowseEndpoint.extractParams(): String? {
    return params
}

private fun AlbumItem.toAlbumCard(): TamedAlbumCard {
    return TamedAlbumCard(
        browseId = browseId,
        title = title,
        subtitle = artists?.joinToString(separator = ", ") { it.name } ?: "Album",
        detail = year?.toString() ?: "Album",
    )
}

private fun YTItem.toShellTrack(): TamedTrack? = when (this) {
    is SongItem -> TamedTrack(
        title = title,
        artist = artists.joinToString(separator = ", ") { it.name },
        detail = buildString {
            append("Song")
            duration?.let {
                append(" · ")
                append(formatDuration(it))
            }
            if (explicit) {
                append(" · Explicit")
            }
        },
        navTarget = album?.id?.let { albumId ->
            TamedNavTarget(
                kind = TamedNavKind.ALBUM,
                id = albumId,
            )
        } ?: artists.firstOrNull()?.id?.let { artistId ->
            TamedNavTarget(
                kind = TamedNavKind.ARTIST,
                id = artistId,
            )
        },
    )
    is AlbumItem -> TamedTrack(
        title = title,
        artist = artists?.joinToString(separator = ", ") { it.name } ?: "Album",
        detail = year?.let { "Album · $it" } ?: "Album",
        navTarget = TamedNavTarget(
            kind = TamedNavKind.ALBUM,
            id = browseId,
        ),
    )
    is PlaylistItem -> TamedTrack(
        title = title,
        artist = author?.name ?: "Playlist",
        detail = songCountText ?: "Playlist",
        navTarget = TamedNavTarget(
            kind = TamedNavKind.PLAYLIST,
            id = id,
        ),
    )
    is ArtistItem -> TamedTrack(
        title = title,
        artist = "Artist profile",
        detail = "Artist",
        navTarget = TamedNavTarget(
            kind = TamedNavKind.ARTIST,
            id = id,
        ),
    )
}

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

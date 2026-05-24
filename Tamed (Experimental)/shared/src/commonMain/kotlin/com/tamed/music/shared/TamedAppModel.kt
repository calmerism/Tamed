package com.tamed.music.shared

import com.tamed.music.innertube.models.AlbumItem
import com.tamed.music.innertube.models.YouTubeClient

enum class TamedTab {
    HOME,
    SEARCH,
    LIBRARY,
    PLAYER,
}

data class TamedFeature(
    val title: String,
    val subtitle: String,
    val readyForIos: Boolean,
)

data class TamedMix(
    val title: String,
    val caption: String,
    val accentHex: Long,
)

data class TamedTrack(
    val title: String,
    val artist: String,
    val detail: String,
    val navTarget: TamedNavTarget? = null,
)

enum class TamedNavKind {
    ALBUM,
    ARTIST,
    PLAYLIST,
}

data class TamedNavTarget(
    val kind: TamedNavKind,
    val id: String,
)

data class TamedAlbumCard(
    val browseId: String,
    val title: String,
    val subtitle: String,
    val detail: String,
)

data class TamedLibraryItem(
    val title: String,
    val subtitle: String,
    val itemCount: Int,
)

data class TamedSearchCluster(
    val title: String,
    val items: List<String>,
)

data class TamedNowPlaying(
    val title: String,
    val artist: String,
    val album: String,
    val progressLabel: String,
    val queue: List<TamedTrack>,
)

data class TamedAppSnapshot(
    val appName: String,
    val platform: String,
    val preferredClientName: String,
    val sharedModules: List<String>,
    val features: List<TamedFeature>,
    val mixes: List<TamedMix>,
    val recentTracks: List<TamedTrack>,
    val libraryItems: List<TamedLibraryItem>,
    val searchClusters: List<TamedSearchCluster>,
    val nowPlaying: TamedNowPlaying,
)

object TamedAppModel {
    fun snapshot(): TamedAppSnapshot {
        return TamedAppSnapshot(
            appName = "Tamed",
            platform = platformName(),
            preferredClientName = YouTubeClient.IOS.clientName,
            sharedModules = listOf(
                "innertube",
                "lrclib",
                "lastfm",
                "kugou",
                "simpmusic",
                "betterlyrics",
                "canvas",
                "shazamkit",
            ),
            features = listOf(
                TamedFeature(
                    title = "Shared networking core",
                    subtitle = "Portable request specs, auth, parsing, and provider modules are already available to the iOS app shell.",
                    readyForIos = true,
                ),
                TamedFeature(
                    title = "Playback runtime",
                    subtitle = "The remaining gap is the native AVPlayer, download, and background-audio layer.",
                    readyForIos = false,
                ),
                TamedFeature(
                    title = "UI parity",
                    subtitle = "This shell now has real surfaces for home, search, library, and player, but exact Android parity is still ahead.",
                    readyForIos = false,
                ),
            ),
            mixes = listOf(
                TamedMix("Morning Current", "Canvas-ready chill, warm keys, lyric sync", 0xFFD7672C),
                TamedMix("Deep Commute", "Queue, offline, and scrobble-focused mix", 0xFF2F6B63),
                TamedMix("Signal Boost", "High-motion alt pop and neon hooks", 0xFF7F5033),
            ),
            recentTracks = listOf(
                TamedTrack("Glass Horizon", "North Signal", "4:12 · Lyrics synced"),
                TamedTrack("Static Bloom", "Aerial Youth", "3:47 · Canvas available"),
                TamedTrack("Night Transit", "Veil Avenue", "5:01 · From Deep Commute"),
                TamedTrack("Soft Engines", "Paper District", "2:58 · Downloaded"),
            ),
            libraryItems = listOf(
                TamedLibraryItem("Liked Songs", "Fast access mix of recent saves", 248),
                TamedLibraryItem("Offline Rotation", "Pinned for travel and bad signal", 63),
                TamedLibraryItem("Focus List", "Instrumental and low-vocal cuts", 42),
                TamedLibraryItem("Release Radar", "New albums and singles to check", 19),
            ),
            searchClusters = listOf(
                TamedSearchCluster("Browse", listOf("Charts", "Moods", "New releases", "Podcasts")),
                TamedSearchCluster("Quick find", listOf("Lyrics", "Artists", "Playlists", "Downloads")),
                TamedSearchCluster("Shared services", listOf("KuGou", "LrcLib", "Last.fm", "Shazam")),
            ),
            nowPlaying = TamedNowPlaying(
                title = "Glass Horizon",
                artist = "North Signal",
                album = "Drift Atlas",
                progressLabel = "1:48 / 4:12",
                queue = listOf(
                    TamedTrack("Static Bloom", "Aerial Youth", "Up next"),
                    TamedTrack("Night Transit", "Veil Avenue", "Queued"),
                    TamedTrack("Soft Engines", "Paper District", "Queued"),
                    TamedTrack("Heat Map", "Velvet Frame", "Radio"),
                ),
            ),
        )
    }
}

package com.tamed.music.sources

import com.tamed.music.innertube.YouTube
import com.tamed.music.innertube.models.SongItem
import java.util.Locale

class YouTubeSource(
    val config: SourceConfig,
) : MusicSource {

    override val configId: String get() = config.id
    override val kind: SourceKind get() = SourceKind.YOUTUBE
    override val displayName: String get() = config.label.ifBlank { SourceKind.YOUTUBE.label }

    override suspend fun health(): SourceHealth = SourceHealth.Ok()

    override suspend fun search(query: String, limit: Int, waitForAll: Boolean): List<Song> {
        val res = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
        return res?.items?.filterIsInstance<SongItem>()?.take(limit)?.map {
            Song(
                videoId = it.id,
                title = it.title,
                artist = it.artists.joinToString(", ") { a -> a.name },
                durationText = it.duration?.let { d -> "%d:%02d".format(Locale.ROOT, d / 60, d % 60) },
                thumbnailUrl = it.thumbnail
            )
        }.orEmpty()
    }

    override suspend fun stream(trackId: String, request: StreamRequest): SourceStream? {
        // Handled natively in MusicService/ResolvingDataSource
        return null
    }
}


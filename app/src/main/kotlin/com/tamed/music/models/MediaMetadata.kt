/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.models

import androidx.compose.runtime.Immutable
import com.tamed.music.innertube.models.SongItem
import com.tamed.music.db.entities.Song
import com.tamed.music.db.entities.SongEntity
import com.tamed.music.ui.utils.resize
import java.io.Serializable
import java.time.LocalDateTime

@Immutable
data class MediaMetadata(
    val id: String,
    val title: String,
    val artists: List<Artist>,
    val duration: Int,
    val thumbnailUrl: String? = null,
    val album: Album? = null,
    val setVideoId: String? = null,
    val explicit: Boolean = false,
    val liked: Boolean = false,
    val likedDate: LocalDateTime? = null,
    val inLibrary: LocalDateTime? = null,
    val sourceType: SourceType? = null,
    val sourceUri: String? = null,
    val sourceMimeType: String? = null,
    val sourceLabel: String? = null,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }

    enum class SourceType : Serializable {
        YOUTUBE,
        DIRECT,
        LOCAL,
    }

    data class Artist(
        val id: String?,
        val name: String,
        val thumbnailUrl: String? = null,
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    data class Album(
        val id: String,
        val title: String,
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    fun toSongEntity() =
        SongEntity(
            id = id,
            title = title,
            duration = duration,
            thumbnailUrl = thumbnailUrl,
            albumId = album?.id,
            albumName = album?.title,
            explicit = explicit,
            liked = liked,
            likedDate = likedDate,
            inLibrary = inLibrary,
            isLocal = resolvedSourceType() == SourceType.LOCAL,
            localPath = sourceUri,
            localMimeType = sourceMimeType,
        )

    fun resolvedSourceType(): SourceType = sourceType ?: SourceType.YOUTUBE

    fun playbackUri(): String = sourceUri ?: id

    fun isYouTubeSource(): Boolean = resolvedSourceType() == SourceType.YOUTUBE

    fun isLocalSource(): Boolean = resolvedSourceType() == SourceType.LOCAL

    fun isExternalSource(): Boolean = !isYouTubeSource()

    fun isLosslessSource(): Boolean = isLikelyLosslessAudio(sourceMimeType, sourceUri)
}

fun Song.toMediaMetadata() =
    MediaMetadata(
        id = song.id,
        title = song.title,
        artists =
        artists.map {
            MediaMetadata.Artist(
                id = it.id,
                name = it.name,
                thumbnailUrl = it.thumbnailUrl,
            )
        },
        duration = song.duration,
        thumbnailUrl = song.thumbnailUrl?.resize(1200, 1200),
        album =
        album?.let {
            MediaMetadata.Album(
                id = it.id,
                title = it.title,
            )
        } ?: song.albumId?.let { albumId ->
            MediaMetadata.Album(
                id = albumId,
                title = song.albumName.orEmpty(),
            )
        },
        sourceType = if (song.isLocal) MediaMetadata.SourceType.LOCAL else MediaMetadata.SourceType.YOUTUBE,
        sourceUri = if (song.isLocal) song.localPath ?: song.id else null,
        sourceMimeType = if (song.isLocal) song.localMimeType ?: inferLocalAudioMimeType(song.localPath ?: song.id) else null,
        sourceLabel =
        if (song.isLocal) {
            if (isLikelyLosslessAudio(song.localMimeType, song.localPath ?: song.id)) "Lossless local" else "Local file"
        } else {
            null
        },
        explicit = song.explicit,
    )

private fun inferLocalAudioMimeType(value: String): String? {
    val extension = value.substringBefore('?').substringAfterLast('.', "").lowercase()
    return when (extension) {
        "flac" -> "audio/flac"
        "wav", "wave" -> "audio/wav"
        "alac", "m4a", "m4b", "m4p", "mp4", "mp4a" -> "audio/mp4"
        "aac" -> "audio/aac"
        "mp3", "mp2", "mp1", "mpga" -> "audio/mpeg"
        "ogg", "oga" -> "audio/ogg"
        "opus" -> "audio/opus"
        "mka", "mkv" -> "audio/x-matroska"
        "aif", "aiff", "aifc" -> "audio/aiff"
        "wma" -> "audio/x-ms-wma"
        "ape" -> "audio/x-ape"
        "wv" -> "audio/x-wavpack"
        "dsf", "dff" -> "audio/x-dsd"
        "amr", "3gp", "3gpp" -> "audio/amr"
        else -> null
    }
}

fun isLikelyLosslessAudio(
    mimeType: String?,
    value: String?,
): Boolean {
    val normalizedMimeType = mimeType?.substringBefore(';')?.trim()?.lowercase()
    if (normalizedMimeType in setOf(
            "audio/flac", "audio/wav", "audio/x-wav", "audio/aiff", "audio/x-aiff",
            "audio/alac", "audio/x-ape", "audio/x-wavpack", "audio/x-dsd"
        )
    ) {
        return true
    }

    val extension = value?.substringBefore('?')?.substringAfterLast('.', "")?.lowercase().orEmpty()
    return extension in setOf("flac", "wav", "wave", "aif", "aiff", "aifc", "alac", "ape", "wv", "dsf", "dff")
}

fun SongItem.toMediaMetadata() =
    MediaMetadata(
        id = id,
        title = title,
        artists =
        artists.map {
            MediaMetadata.Artist(
                id = it.id,
                name = it.name,
                thumbnailUrl = null,
            )
        },
        duration = duration ?: -1,
        thumbnailUrl = thumbnail.resize(1200, 1200),
        album =
        album?.let {
            MediaMetadata.Album(
                id = it.id,
                title = it.name,
            )
        },
        explicit = explicit,
        setVideoId = setVideoId
    )

fun MediaMetadata.toSong() = Song(
    song = toSongEntity(),
    artists = artists.map {
        com.tamed.music.db.entities.ArtistEntity(
            id = it.id ?: "",
            name = it.name,
            thumbnailUrl = it.thumbnailUrl,
        )
    },
    album = album?.let {
        com.tamed.music.db.entities.AlbumEntity(
            id = it.id,
            title = it.title,
            thumbnailUrl = null,
            songCount = 0,
            duration = 0,
        )
    }
)

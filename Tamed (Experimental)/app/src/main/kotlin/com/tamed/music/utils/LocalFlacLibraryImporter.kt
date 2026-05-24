/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.utils

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.tamed.music.db.MusicDatabase
import com.tamed.music.db.entities.AlbumArtistMap
import com.tamed.music.db.entities.AlbumEntity
import com.tamed.music.db.entities.AlbumWithSongs
import com.tamed.music.db.entities.ArtistEntity
import com.tamed.music.db.entities.Song
import com.tamed.music.db.entities.SongAlbumMap
import com.tamed.music.db.entities.SongArtistMap
import com.tamed.music.db.entities.SongEntity
import com.tamed.music.models.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object LocalFlacLibraryImporter {
    data class Result(
        val imported: Int,
        val matched: Int,
        val total: Int,
    )

    suspend fun importMatchingAlbum(
        context: Context,
        database: MusicDatabase,
        albumWithSongs: AlbumWithSongs,
    ): Result = withContext(Dispatchers.IO) {
        val candidates = context.queryLocalFlacs()
        if (candidates.isEmpty()) return@withContext Result(imported = 0, matched = 0, total = albumWithSongs.songs.size)

        val albumKey = normalize(albumWithSongs.album.title)
        val albumArtistKeys = albumWithSongs.artists.map { normalize(it.name) }.filter { it.isNotBlank() }
        val localAlbumId = "local:flac:album:${albumWithSongs.album.id}"
        val now = LocalDateTime.now()
        var imported = 0
        var matched = 0
        val usedCandidateUris = mutableSetOf<String>()

        database.withTransaction {
            insert(
                AlbumEntity(
                    id = localAlbumId,
                    playlistId = null,
                    title = "${albumWithSongs.album.title} (FLAC)",
                    year = albumWithSongs.album.year,
                    thumbnailUrl = albumWithSongs.album.thumbnailUrl,
                    themeColor = albumWithSongs.album.themeColor,
                    songCount = albumWithSongs.songs.size,
                    duration = albumWithSongs.album.duration,
                    explicit = albumWithSongs.album.explicit,
                    inLibrary = now,
                    isLocal = true,
                ),
            )
            albumWithSongs.artists.forEachIndexed { index, artist ->
                insert(artist.copy(bookmarkedAt = artist.bookmarkedAt, isLocal = artist.isLocal))
                insert(
                    AlbumArtistMap(
                        albumId = localAlbumId,
                        artistId = artist.id,
                        order = index,
                    ),
                )
            }

            albumWithSongs.songs.forEach { song ->
                val trackKey = normalize(song.song.title)
                val candidate = candidates.firstOrNull { candidate ->
                    candidate.contentUri !in usedCandidateUris &&
                    candidate.matches(trackKey = trackKey, albumKey = albumKey, albumArtistKeys = albumArtistKeys)
                } ?: return@forEach

                matched++
                usedCandidateUris += candidate.contentUri
                val localSong =
                    SongEntity(
                        id = candidate.contentUri,
                        title = song.song.title,
                        duration = candidate.durationSeconds.takeIf { it > 0 } ?: song.song.duration,
                        thumbnailUrl = song.song.thumbnailUrl ?: albumWithSongs.album.thumbnailUrl,
                        albumId = localAlbumId,
                        albumName = "${albumWithSongs.album.title} (FLAC)",
                        explicit = song.song.explicit,
                        year = song.song.year ?: albumWithSongs.album.year,
                        date = song.song.date,
                        dateModified = candidate.dateModified ?: song.song.dateModified,
                        liked = false,
                        likedDate = null,
                        totalPlayTime = 0,
                        inLibrary = now,
                        dateDownload = now,
                        isLocal = true,
                        localPath = candidate.contentUri,
                        localMimeType = candidate.mimeType,
                    )

                upsert(localSong)
                albumWithSongs.artists.forEachIndexed { index, artist ->
                    insert(
                        SongArtistMap(
                            songId = localSong.id,
                            artistId = artist.id,
                            position = index,
                        ),
                    )
                }
                insert(
                    SongAlbumMap(
                        songId = localSong.id,
                        albumId = localAlbumId,
                        index = albumWithSongs.songs.indexOf(song),
                    ),
                )
                imported++
            }
        }

        Result(imported = imported, matched = matched, total = albumWithSongs.songs.size)
    }

    data class BatchResult(
        val imported: Int,
        val total: Int,
        val matchedIds: Set<String>,
    )

    suspend fun importMatchingSongs(
        context: Context,
        database: MusicDatabase,
        songs: List<com.tamed.music.db.entities.Song>,
    ): BatchResult = withContext(Dispatchers.IO) {
        val candidates = context.queryLocalFlacs()
        if (candidates.isEmpty()) return@withContext BatchResult(0, songs.size, emptySet())

        var imported = 0
        val matchedIds = mutableSetOf<String>()
        val usedCandidateUris = mutableSetOf<String>()
        val now = LocalDateTime.now()

        database.withTransaction {
            songs.forEach { song ->
                val trackKey = normalize(song.song.title)
                val albumKey = normalize(song.song.albumName)
                val artistKeys = song.artists.map { normalize(it.name) }

                val candidate = candidates.firstOrNull { candidate ->
                    candidate.contentUri !in usedCandidateUris &&
                    candidate.matches(trackKey = trackKey, albumKey = albumKey, albumArtistKeys = artistKeys)
                } ?: return@forEach

                matchedIds.add(song.id)
                usedCandidateUris += candidate.contentUri
                
                val localAlbumId = song.song.albumId?.let { "local:flac:album:$it" } ?: "local:flac:album:unknown"
                
                // Ensure album exists
                if (song.song.albumId != null) {
                    insert(
                        AlbumEntity(
                            id = localAlbumId,
                            title = "${song.song.albumName} (FLAC)",
                            thumbnailUrl = song.song.thumbnailUrl,
                            songCount = 1,
                            duration = song.song.duration,
                            inLibrary = now,
                            isLocal = true,
                        )
                    )
                }

                val localSong =
                    SongEntity(
                        id = candidate.contentUri,
                        title = song.song.title,
                        duration = candidate.durationSeconds.takeIf { it > 0 } ?: song.song.duration,
                        thumbnailUrl = song.song.thumbnailUrl,
                        albumId = localAlbumId,
                        albumName = "${song.song.albumName} (FLAC)",
                        explicit = song.song.explicit,
                        inLibrary = now,
                        dateDownload = now,
                        isLocal = true,
                        localPath = candidate.contentUri,
                        localMimeType = candidate.mimeType,
                    )

                upsert(localSong)
                song.artists.forEachIndexed { index, artist ->
                    insert(artist.copy(isLocal = true))
                    insert(
                        SongArtistMap(
                            songId = localSong.id,
                            artistId = artist.id,
                            position = index,
                        ),
                    )
                }
                imported++
            }
        }

        BatchResult(imported, songs.size, matchedIds)
    }

    suspend fun importMatchingMediaMetadata(
        context: Context,
        database: MusicDatabase,
        items: List<com.tamed.music.models.MediaMetadata>,
    ): BatchResult = withContext(Dispatchers.IO) {
        val candidates = context.queryLocalFlacs()
        if (candidates.isEmpty()) return@withContext BatchResult(0, items.size, emptySet())

        var imported = 0
        val matchedIds = mutableSetOf<String>()
        val usedCandidateUris = mutableSetOf<String>()
        val now = LocalDateTime.now()

        database.withTransaction {
            items.forEach { item ->
                val trackKey = normalize(item.title)
                val albumKey = normalize(item.album?.title)
                val artistKeys = item.artists.map { normalize(it.name) }

                val candidate = candidates.firstOrNull { candidate ->
                    candidate.contentUri !in usedCandidateUris &&
                    candidate.matches(trackKey = trackKey, albumKey = albumKey, albumArtistKeys = artistKeys)
                } ?: return@forEach

                matchedIds.add(item.id)
                usedCandidateUris += candidate.contentUri
                
                val localAlbumId = item.album?.id?.let { "local:flac:album:$it" } ?: "local:flac:album:unknown"
                
                if (item.album?.id != null) {
                    insert(
                        AlbumEntity(
                            id = localAlbumId,
                            title = "${item.album.title} (FLAC)",
                            thumbnailUrl = item.thumbnailUrl,
                            songCount = 1,
                            duration = item.duration,
                            inLibrary = now,
                            isLocal = true,
                        )
                    )
                }

                val localSong =
                    SongEntity(
                        id = candidate.contentUri,
                        title = item.title,
                        duration = candidate.durationSeconds.takeIf { it > 0 } ?: item.duration,
                        thumbnailUrl = item.thumbnailUrl,
                        albumId = localAlbumId,
                        albumName = "${item.album?.title} (FLAC)",
                        explicit = item.explicit,
                        inLibrary = now,
                        dateDownload = now,
                        isLocal = true,
                        localPath = candidate.contentUri,
                        localMimeType = candidate.mimeType,
                    )

                upsert(localSong)
                item.artists.forEachIndexed { index, artist ->
                    val artistId = artist.id ?: "local:flac:artist:${artist.name}"
                    insert(
                        ArtistEntity(
                            id = artistId,
                            name = artist.name,
                            isLocal = true,
                        )
                    )
                    insert(
                        SongArtistMap(
                            songId = localSong.id,
                            artistId = artistId,
                            position = index,
                        ),
                    )
                }
                imported++
            }
        }

        BatchResult(imported, items.size, matchedIds)
    }

    private data class LocalFlacCandidate(
        val contentUri: String,
        val titleKey: String,
        val displayNameKey: String,
        val albumKey: String,
        val artistKey: String,
        val durationSeconds: Int,
        val dateModified: LocalDateTime?,
        val mimeType: String,
    ) {
        fun matches(
            trackKey: String,
            albumKey: String,
            albumArtistKeys: List<String>,
        ): Boolean {
            if (trackKey.isBlank()) return false
            val titleMatches = titleKey == trackKey || displayNameKey.contains(trackKey)
            if (!titleMatches) return false

            val albumMatches = this.albumKey.isBlank() || albumKey.isBlank() || this.albumKey == albumKey
            if (!albumMatches) return false

            return artistKey.isBlank() ||
                albumArtistKeys.isEmpty() ||
                albumArtistKeys.any { artistKey.contains(it) || it.contains(artistKey) }
        }
    }

    private fun Context.queryLocalFlacs(): List<LocalFlacCandidate> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection =
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.MIME_TYPE,
            )
        val selection = "${MediaStore.Audio.Media.MIME_TYPE} = ? OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("audio/flac", "%.flac")

        return runCatching {
            contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val items = mutableListOf<LocalFlacCandidate>()

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val uri = ContentUris.withAppendedId(collection, id).toString()
                    val modifiedSeconds = cursor.getLongOrNull(modifiedColumn)
                    items +=
                        LocalFlacCandidate(
                            contentUri = uri,
                            titleKey = normalize(cursor.getStringOrNull(titleColumn)),
                            displayNameKey = normalize(cursor.getStringOrNull(displayNameColumn)?.substringBeforeLast('.')),
                            albumKey = normalize(cursor.getStringOrNull(albumColumn)),
                            artistKey = normalize(cursor.getStringOrNull(artistColumn)),
                            durationSeconds = (cursor.getLongOrNull(durationColumn) ?: 0L).div(1000L).toInt(),
                            dateModified = modifiedSeconds?.let {
                                Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
                            },
                            mimeType = cursor.getStringOrNull(mimeTypeColumn) ?: "audio/flac",
                        )
                }

                items
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun android.database.Cursor.getStringOrNull(columnIndex: Int): String? =
        if (isNull(columnIndex)) null else getString(columnIndex)

    private fun android.database.Cursor.getLongOrNull(columnIndex: Int): Long? =
        if (isNull(columnIndex)) null else getLong(columnIndex)

    private fun normalize(value: String?): String {
        val normalized = Normalizer.normalize(value.orEmpty(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .lowercase()
        return normalized.replace("[^a-z0-9]+".toRegex(), " ").trim()
    }
}

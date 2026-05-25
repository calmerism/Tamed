/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.spotiflac

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.tamed.music.R
import com.tamed.music.db.MusicDatabase
import com.tamed.music.db.entities.AlbumArtistMap
import com.tamed.music.db.entities.AlbumEntity
import com.tamed.music.db.entities.ArtistEntity
import com.tamed.music.db.entities.Song
import com.tamed.music.db.entities.SongAlbumMap
import com.tamed.music.db.entities.SongArtistMap
import com.tamed.music.db.entities.SongEntity
import com.tamed.music.innertube.models.SongItem
import com.tamed.music.models.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import kotlin.math.abs

object SpotiFlacDownloader {
    private const val TAG = "SpotiFlacDownloader"
    private val builtInProviders = setOf("tidal", "qobuz")

    sealed interface Result {
        data class Success(val filePath: String, val localSongId: String) : Result
        data object BackendMissing : Result
        data class Failed(val message: String) : Result
    }

    suspend fun downloadSongItem(
        context: Context,
        database: MusicDatabase,
        song: SongItem,
        provider: SpotiFlacProvider,
        quality: SpotiFlacQuality,
    ): Result {
        val albumTitle = song.album?.name.orEmpty().ifBlank { "Singles" }
        val albumId = song.album?.id ?: "single:${song.id}"
        val artistNames = song.artists.map { it.name }.filter { it.isNotBlank() }
        return downloadTrack(
            context = context,
            database = database,
            sourceSongId = song.id,
            title = song.title,
            artists = artistNames,
            albumId = albumId,
            albumTitle = albumTitle,
            thumbnailUrl = song.thumbnail,
            durationSeconds = song.duration ?: -1,
            explicit = song.explicit,
            trackNumber = 0,
            totalTracks = 1,
            provider = provider,
            quality = quality,
        )
    }

    suspend fun downloadSong(
        context: Context,
        database: MusicDatabase,
        song: Song,
        provider: SpotiFlacProvider,
        quality: SpotiFlacQuality,
    ): Result {
        val albumTitle = song.album?.title ?: song.song.albumName.orEmpty().ifBlank { "Singles" }
        val albumId = song.album?.id ?: song.song.albumId ?: "single:${song.id}"
        return downloadTrack(
            context = context,
            database = database,
            sourceSongId = song.id,
            title = song.song.title,
            artists = song.artists.map { it.name },
            albumId = albumId,
            albumTitle = albumTitle,
            thumbnailUrl = song.song.thumbnailUrl ?: song.album?.thumbnailUrl,
            durationSeconds = song.song.duration,
            explicit = song.song.explicit,
            trackNumber = 0,
            totalTracks = 1,
            provider = provider,
            quality = quality,
        )
    }

    suspend fun downloadSongs(
        context: Context,
        database: MusicDatabase,
        songs: List<com.tamed.music.db.entities.Song>,
        provider: SpotiFlacProvider,
        quality: SpotiFlacQuality,
    ): List<Result> {
        return songs.map { song ->
            val albumTitle = song.album?.title ?: song.song.albumName.orEmpty().ifBlank { "Singles" }
            val albumId = song.album?.id ?: song.song.albumId ?: "single:${song.id}"
            downloadTrack(
                context = context,
                database = database,
                sourceSongId = song.id,
                title = song.song.title,
                artists = song.artists.map { it.name },
                albumId = albumId,
                albumTitle = albumTitle,
                thumbnailUrl = song.song.thumbnailUrl ?: song.album?.thumbnailUrl,
                durationSeconds = song.song.duration,
                explicit = song.song.explicit,
                trackNumber = 0,
                totalTracks = 1,
                provider = provider,
                quality = quality,
            )
        }
    }

    suspend fun downloadMediaMetadata(
        context: Context,
        database: MusicDatabase,
        mediaMetadata: MediaMetadata,
        provider: SpotiFlacProvider,
        quality: SpotiFlacQuality,
    ): Result {
        val albumTitle = mediaMetadata.album?.title.orEmpty().ifBlank { "Singles" }
        val albumId = mediaMetadata.album?.id ?: "single:${mediaMetadata.id}"
        val artistNames = mediaMetadata.artists.map { it.name }.filter { it.isNotBlank() }
        return downloadTrack(
            context = context,
            database = database,
            sourceSongId = mediaMetadata.id,
            title = mediaMetadata.title,
            artists = artistNames,
            albumId = albumId,
            albumTitle = albumTitle,
            thumbnailUrl = mediaMetadata.thumbnailUrl,
            durationSeconds = mediaMetadata.duration,
            explicit = mediaMetadata.explicit,
            trackNumber = 0,
            totalTracks = 1,
            provider = provider,
            quality = quality,
        )
    }

    suspend fun downloadAlbum(
        context: Context,
        database: MusicDatabase,
        albumId: String,
        albumTitle: String,
        albumArtistNames: List<String>,
        thumbnailUrl: String?,
        year: Int?,
        songs: List<com.tamed.music.db.entities.Song>,
        provider: SpotiFlacProvider,
        quality: SpotiFlacQuality,
    ): List<Result> {
        val localAlbumId = localAlbumId(albumId)
        database.withTransaction {
            insert(
                AlbumEntity(
                    id = localAlbumId,
                    title = "$albumTitle (FLAC)",
                    year = year,
                    thumbnailUrl = thumbnailUrl,
                    songCount = songs.size,
                    duration = songs.sumOf { it.song.duration.takeIf { duration -> duration > 0 } ?: 0 },
                    explicit = songs.any { it.song.explicit },
                    inLibrary = LocalDateTime.now(),
                    isLocal = true,
                ),
            )
            songs.firstOrNull()?.artists.orEmpty().forEachIndexed { index, artist ->
                insert(artist)
                insert(AlbumArtistMap(albumId = localAlbumId, artistId = artist.id, order = index))
            }
        }

        return songs.mapIndexed { index, song ->
            downloadTrack(
                context = context,
                database = database,
                sourceSongId = song.id,
                title = song.song.title,
                artists = song.artists.map { it.name }.ifEmpty { albumArtistNames },
                albumId = albumId,
                albumTitle = albumTitle,
                thumbnailUrl = song.song.thumbnailUrl ?: thumbnailUrl,
                durationSeconds = song.song.duration,
                explicit = song.song.explicit,
                trackNumber = index + 1,
                totalTracks = songs.size,
                provider = provider,
                quality = quality,
            )
        }
    }

    private suspend fun downloadTrack(
        context: Context,
        database: MusicDatabase,
        sourceSongId: String,
        title: String,
        artists: List<String>,
        albumId: String,
        albumTitle: String,
        thumbnailUrl: String?,
        durationSeconds: Int,
        explicit: Boolean,
        trackNumber: Int,
        totalTracks: Int,
        provider: SpotiFlacProvider,
        quality: SpotiFlacQuality,
    ): Result = withContext(Dispatchers.IO) {
        val backend = spotiFlacGoBackend ?: return@withContext Result.BackendMissing
        val artistNames = artists.filter { it.isNotBlank() }
        val artistName = artistNames.joinToString(", ").ifBlank { "Unknown Artist" }
        val downloadItemId =
            sourceSongId.ifBlank {
                listOf(albumId, title, artistName, durationSeconds.takeIf { it > 0 }?.toString().orEmpty())
                    .filter { it.isNotBlank() }
                    .joinToString(":")
                    .ifBlank { "spotiflac-${System.currentTimeMillis()}" }
            }
        val outputDir =
            File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir,
                "SpotiFLAC",
            ).apply { mkdirs() }

        val metadataHint =
            resolveTrackMetadataHint(
                backend = backend,
                title = title,
                artists = artistNames,
                albumTitle = albumTitle,
                durationSeconds = durationSeconds,
                preferredProvider = provider,
            )

        val localAlbumId = localAlbumId(albumId)
        val resolvedTrackNumber = metadataHint?.trackNumber?.takeIf { it > 0 } ?: trackNumber.takeIf { it > 0 } ?: 1
        val resolvedTotalTracks = metadataHint?.totalTracks?.takeIf { it > 0 } ?: totalTracks.coerceAtLeast(1)
        val resolvedDiscNumber = metadataHint?.discNumber?.takeIf { it > 0 } ?: 1
        val resolvedTotalDiscs = metadataHint?.totalDiscs?.takeIf { it > 0 } ?: 1
        val resolvedTitle = metadataHint?.name?.takeIf { it.isNotBlank() } ?: title
        val resolvedArtists = metadataHint?.artists?.takeIf { it.isNotBlank() } ?: artistName
        val resolvedAlbumTitle = metadataHint?.albumName?.takeIf { it.isNotBlank() } ?: albumTitle
        val resolvedAlbumArtist = metadataHint?.albumArtist?.takeIf { it.isNotBlank() } ?: resolvedArtists
        val resolvedThumbnailUrl = metadataHint?.coverUrl?.takeIf { it.isNotBlank() } ?: thumbnailUrl.orEmpty()
        val resolvedSource = metadataHint?.providerId.orEmpty()
        val requestIsrc = metadataHint?.isrc.orEmpty()
        val requestSpotifyId = metadataHint?.spotifyId.orEmpty()
        val requestTidalId = metadataHint?.tidalId.orEmpty()
        val requestQobuzId = metadataHint?.qobuzId.orEmpty()
        val normalizedSource = resolvedSource.trim().lowercase()
        val useExtensionSource = normalizedSource.isNotBlank() && normalizedSource !in builtInProviders

        fun buildPayload(
            trackName: String,
            artistNameValue: String,
            albumNameValue: String,
            albumArtistValue: String,
            coverUrlValue: String,
            sourceValue: String,
            useExtensions: Boolean,
        ): JSONObject =
            JSONObject()
                .put("isrc", requestIsrc)
                .put("service", provider.requestValue)
                .put("source", sourceValue)
                .put("spotify_id", requestSpotifyId)
                .put("tidal_id", requestTidalId)
                .put("qobuz_id", requestQobuzId)
                .put("track_name", trackName)
                .put("artist_name", artistNameValue)
                .put("album_name", albumNameValue)
                .put("album_artist", albumArtistValue)
                .put("cover_url", coverUrlValue)
                .put("output_dir", outputDir.absolutePath)
                .put("filename_format", "{album_artist}/{album}/{tracknumber}. {title}")
                .put("quality", quality.requestValue)
                .put("embed_metadata", true)
                .put("artist_tag_mode", "joined")
                .put("embed_lyrics", true)
                .put("embed_max_quality_cover", true)
                .put("track_number", resolvedTrackNumber)
                .put("disc_number", resolvedDiscNumber)
                .put("total_tracks", resolvedTotalTracks)
                .put("total_discs", resolvedTotalDiscs)
                .put("release_date", metadataHint?.releaseDate.orEmpty())
                .put("item_id", downloadItemId)
                .put("duration_ms", durationSeconds.takeIf { it > 0 }?.times(1000) ?: 0)
                .put("use_extensions", useExtensions)
                .put("use_fallback", true)
                .put("songlink_region", "US")

        data class DownloadAttempt(
            val label: String,
            val payload: JSONObject,
        )

        val originalArtists = artistName
        val originalAlbumTitle = albumTitle.takeUnless { it.equals("Singles", ignoreCase = true) }.orEmpty()
        val originalAlbumArtist = originalArtists
        val attempts =
            buildList {
                add(
                    DownloadAttempt(
                        label = "resolved",
                        payload =
                            buildPayload(
                                trackName = resolvedTitle,
                                artistNameValue = resolvedArtists,
                                albumNameValue = resolvedAlbumTitle,
                                albumArtistValue = resolvedAlbumArtist,
                                coverUrlValue = resolvedThumbnailUrl,
                                sourceValue = if (useExtensionSource) resolvedSource else "",
                                useExtensions = useExtensionSource,
                            ),
                    ),
                )

                val fallbackPayload =
                    buildPayload(
                        trackName = title,
                        artistNameValue = originalArtists,
                        albumNameValue = originalAlbumTitle,
                        albumArtistValue = originalAlbumArtist,
                        coverUrlValue = thumbnailUrl.orEmpty(),
                        sourceValue = "",
                        useExtensions = false,
                    )

                if (fallbackPayload.toString() != first().payload.toString()) {
                    add(
                        DownloadAttempt(
                            label = "fallback-built-in",
                            payload = fallbackPayload,
                        ),
                    )
                }

                val minimalPayload =
                    buildPayload(
                        trackName = title,
                        artistNameValue = originalArtists,
                        albumNameValue = "",
                        albumArtistValue = "",
                        coverUrlValue = "",
                        sourceValue = "",
                        useExtensions = false,
                    )

                if (minimalPayload.toString() != last().payload.toString()) {
                    add(
                        DownloadAttempt(
                            label = "minimal-built-in",
                            payload = minimalPayload,
                        ),
                    )
                }
            }

        coroutineScope {
            val progressJob =
                launch {
                    while (isActive) {
                        val progressJson = runCatching { backend.getItemProgress(downloadItemId) }.getOrNull().orEmpty()
                        val progressObject = runCatching { JSONObject(progressJson) }.getOrNull()
                        if (progressObject != null && progressObject.length() > 0) {
                            val progressValue = progressObject.optDouble("progress", -1.0).toFloat()
                            val status = progressObject.optString("status").ifBlank { null }
                            val isDownloading = progressObject.optBoolean("is_downloading", true)
                            val subtitle =
                                when {
                                    status.equals("finalizing", ignoreCase = true) -> context.getString(R.string.spotiflac_finalizing_download)
                                    isDownloading -> artistName
                                    else -> resolvedAlbumTitle
                                }

                            SpotiFlacProgressNotifier.show(
                                context = context.applicationContext,
                                itemId = downloadItemId,
                                title = resolvedTitle,
                                subtitle = subtitle,
                                progress = progressValue.takeIf { it in 0f..1f },
                                status = status,
                            )

                            if (status.equals("completed", ignoreCase = true) || !isDownloading) {
                                break
                            }
                        } else {
                            SpotiFlacProgressNotifier.show(
                                context = context.applicationContext,
                                itemId = downloadItemId,
                                title = resolvedTitle,
                                subtitle = artistName,
                                progress = null,
                                status = null,
                            )
                        }
                        delay(750)
                    }
                }

            SpotiFlacProgressNotifier.show(
                context = context.applicationContext,
                itemId = downloadItemId,
                title = resolvedTitle,
                subtitle = artistName,
                progress = null,
                status = null,
            )

            var keepTerminalNotification = false
            try {
                var lastError = "SpotiFLAC download failed"
                var response: JSONObject? = null

                for ((index, attempt) in attempts.withIndex()) {
                    Log.d(TAG, "Trying SpotiFLAC attempt ${index + 1}/${attempts.size} for $title via ${attempt.label}")
                    val attemptResponse =
                        runCatching { JSONObject(backend.downloadByStrategy(attempt.payload.toString())) }
                            .getOrElse { throwable ->
                                lastError = throwable.message ?: "SpotiFLAC failed"
                                Log.w(TAG, "SpotiFLAC attempt '${attempt.label}' threw for $title", throwable)
                                null
                            }

                    if (attemptResponse == null) continue
                    if (attemptResponse.optBoolean("success")) {
                        response = attemptResponse
                        break
                    }

                    lastError = attemptResponse.optString("error", lastError)
                    Log.w(TAG, "SpotiFLAC attempt '${attempt.label}' failed for $title: $lastError")
                }

                val successResponse = response
                if (successResponse == null) {
                    keepTerminalNotification = true
                    SpotiFlacProgressNotifier.showFailed(
                        context = context.applicationContext,
                        itemId = downloadItemId,
                        title = resolvedTitle,
                        subtitle = artistName,
                        errorMsg = lastError,
                    )
                    return@coroutineScope Result.Failed(lastError)
                }

                val filePath = successResponse.optString("file_path").ifBlank {
                    Log.w(TAG, "SpotiFLAC succeeded without a file path for $title")
                    val error = "SpotiFLAC did not return a file path"
                    keepTerminalNotification = true
                    SpotiFlacProgressNotifier.showFailed(
                        context = context.applicationContext,
                        itemId = downloadItemId,
                        title = resolvedTitle,
                        subtitle = artistName,
                        errorMsg = error,
                    )
                    return@coroutineScope Result.Failed(error)
                }

                val localSongId = Uri.fromFile(File(filePath)).toString()
                val now = LocalDateTime.now()

                database.withTransaction {
                    insert(
                        AlbumEntity(
                            id = localAlbumId,
                            title = "${resolvedAlbumTitle} (FLAC)",
                            thumbnailUrl = resolvedThumbnailUrl.ifBlank { thumbnailUrl },
                            songCount = resolvedTotalTracks,
                            duration = durationSeconds.takeIf { it > 0 } ?: 0,
                            explicit = explicit,
                            inLibrary = now,
                            bookmarkedAt = now,
                            isLocal = true,
                        ),
                    )
                    upsert(
                        SongEntity(
                            id = localSongId,
                            title = successResponse.optString("title").ifBlank { resolvedTitle },
                            duration = durationSeconds,
                            thumbnailUrl = resolvedThumbnailUrl.ifBlank { thumbnailUrl },
                            albumId = localAlbumId,
                            albumName = "${resolvedAlbumTitle} (FLAC)",
                            explicit = explicit,
                            inLibrary = now,
                            dateDownload = now,
                            liked = true,
                            likedDate = now,
                            isLocal = true,
                            localPath = localSongId,
                            localMimeType = "audio/flac",
                        ),
                    )
                    parseArtistsForStorage(resolvedArtists, artistNames).forEachIndexed { index, artistNameValue ->
                        val artist =
                            artistByName(artistNameValue) ?: ArtistEntity.generateArtistId().let {
                                ArtistEntity(
                                    id = it, 
                                    name = artistNameValue, 
                                    isLocal = true,
                                    bookmarkedAt = now,
                                )
                            }
                        insert(artist.copy(bookmarkedAt = now))
                        insert(SongArtistMap(songId = localSongId, artistId = artist.id, position = index))
                    }
                    insert(SongAlbumMap(songId = localSongId, albumId = localAlbumId, index = resolvedTrackNumber - 1))
                }

                Result.Success(filePath = filePath, localSongId = localSongId)
            } finally {
                progressJob.cancelAndJoin()
                if (!keepTerminalNotification) {
                    SpotiFlacProgressNotifier.cancel(context.applicationContext, downloadItemId)
                }
            }
        }
    }

    private fun localAlbumId(albumId: String): String = "local:flac:album:$albumId"

    private fun parseArtistsForStorage(
        resolvedArtists: String,
        fallbackArtists: List<String>,
    ): List<String> {
        val parsed =
            resolvedArtists
                .split(',', '&')
                .map { it.trim() }
                .filter { it.isNotBlank() }
        return parsed.ifEmpty { fallbackArtists.ifEmpty { listOf("Unknown Artist") } }
    }

    internal fun resolveTrackMetadataHint(
        backend: Backend,
        title: String,
        artists: List<String>,
        albumTitle: String,
        durationSeconds: Int,
        preferredProvider: SpotiFlacProvider,
    ): ResolvedTrackMetadata? {
        val joinedArtists = artists.joinToString(" ").trim()
        val firstArtist = artists.firstOrNull().orEmpty()
        val queries =
            linkedSetOf(
                listOf(title, joinedArtists, albumTitle).joinToString(" ").trim(),
                listOf(title, joinedArtists).joinToString(" ").trim(),
                listOf(joinedArtists, title).joinToString(" ").trim(),
                listOf(title, firstArtist, albumTitle).joinToString(" ").trim(),
                listOf(firstArtist, title).joinToString(" ").trim(),
                title.trim(),
            ).filter { it.isNotBlank() }

        val candidates = LinkedHashMap<String, ResolvedTrackMetadata>()
        for (query in queries) {
            runCatching { JSONArray(backend.searchTracksWithMetadataProvidersJSON(query, 12, true)) }
                .getOrNull()
                ?.let { results ->
                    repeat(results.length()) { index ->
                        results.optJSONObject(index)?.let { json ->
                            val candidate = ResolvedTrackMetadata.fromJson(json)
                            val key = candidate.uniqueKey()
                            if (key.isNotBlank() && key !in candidates) {
                                candidates[key] = candidate
                            }
                        }
                    }
                }

            if (candidates.isNotEmpty()) {
                val bestScore = candidates.values.maxOf { candidate ->
                    scoreMetadataCandidate(
                        candidate = candidate,
                        title = title,
                        artists = artists,
                        albumTitle = albumTitle,
                        durationSeconds = durationSeconds,
                        preferredProvider = preferredProvider,
                    )
                }
                if (bestScore >= 120) {
                    Log.d(TAG, "resolveTrackMetadataHint: Found high confidence match (score=$bestScore) for query '$query'. Stopping search.")
                    break
                }
            }
        }

        val scoredCandidates =
            candidates.values.map { candidate ->
                candidate to scoreMetadataCandidate(
                    candidate = candidate,
                    title = title,
                    artists = artists,
                    albumTitle = albumTitle,
                    durationSeconds = durationSeconds,
                    preferredProvider = preferredProvider,
                )
            }

        return scoredCandidates
            .filter { (_, score) -> score >= 85 }
            .maxByOrNull { (_, score) -> score }
            ?.first
    }

    private fun scoreMetadataCandidate(
        candidate: ResolvedTrackMetadata,
        title: String,
        artists: List<String>,
        albumTitle: String,
        durationSeconds: Int,
        preferredProvider: SpotiFlacProvider,
    ): Int {
        if (!titlesRoughlyMatch(title, candidate.name)) return Int.MIN_VALUE
        if (artists.isNotEmpty() && artists.none { artistsRoughlyMatch(it, candidate.artists) }) {
            return Int.MIN_VALUE
        }

        var score = 100

        if (artists.isNotEmpty()) {
            score += 60
        }

        if (albumTitle.isNotBlank() && candidate.albumName.isNotBlank()) {
            score += if (titlesRoughlyMatch(albumTitle, candidate.albumName)) 30 else -10
        }

        if (durationSeconds > 0 && candidate.durationMs > 0) {
            val durationDiff = abs(durationSeconds - (candidate.durationMs / 1000))
            score += when {
                durationDiff <= 2 -> 40
                durationDiff <= 5 -> 25
                durationDiff <= 10 -> 10
                else -> -30
            }
        }

        if (candidate.isrc.isNotBlank()) score += 20
        if (candidate.spotifyId.isNotBlank()) score += 20
        if (preferredProvider == SpotiFlacProvider.TIDAL && candidate.tidalId.isNotBlank()) score += 50
        if (preferredProvider == SpotiFlacProvider.QOBUZ && candidate.qobuzId.isNotBlank()) score += 50

        return score
    }

    private fun titlesRoughlyMatch(
        expected: String,
        found: String,
    ): Boolean {
        val normalizedExpected = normalizeLooseValue(expected)
        val normalizedFound = normalizeLooseValue(found)
        if (normalizedExpected.isBlank() || normalizedFound.isBlank()) return false
        if (normalizedExpected == normalizedFound) return true
        if (normalizedExpected.contains(normalizedFound) || normalizedFound.contains(normalizedExpected)) return true

        val expectedCore = normalizedExpected.substringBefore(" feat ").substringBefore(" ft ").trim()
        val foundCore = normalizedFound.substringBefore(" feat ").substringBefore(" ft ").trim()
        return expectedCore.isNotBlank() && expectedCore == foundCore
    }

    private fun artistsRoughlyMatch(
        expected: String,
        found: String,
    ): Boolean {
        val normalizedExpected = normalizeLooseValue(expected)
        val normalizedFound = normalizeLooseValue(found)
        if (normalizedExpected.isBlank() || normalizedFound.isBlank()) return false
        if (normalizedExpected == normalizedFound) return true
        if (normalizedExpected.contains(normalizedFound) || normalizedFound.contains(normalizedExpected)) return true

        val foundArtists = normalizedFound.split(',', '&').map { it.trim() }.filter { it.isNotBlank() }
        return foundArtists.any {
            it == normalizedExpected || it.contains(normalizedExpected) || normalizedExpected.contains(it)
        }
    }

    private fun normalizeLooseValue(value: String): String =
        value
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    internal val spotiFlacGoBackend: Backend?
        get() =
            runCatching {
                val clazz = Class.forName("gobackend.Gobackend")
                Backend(
                    downloadByStrategyMethod = clazz.getMethod("downloadByStrategy", String::class.java),
                    searchTracksWithMetadataProvidersMethod =
                        clazz.getMethod(
                            "searchTracksWithMetadataProvidersJSON",
                            String::class.java,
                            java.lang.Long.TYPE,
                            java.lang.Boolean.TYPE,
                        ),
                    getItemProgressMethod =
                        runCatching { clazz.getMethod("getItemProgress", String::class.java) }
                            .getOrElse {
                                // Older generated bindings can keep the exported Go casing.
                                clazz.getMethod("GetItemProgress", String::class.java)
                            },
                    resolvePlaybackStreamMethod =
                        runCatching { clazz.getMethod("resolvePlaybackStreamJSON", String::class.java) }
                            .recoverCatching {
                                clazz.getMethod("ResolvePlaybackStreamJSON", String::class.java)
                            }.getOrNull(),
                )
            }.onFailure {
                Log.e(TAG, "Failed to initialize embedded SpotiFLAC backend", it)
            }.getOrNull()

    internal class Backend(
        private val downloadByStrategyMethod: java.lang.reflect.Method,
        private val searchTracksWithMetadataProvidersMethod: java.lang.reflect.Method,
        private val getItemProgressMethod: java.lang.reflect.Method,
        private val resolvePlaybackStreamMethod: java.lang.reflect.Method?,
    ) {
        fun downloadByStrategy(requestJson: String): String =
            downloadByStrategyMethod.invoke(null, requestJson) as String

        fun searchTracksWithMetadataProvidersJSON(
            query: String,
            limit: Int,
            includeExtensions: Boolean,
        ): String =
            searchTracksWithMetadataProvidersMethod.invoke(null, query, limit.toLong(), includeExtensions) as String

        fun getItemProgress(itemId: String): String =
            getItemProgressMethod.invoke(null, itemId) as String

        fun resolvePlaybackStreamJSON(requestJson: String): String? =
            resolvePlaybackStreamMethod?.invoke(null, requestJson) as? String
    }

    internal data class ResolvedTrackMetadata(
        val name: String,
        val artists: String,
        val albumName: String,
        val albumArtist: String,
        val durationMs: Int,
        val isrc: String,
        val spotifyId: String,
        val tidalId: String,
        val qobuzId: String,
        val releaseDate: String,
        val trackNumber: Int,
        val totalTracks: Int,
        val discNumber: Int,
        val totalDiscs: Int,
        val coverUrl: String,
        val providerId: String,
        val id: String,
    ) {
        fun uniqueKey(): String =
            listOf(
                providerId,
                id,
                tidalId,
                qobuzId,
                spotifyId,
                isrc,
                name,
                artists,
            ).firstOrNull { it.isNotBlank() }.orEmpty()

        companion object {
            fun fromJson(json: JSONObject): ResolvedTrackMetadata =
                ResolvedTrackMetadata(
                    name = json.optString("name"),
                    artists = json.optString("artists"),
                    albumName = json.optString("album_name"),
                    albumArtist = json.optString("album_artist"),
                    durationMs = json.optInt("duration_ms"),
                    isrc = json.optString("isrc"),
                    spotifyId = json.optString("spotify_id"),
                    tidalId = json.optString("tidal_id"),
                    qobuzId = json.optString("qobuz_id"),
                    releaseDate = json.optString("release_date"),
                    trackNumber = json.optInt("track_number"),
                    totalTracks = json.optInt("total_tracks"),
                    discNumber = json.optInt("disc_number"),
                    totalDiscs = json.optInt("total_discs"),
                    coverUrl = json.optString("cover_url").ifBlank { json.optString("images") },
                    providerId = json.optString("provider_id"),
                    id = json.optString("id"),
                )
        }
    }
}

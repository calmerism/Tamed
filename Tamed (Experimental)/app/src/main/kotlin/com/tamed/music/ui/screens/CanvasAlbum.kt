/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.tamed.music.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tamed.music.canvas.CanvasArtwork
import com.tamed.music.ui.player.CanvasArtworkPlaybackCache
import com.tamed.music.vivimusiccanvas.ViviMusicCanvasProvider
import com.tamed.music.applecanvas.AppleMusicCanvasProvider
import com.tamed.music.ui.player.normalizeCanvasSongTitle
import com.tamed.music.ui.player.normalizeCanvasArtistName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun rememberAlbumCanvas(
    albumTitle: String?,
    artistName: String?,
    firstSongTitle: String? = null,
): CanvasArtwork? {
    val cacheKey = remember(albumTitle, artistName, firstSongTitle) {
        when {
            !albumTitle.isNullOrBlank() && !artistName.isNullOrBlank() -> "album|$albumTitle|$artistName"
            !firstSongTitle.isNullOrBlank() && !artistName.isNullOrBlank() -> "track|$firstSongTitle|$artistName"
            else -> null
        }
    }

    val storefront = remember {
        val country = Locale.getDefault().country
        if (country.length == 2) country.lowercase(Locale.ROOT) else "us"
    }

    var canvasArtwork by remember(cacheKey) {
        mutableStateOf(cacheKey?.let { CanvasArtworkPlaybackCache.get(it) })
    }
    LaunchedEffect(albumTitle, artistName, firstSongTitle) {
        if (canvasArtwork != null || cacheKey == null) return@LaunchedEffect
        if (artistName.isNullOrBlank() || (albumTitle.isNullOrBlank() && firstSongTitle.isNullOrBlank())) {
            canvasArtwork = null
            return@LaunchedEffect
        }

        val fetched = withContext(Dispatchers.IO) {
            val normalizedAlbumTitle = albumTitle?.let(::normalizeCanvasSongTitle).orEmpty()
            val normalizedFirstSongTitle = firstSongTitle?.let { normalizeCanvasSongTitle(it) }
            val normalizedArtistName = normalizeCanvasArtistName(artistName)
            val albumTitleWithoutLocalSuffix =
                albumTitle
                    ?.replace(Regex("\\s*\\((?:flac|lossless|local)\\)\\s*$", RegexOption.IGNORE_CASE), "")
                    ?.let(::normalizeCanvasSongTitle)

            val albumCandidates = linkedSetOf(
                normalizedAlbumTitle to normalizedArtistName,
                albumTitle.orEmpty() to normalizedArtistName,
                normalizedAlbumTitle to artistName,
                albumTitle.orEmpty() to artistName,
                normalizedFirstSongTitle.orEmpty() to normalizedArtistName,
                firstSongTitle.orEmpty() to normalizedArtistName,
                albumTitleWithoutLocalSuffix.orEmpty() to normalizedArtistName,
                albumTitleWithoutLocalSuffix.orEmpty() to artistName,
            ).filter { it.first.isNotBlank() && it.second.isNotBlank() }

            albumCandidates.firstNotNullOfOrNull { (album, artist) ->
                ViviMusicCanvasProvider.getByAlbumArtist(
                    album = album,
                    artist = artist
                )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                    ?: AppleMusicCanvasProvider.getByAlbumArtist(
                        album = album,
                        artist = artist,
                        storefront = storefront
                    )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
            }
        }

        // Artist validation check (matches Thumbnail.kt logic)
        val validated = (fetched?.let { artwork ->
            val resultArtist = artwork.artist
            if (resultArtist != null && artistName.isNotBlank()) {
                val normalizedResultArtist = normalizeCanvasArtistName(resultArtist)
                val normalizedRequestedArtist = normalizeCanvasArtistName(artistName)
                if (
                    resultArtist.contains(artistName, ignoreCase = true) ||
                    artistName.contains(resultArtist, ignoreCase = true) ||
                    normalizedResultArtist == normalizedRequestedArtist
                ) {
                    artwork
                } else null
            } else artwork
        })

        if (validated != null) {
            canvasArtwork = validated
            CanvasArtworkPlaybackCache.put(cacheKey, validated)
        }
    }

    return canvasArtwork
}



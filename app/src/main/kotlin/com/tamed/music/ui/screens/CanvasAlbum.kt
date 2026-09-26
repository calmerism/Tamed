package com.tamed.music.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tamed.music.canvas.CanvasArtwork
import com.tamed.music.canvas.TidalCanvasProvider
import com.tamed.music.ui.player.CanvasArtworkPlaybackCache
import com.tamed.music.vivimusiccanvas.ViviMusicCanvasProvider
import com.tamed.music.applecanvas.AppleMusicCanvasProvider
import com.tamed.music.canvas.normalizeCanvasSongTitle
import com.tamed.music.canvas.normalizeCanvasArtistName
import com.tamed.music.canvas.validateCanvasMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun rememberAlbumCanvas(
    albumTitle: String?,
    artistName: String?,
    firstSongTitle: String? = null,
    enabled: Boolean = true,
): CanvasArtwork? {
    if (!enabled) return null

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

    var canvasArtwork by remember(cacheKey, enabled) {
        mutableStateOf(if (enabled) cacheKey?.let { CanvasArtworkPlaybackCache.get(it) } else null)
    }

    LaunchedEffect(albumTitle, artistName, firstSongTitle, enabled) {
        if (!enabled || canvasArtwork != null || cacheKey == null) return@LaunchedEffect
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

            coroutineScope {
                val primaryAlbum = normalizedAlbumTitle.ifBlank { albumTitle.orEmpty() }
                val primaryArtist = normalizedArtistName.ifBlank { artistName.orEmpty() }

                val applePrimary = async {
                    try {
                        AppleMusicCanvasProvider.getByAlbumArtist(
                            album = primaryAlbum,
                            artist = primaryArtist,
                            storefront = storefront
                        )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                    } catch (_: Exception) { null }
                }

                val tidalPrimary = async {
                    try {
                        TidalCanvasProvider.getByAlbumArtist(
                            album = primaryAlbum,
                            artist = primaryArtist
                        )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                    } catch (_: Exception) { null }
                }

                val viviPrimary = async {
                    try {
                        ViviMusicCanvasProvider.getByAlbumArtist(
                            album = primaryAlbum,
                            artist = primaryArtist
                        )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                    } catch (_: Exception) { null }
                }

                val fastResult = applePrimary.await() ?: tidalPrimary.await() ?: viviPrimary.await()
                if (fastResult != null) return@coroutineScope fastResult

                val fallbackCandidates = linkedSetOf(
                    albumTitle.orEmpty() to normalizedArtistName,
                    normalizedAlbumTitle to artistName,
                    albumTitle.orEmpty() to artistName,
                    normalizedFirstSongTitle.orEmpty() to normalizedArtistName,
                    firstSongTitle.orEmpty() to normalizedArtistName,
                    albumTitleWithoutLocalSuffix.orEmpty() to normalizedArtistName,
                    albumTitleWithoutLocalSuffix.orEmpty() to artistName,
                ).filter { it.first.isNotBlank() && it.second.isNotBlank() && (it.first != primaryAlbum || it.second != primaryArtist) }

                fallbackCandidates.firstNotNullOfOrNull { (album, artist) ->
                    val appleFallback = async {
                        try {
                            AppleMusicCanvasProvider.getByAlbumArtist(
                                album = album,
                                artist = artist,
                                storefront = storefront
                            )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                        } catch (_: Exception) { null }
                    }
                    val tidalFallback = async {
                        try {
                            TidalCanvasProvider.getByAlbumArtist(
                                album = album,
                                artist = artist
                            )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                        } catch (_: Exception) { null }
                    }
                    val viviFallback = async {
                        try {
                            ViviMusicCanvasProvider.getByAlbumArtist(
                                album = album,
                                artist = artist
                            )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                        } catch (_: Exception) { null }
                    }
                    appleFallback.await() ?: tidalFallback.await() ?: viviFallback.await()
                }
            }
        }

        // Artist and Album validation check (matches Thumbnail.kt logic)
        val validated = fetched?.takeIf { artwork ->
            validateCanvasMatch(
                artwork = artwork,
                requestedTitle = firstSongTitle.orEmpty(),
                requestedArtist = artistName,
                requestedAlbum = albumTitle.orEmpty()
            )
        }

        if (validated != null) {
            canvasArtwork = validated
            CanvasArtworkPlaybackCache.put(cacheKey, validated)
        }
    }

    return canvasArtwork
}

/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.vivimusiccanvas

import android.util.Log
import com.tamed.music.canvas.CanvasArtwork
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Calls the Go backend's [FetchCanvasBySongArtist] function via reflection
 * (same pattern as SpotiFlacDownloader).  Returns null if the backend AAR
 * is not present or if no canvas was found for the given song/artist.
 */
object CanvasGoProvider {

    private const val TAG = "CanvasGoProvider"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Tries to resolve a [CanvasArtwork] for [song] / [artist] using the embedded
     * Go backend which in turn fetches Apple Music trickplay motion artwork.
     *
     * @return A [CanvasArtwork] with [CanvasArtwork.animated] set to the M3U8 URL,
     *         or null if no canvas is available.
     */
    suspend fun getBySongArtist(song: String, artist: String): CanvasArtwork? {
        if (song.isBlank() || artist.isBlank()) return null

        return try {
            val clazz = Class.forName("gobackend.Gobackend")
            val method = runCatching {
                clazz.getMethod(
                    "fetchCanvasBySongArtist",
                    String::class.java,
                    String::class.java,
                    String::class.java,
                )
            }.getOrElse {
                clazz.getMethod(
                    "FetchCanvasBySongArtist",
                    String::class.java,
                    String::class.java,
                    String::class.java,
                )
            }
            val storefront = java.util.Locale.getDefault().country.lowercase()
            val resultJson = method.invoke(null, song, artist, storefront) as? String
            if (resultJson.isNullOrBlank()) return null

            // Parse the JSON payload returned by the Go function.
            val jsonObj = runCatching { json.parseToJsonElement(resultJson).jsonObject }.getOrNull()
                ?: return null

            fun field(key: String) = jsonObj[key]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }

            val animated = field("animated") ?: field("videoUrl") ?: return null

            CanvasArtwork(
                name    = field("name") ?: song,
                artist  = field("artist") ?: artist,
                albumId = field("albumId"),
                static  = field("static"),
                animated = animated,
                videoUrl = animated,
            )
        } catch (e: ClassNotFoundException) {
            // Backend AAR not present — silent no-op.
            null
        } catch (e: NoSuchMethodException) {
            Log.w(TAG, "fetchCanvasBySongArtist method not found in Gobackend — rebuild the AAR", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "CanvasGoProvider.getBySongArtist failed for '$song' / '$artist'", e)
            null
        }
    }
}

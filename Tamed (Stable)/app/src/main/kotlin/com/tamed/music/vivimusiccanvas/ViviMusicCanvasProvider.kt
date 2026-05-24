package com.tamed.music.vivimusiccanvas

import com.tamed.music.canvas.CanvasArtwork
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ViviMusicCanvasManifest(
    val items: List<ViviMusicCanvasItem> = emptyList()
)

@Serializable
data class ViviMusicCanvasItem(
    val song: String,
    val artist: String,
    val url: String
)

object ViviMusicCanvasProvider {
    private const val BASE_URL = "https://cdn.jsdelivr.net/gh/vivizzz007/vivimusicanvas@main/canvas.json"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = 12_000
                requestTimeoutMillis = 18_000
                socketTimeoutMillis = 18_000
            }
            install(ContentEncoding) {
                gzip()
                deflate()
            }
            install(HttpCache)
            expectSuccess = false
        }
    }

    private data class CacheEntry(
        val value: ViviMusicCanvasManifest?,
        val expiresAtMs: Long,
    )

    private var manifestCache: CacheEntry? = null
    // Cache TTL 1 minute (re-fetches json index every minute max for instant updates)
    private val ttlMs = 60_000L

    private suspend fun fetchManifest(): ViviMusicCanvasManifest? {
        val currentCache = manifestCache
        if (currentCache != null && currentCache.expiresAtMs > System.currentTimeMillis()) {
            return currentCache.value
        }

        return try {
            val manifest: ViviMusicCanvasManifest = client.get(BASE_URL).body()
            
            manifestCache = CacheEntry(
                value = manifest,
                expiresAtMs = System.currentTimeMillis() + ttlMs
            )
            manifest
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getByAlbumArtist(
        album: String,
        artist: String,
    ): CanvasArtwork? {
        if (album.isBlank() || artist.isBlank()) return null
        val manifest = fetchManifest() ?: return null

        val target = manifest.items
            .map { item ->
                var score = 0
                val artistExact = artist.equals(item.artist, ignoreCase = true)
                if (artistExact) score += 10 else return@map item to -1

                val albumExact = album.equals(item.song, ignoreCase = true)
                val albumFuzzy = !albumExact && (album.contains(item.song, ignoreCase = true) || item.song.contains(album, ignoreCase = true))

                if (albumExact) score += 50
                else if (albumFuzzy && item.song.length > 5) score += 30
                else return@map item to -1

                item to score
            }
            .filter { it.second >= 40 }
            .maxByOrNull { it.second }
            ?.first

        return target?.let {
            CanvasArtwork(
                name = it.song,
                artist = it.artist,
                videoUrl = it.url,
                animated = it.url
            )
        }
    }

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null,
    ): CanvasArtwork? {
        if (song.isBlank() || artist.isBlank()) return null
        
        val manifest = fetchManifest() ?: return null

        // Improved matching with scoring
        val target = manifest.items
            .map { item ->
                var score = 0
                
                // Artist match (required)
                val artistExact = artist.equals(item.artist, ignoreCase = true)
                if (artistExact) score += 10 else return@map item to -1
                
                // Song match
                val songExact = song.equals(item.song, ignoreCase = true)
                val songFuzzy = !songExact && (song.contains(item.song, ignoreCase = true) || item.song.contains(song, ignoreCase = true))
                
                if (songExact) score += 60
                else if (songFuzzy && item.song.length > 5) score += 20
                else if (album != null) {
                    // Try album matching if song didn't match
                    val albumExact = album.equals(item.song, ignoreCase = true)
                    val albumFuzzy = !albumExact && (album.contains(item.song, ignoreCase = true) || item.song.contains(album, ignoreCase = true))
                    if (albumExact) score += 40
                    else if (albumFuzzy && item.song.length > 5) score += 15
                    else return@map item to -1
                } else return@map item to -1
                
                item to score
            }
            .filter { it.second >= 45 } // Increased threshold
            .maxByOrNull { it.second }
            ?.first

        if (target != null) {
            return CanvasArtwork(
                name = target.song,
                artist = target.artist,
                videoUrl = target.url,
                animated = target.url
            )
        } else {
            return null
        }
    }
}

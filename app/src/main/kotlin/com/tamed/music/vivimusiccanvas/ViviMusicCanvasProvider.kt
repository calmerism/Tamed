package com.tamed.music.vivimusiccanvas

import com.tamed.music.canvas.CanvasArtwork
import com.tamed.music.canvas.normalizeForComparison
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
    val url: String,
    val album: String = "",
)

object ViviMusicCanvasProvider {
    private const val BASE_URL = "https://vivimusicanvas.mkmdevilmi.workers.dev/canvas.json"

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

        val normAlbum = album.normalizeForComparison()
        val normArtist = artist.normalizeForComparison()

        val target = manifest.items.firstOrNull { item ->
            val matchArtist = normArtist.contains(item.artist.normalizeForComparison()) ||
                    item.artist.normalizeForComparison().contains(normArtist)
            val matchAlbum = item.album.isNotBlank() && (
                    normAlbum == item.album.normalizeForComparison() ||
                    normAlbum.contains(item.album.normalizeForComparison()) ||
                    item.album.normalizeForComparison().contains(normAlbum)
            )
            matchArtist && matchAlbum
        }

        return target?.let {
            CanvasArtwork(
                name = it.song,
                artist = it.artist,
                albumName = it.album.ifBlank { album },
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

        val normSong = song.normalizeForComparison()
        val normArtist = artist.normalizeForComparison()
        val normAlbum = album?.normalizeForComparison().orEmpty()

        // 1. First attempt: match song, artist, and album if album is available
        var target = if (normAlbum.isNotBlank()) {
            manifest.items.firstOrNull { item ->
                val matchSong = normSong.contains(item.song.normalizeForComparison()) ||
                        item.song.normalizeForComparison().contains(normSong)
                val matchArtist = normArtist.contains(item.artist.normalizeForComparison()) ||
                        item.artist.normalizeForComparison().contains(normArtist)
                val matchAlbum = item.album.isBlank() || normAlbum == item.album.normalizeForComparison() ||
                        normAlbum.contains(item.album.normalizeForComparison()) ||
                        item.album.normalizeForComparison().contains(normAlbum)
                matchSong && matchArtist && matchAlbum
            }
        } else null

        // 2. Fallback: match song and artist
        if (target == null) {
            target = manifest.items.firstOrNull { item ->
                val matchSong = normSong == item.song.normalizeForComparison() ||
                        normSong.contains(item.song.normalizeForComparison()) ||
                        item.song.normalizeForComparison().contains(normSong)
                val matchArtist = normArtist.contains(item.artist.normalizeForComparison()) ||
                        item.artist.normalizeForComparison().contains(normArtist)
                matchSong && matchArtist
            }
        }

        return target?.let {
            CanvasArtwork(
                name = it.song,
                artist = it.artist,
                albumName = it.album.takeIf { a -> a.isNotBlank() } ?: album,
                videoUrl = it.url,
                animated = it.url
            )
        }
    }
}

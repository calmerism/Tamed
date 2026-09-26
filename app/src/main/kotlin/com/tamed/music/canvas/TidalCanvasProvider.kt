package com.tamed.music.canvas

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import timber.log.Timber

/**
 * A canvas provider that fetches Tidal video covers directly from api.tidal.com
 * using the public embed player token.
 */
object TidalCanvasProvider {
    private const val BASE_URL = "https://api.tidal.com/v1/"
    private const val TIDAL_TOKEN = "vNVdglQOjFJJGG2U"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }
            install(ContentEncoding) {
                gzip()
                deflate()
            }
            install(HttpCache)
            expectSuccess = false
        }
    }

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    private data class CacheEntry(
        val value: CanvasArtwork?,
        val expiresAtMs: Long
    )

    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24 // 24 hours

    private val countryCode by lazy {
        val country = Locale.getDefault().country
        if (country.length == 2) country.uppercase(Locale.ROOT) else "US"
    }

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null
    ): CanvasArtwork? {
        Timber.d("TidalCanvas: getBySongArtist called - song='$song', artist='$artist', album='$album'")
        val key = cacheKey("search_song", song, artist, album ?: "")
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let {
            Timber.d("TidalCanvas: getBySongArtist returning cached result")
            return it.value
        }

        val query = if (!album.isNullOrBlank()) "$album $artist $song" else "$artist $song"

        val result = searchOnTidal(
            query = query,
            types = "TRACKS",
            songValidation = song,
            artistValidation = artist
        )
        if (result != null) {
            cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        }
        return result
    }

    suspend fun getByAlbumArtist(
        album: String,
        artist: String
    ): CanvasArtwork? {
        Timber.d("TidalCanvas: getByAlbumArtist called - album='$album', artist='$artist'")
        val key = cacheKey("search_album", album, artist)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let {
            Timber.d("TidalCanvas: getByAlbumArtist returning cached result")
            return it.value
        }

        val result = searchOnTidal(
            query = "$album $artist",
            types = "ALBUMS",
            songValidation = null,
            artistValidation = artist,
            albumValidation = album
        )
        if (result != null) {
            cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        }
        return result
    }

    private suspend fun searchOnTidal(
        query: String,
        types: String,
        songValidation: String? = null,
        artistValidation: String? = null,
        albumValidation: String? = null
    ): CanvasArtwork? {
        try {
            Timber.d("TidalCanvas: searching query='$query', types=$types, songValidation='$songValidation', artistValidation='$artistValidation', albumValidation='$albumValidation', countryCode=$countryCode")
            val response = client.get("${BASE_URL}search") {
                header("X-Tidal-Token", TIDAL_TOKEN)
                parameter("query", query)
                parameter("limit", "10")
                parameter("types", types)
                parameter("countryCode", countryCode)
            }
            if (response.status != HttpStatusCode.OK) {
                Timber.w("TidalCanvas: API returned status ${response.status}")
                return null
            }

            val root = response.body<JsonObject>()
            val key = types.lowercase(Locale.ROOT) // "tracks" or "albums"
            val section = findSearchSection(root, key)
            if (section == null) {
                Timber.d("TidalCanvas: No '$key' section found in response")
                return null
            }
            val items = section.jsonObject["items"]?.jsonArray
            if (items == null) {
                Timber.d("TidalCanvas: No 'items' array found in section")
                return null
            }
            Timber.d("TidalCanvas: Found ${items.size} results")

            for ((index, item) in items.withIndex()) {
                val obj = item.jsonObject

                // Get result title
                val resultTitle = obj["title"]?.jsonPrimitive?.contentOrNull

                // Collect ALL artist names from the artists array
                val artistsArray = obj["artists"]?.jsonArray
                val allArtistNames = artistsArray?.mapNotNull { 
                    it.jsonObject["name"]?.jsonPrimitive?.contentOrNull 
                } ?: emptyList()
                // Build a combined artist string (e.g. "Drake, 21 Savage")
                val combinedArtistStr = if (allArtistNames.isNotEmpty()) {
                    allArtistNames.joinToString(", ")
                } else {
                    obj["artist"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
                }

                Timber.d("TidalCanvas: [$index] title='$resultTitle', artists='$combinedArtistStr'")

                // Validate track title if searching tracks
                if (songValidation != null && resultTitle != null) {
                    val rNorm = resultTitle.normalizeForComparison()
                    val sNorm = songValidation.normalizeForComparison()
                    val rClean = normalizeCanvasSongTitle(resultTitle).normalizeForComparison()
                    val sClean = normalizeCanvasSongTitle(songValidation).normalizeForComparison()
                    val titleMatches = rNorm == sNorm ||
                        (rClean.isNotBlank() && rClean == sClean) ||
                        (rClean.isNotBlank() && rClean == sNorm) ||
                        (sClean.isNotBlank() && sClean == rNorm)
                    if (!titleMatches) {
                        Timber.d("TidalCanvas: [$index] SKIP - song title mismatch: '$resultTitle' != '$songValidation'")
                        continue
                    }
                }

                // Validate album title if searching albums
                if (albumValidation != null && resultTitle != null) {
                    val rNorm = resultTitle.normalizeForComparison()
                    val aNorm = albumValidation.normalizeForComparison()
                    val albumMatches = rNorm == aNorm || rNorm.contains(aNorm) || aNorm.contains(rNorm)
                    if (!albumMatches) {
                        Timber.d("TidalCanvas: [$index] SKIP - album title mismatch: '$resultTitle' != '$albumValidation'")
                        continue
                    }
                }

                // Validate artist - strict exact match using all artists from result
                if (artistValidation != null && combinedArtistStr.isNotBlank()) {
                    val splitDelimiters = Regex("(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)", RegexOption.IGNORE_CASE)
                    val requestedList = artistValidation.split(splitDelimiters)
                        .map { it.normalizeForComparison() }
                        .filter { it.isNotBlank() }
                    // For Tidal, artists are already separate objects, so use allArtistNames directly
                    val returnedList = allArtistNames.map { it.normalizeForComparison() }
                    val artistMatches = requestedList.isNotEmpty() && returnedList.isNotEmpty() &&
                        requestedList.all { req -> returnedList.any { res -> res == req } }
                    Timber.d("TidalCanvas: [$index] artistValidation: requestedList=$requestedList, returnedList=$returnedList, matches=$artistMatches")
                    if (!artistMatches) {
                        Timber.d("TidalCanvas: [$index] SKIP - artist mismatch")
                        continue
                    }
                }

                // Retrieve videoCover
                val albumObj = if (types == "TRACKS") obj["album"]?.jsonObject else obj
                val videoCover = albumObj?.get("videoCover")?.jsonPrimitive?.contentOrNull
                val albumTitle = if (types == "TRACKS") albumObj?.get("title")?.jsonPrimitive?.contentOrNull else resultTitle

                Timber.d("TidalCanvas: [$index] videoCover='$videoCover', albumTitle='$albumTitle'")

                if (!videoCover.isNullOrBlank()) {
                    val videoUrl = formatVideoUrl(videoCover)
                    if (videoUrl != null) {
                        Timber.d("TidalCanvas: [$index] SUCCESS - returning videoUrl='$videoUrl'")
                        return CanvasArtwork(
                            name = resultTitle ?: songValidation ?: albumValidation ?: "",
                            artist = combinedArtistStr.ifBlank { artistValidation ?: "" },
                            videoUrl = videoUrl,
                            albumName = albumTitle
                        )
                    }
                } else {
                    Timber.d("TidalCanvas: [$index] No videoCover found for this result")
                }
            }
            Timber.d("TidalCanvas: No matching result with videoCover found")
        } catch (e: Exception) {
            Timber.e(e, "TidalCanvas: Exception during search")
        }
        return null
    }

    private fun findSearchSection(source: JsonElement, key: String): JsonElement? {
        if (source is JsonObject) {
            if (source.containsKey("items") && source["items"] is JsonArray) return source
            if (source.containsKey(key)) {
                val found = findSearchSection(source[key]!!, key)
                if (found != null) return found
            }
            for (value in source.values) {
                val found = findSearchSection(value, key)
                if (found != null) return found
            }
        } else if (source is JsonArray) {
            for (element in source) {
                val found = findSearchSection(element, key)
                if (found != null) return found
            }
        }
        return null
    }

    internal fun formatVideoUrl(id: String): String? {
        val parts = id.split("-")
        if (parts.size != 5) return null
        return "https://resources.tidal.com/videos/${parts[0]}/${parts[1]}/${parts[2]}/${parts[3]}/${parts[4]}/1280x1280.mp4"
    }

    private fun cacheKey(prefix: String, vararg parts: String): String {
        return "$prefix|" + parts.joinToString("|") { it.trim().lowercase(Locale.ROOT) }
    }
}

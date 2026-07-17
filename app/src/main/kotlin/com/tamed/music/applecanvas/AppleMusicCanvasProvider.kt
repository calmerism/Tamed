/*
 * Tamed Project (2026)
 * Ported from ViviMusic Project
 * Licensed Under GPL-3.0
 */

package com.tamed.music.applecanvas

import com.tamed.music.canvas.CanvasArtwork
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
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.KotlinxSerializationConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.ktor.client.statement.HttpResponse

/**
 * Fetches Apple Music album motion artwork (HLS canvas) for the album screen.
 *
 * Two extraction strategies are tried in order:
 *
 * 1. **editorialVideo** — present on albums that have Apple Motion artwork.
 *    Accessed via `?extend=editorialVideo` on the AMP albums endpoint.
 *
 * 2. **music-video tracks** — some albums embed a full-length music video as a track.
 *    Accessed via `?include=tracks`.
 *
 * Results are cached for 24 hours.
 */
object AppleMusicCanvasProvider {

    private val tokenMutex = Mutex()
    private var cachedToken: String? = null

    private suspend fun getOrFetchToken(): String {
        tokenMutex.withLock {
            cachedToken?.let { return it }
            
            try {
                Timber.d("AppleMusicCanvas: Fetching fresh developer token dynamically...")
                val html: String = client.get("https://music.apple.com/us/browse") {
                    header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                }.body()

                val jsRegex = Regex("""src="([^"]+?\.js)"""")
                val jsMatches = jsRegex.findAll(html).map { it.groupValues[1] }.toList()
                
                val jwtRegex = Regex("""\b(ey[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,})\b""")
                
                for (jsPath in jsMatches) {
                    val fullUrl = if (jsPath.startsWith("http")) jsPath else "https://music.apple.com$jsPath"
                    try {
                        val jsContent: String = client.get(fullUrl) {
                            header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                        }.body()
                        
                        val matches = jwtRegex.findAll(jsContent).map { it.groupValues[1] }
                        for (token in matches) {
                            if (token.startsWith("eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IldlYlBsYXlLaWQifQ") ||
                                token.startsWith("eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IldlYlBsYXlLaWQifQ")) {
                                Timber.d("AppleMusicCanvas: Found fresh token: ${token.take(30)}...")
                                cachedToken = token
                                return token
                            }
                        }
                    } catch (e: Exception) {
                        Timber.w("AppleMusicCanvas: failed to scan JS bundle $fullUrl: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "AppleMusicCanvas: Failed to fetch token dynamically")
            }
            
            // Fallback to currently known active token (expires July 24, 2026)
            val fallback = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IldlYlBsYXlLaWQifQ.eyJpc3MiOiJBTVBXZWJQbGF5IiwiaWF0IjoxNzgxNjY1NDYwLCJleHAiOjE3ODQ2ODk0NjAsInJvb3RfaHR0cHNfb3JpZ2luIjpbImFwcGxlLmNvbSJdfQ.G9gedBX_DjTsGMB29EXibiCYMkVfgJ3YY2TchriAqcVZB1ecZsBR1R-7WjK2RcTXLWNJYxELYzR62iShvU5hhQ"
            cachedToken = fallback
            return fallback
        }
    }

    private suspend inline fun executeWithToken(
        crossinline block: suspend (String) -> HttpResponse
    ): HttpResponse {
        var token = getOrFetchToken()
        var response = block(token)
        if (response.status == HttpStatusCode.Unauthorized) {
            Timber.d("AppleMusicCanvas: Token unauthorized, clearing cache and re-fetching...")
            tokenMutex.withLock {
                cachedToken = null
            }
            token = getOrFetchToken()
            response = block(token)
        }
        return response
    }

    private const val ITUNES_SEARCH_URL = "https://itunes.apple.com/search"
    private const val AMP_BASE_URL = "https://amp-api.music.apple.com"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json)
                // iTunes returns text/javascript for JSON responses
                register(ContentType.Text.JavaScript, KotlinxSerializationConverter(json))
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 25_000
                socketTimeoutMillis = 25_000
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
        val value: CanvasArtwork?,
        val expiresAtMs: Long,
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24 // 24 hours

    suspend fun getByAlbumArtist(
        album: String,
        artist: String,
        storefront: String = "us",
    ): CanvasArtwork? {
        Timber.d("AppleMusicCanvas: getByAlbumArtist: album='$album', artist='$artist'")
        val key = cacheKey("sa", album, artist, storefront)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.value }

        val result = searchAndFetchMotion(album, artist, album, storefront, "albums")
        if (result != null) {
            cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        }
        return result
    }

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null,
        storefront: String = "us",
    ): CanvasArtwork? {
        val key = cacheKey("song", song, artist, album ?: "", storefront)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.value }

        // Use searchAndFetchMotion which can handle song searches by resolving to albums
        val result = searchAndFetchMotion(song, artist, album, storefront, "songs")
        if (result != null) {
            cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        }
        return result
    }

    suspend fun getByAlbumId(
        albumId: String,
        storefront: String = "us",
    ): CanvasArtwork? {
        val key = cacheKey("id", albumId, storefront)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.value }

        val result = fetchMotionArtwork(albumId, storefront, null)
        cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        return result
    }

    /**
     * Searches via AMP API and tries to fetch motion artwork.
     * This is faster than iTunes search + AMP lookup.
     */
    private suspend fun searchAndFetchMotion(
        term: String,
        artist: String,
        album: String?,
        storefront: String,
        type: String, // "albums" or "songs"
    ): CanvasArtwork? {
        return runCatching {
            Timber.d("AppleMusicCanvas: searching for $type: $term (album: $album) in $storefront")
            var query = if (term.contains(artist, ignoreCase = true)) term else "$artist $term"
            if (!album.isNullOrBlank() && !query.contains(album, ignoreCase = true)) {
                query = "$query $album"
            }
            val url = "$AMP_BASE_URL/v1/catalog/$storefront/search"
            val response = executeWithToken { token ->
                client.get(url) {
                    header("Authorization", "Bearer $token")
                    header("Origin", "https://music.apple.com")
                    header("Referer", "https://music.apple.com/")
                    header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    parameter("term", query)
                    parameter("types", type)
                    parameter("limit", "10")
                    parameter("extend", "editorialVideo")
                    parameter("include", "albums")
                }
            }
            if (response.status != HttpStatusCode.OK) {
                Timber.w("AppleMusicCanvas: search failed with status ${response.status}")
                return@runCatching null
            }

            val root = response.body<JsonObject>()
            val results = root["results"]?.jsonObject?.get(type)?.jsonObject?.get("data")?.jsonArray ?: return@runCatching null
            
            // Score results for quality and edition matching
            val scoredResults = results.mapNotNull { item ->
                val obj = item.jsonObject
                val attributes = obj["attributes"]?.jsonObject ?: return@mapNotNull null
                val resultArtistName = attributes["artistName"]?.jsonPrimitive?.contentOrNull ?: ""
                val resultName = attributes["name"]?.jsonPrimitive?.contentOrNull ?: ""
                val resultCollectionName = attributes["collectionName"]?.jsonPrimitive?.contentOrNull ?: ""
                
                // --- Playlist/Set List Filtering ---
                // We should never use playlist animations as album canvas.
                val nameLower = resultName.lowercase(Locale.ROOT)
                val collectionLower = resultCollectionName.lowercase(Locale.ROOT)
                val isBlacklisted = nameLower.contains("playlist") || nameLower.contains("set list") ||
                        collectionLower.contains("playlist") || collectionLower.contains("set list") ||
                        nameLower.contains("essentials") || collectionLower.contains("essentials") ||
                        collectionLower.contains("dj mix") || collectionLower.contains("mixed") ||
                        collectionLower.contains("apple music") || collectionLower.contains("today's hits") ||
                        nameLower.contains("session") || collectionLower.contains("session")
                
                if (isBlacklisted) {
                    Timber.d("AppleMusicCanvas:   - Skipping blacklisted result: '$resultName' (Album: '$resultCollectionName')")
                    return@mapNotNull null
                }

                // Strict artist check: result must contain requested artist or vice versa
                val artistMatch = resultArtistName.equals(artist, ignoreCase = true)
                val artistFuzzy = resultArtistName.contains(artist, ignoreCase = true) || artist.contains(resultArtistName, ignoreCase = true)
                
                if (!artistFuzzy) return@mapNotNull null
                
                var score = 0
                if (artistMatch) score += 15
                else score += 5
                
                // Name matching (Song or Album title)
                val nameMatch = resultName.equals(term, ignoreCase = true)
                val nameFuzzy = resultName.contains(term, ignoreCase = true) || term.contains(resultName, ignoreCase = true)
                
                if (nameMatch) {
                    score += 25
                } else if (nameFuzzy) {
                    score += 12
                } else {
                    // If name doesn't match at all, this is a strong indicator of wrong result
                    score -= 30
                }

                // Special editions handling (Deluxe, Expanded, etc)
                val editionWords = listOf("deluxe", "expanded", "remastered", "remix", "version", "edit", "mix", "bonus")
                for (word in editionWords) {
                    val inTerm = term.contains(word, ignoreCase = true)
                    val inResult = resultName.contains(word, ignoreCase = true)
                    if (inTerm && inResult) score += 5
                    else if (inTerm != inResult && inResult) score -= 5 // Penalty for unexpected "Deluxe" etc.
                }

                // Album matching - very strong signal
                if (!album.isNullOrBlank() && resultCollectionName.isNotBlank()) {
                    val albumMatch = resultCollectionName.equals(album, ignoreCase = true)
                    val albumFuzzy = resultCollectionName.contains(album, ignoreCase = true) || album.contains(resultCollectionName, ignoreCase = true)
                    
                    if (albumMatch) score += 35
                    else if (albumFuzzy) score += 15
                    else score -= 40 // Increased penalty for album mismatch
                }
                
                Timber.d("AppleMusicCanvas:   - Result: '$resultName' by '$resultArtistName' (Album: '$resultCollectionName', ID: ${obj["id"]}) -> Score: $score")
                score to item
            }.sortedByDescending { it.first }
            
            Timber.d("AppleMusicCanvas: Found ${scoredResults.size} scored results for term '$term'")
            
            // Try results until we find motion or exhaustion
            for ((score, item) in scoredResults) {
                if (score < 25) { // Raised minimum score for safer matching
                    Timber.d("AppleMusicCanvas: skipping result with low score: $score")
                    continue
                }
                val obj = item.jsonObject
                val attributes = obj["attributes"]?.jsonObject ?: continue
                val resultName = attributes["name"]?.jsonPrimitive?.contentOrNull ?: ""
                val resultArtistName = attributes["artistName"]?.jsonPrimitive?.contentOrNull ?: ""

                // 1. Resolve Album ID
                var targetAlbumId: String? = null
                val type = obj["type"]?.jsonPrimitive?.contentOrNull
                if (type == "songs") {
                    val relationships = obj["relationships"]?.jsonObject
                    targetAlbumId = relationships?.get("albums")?.jsonObject?.get("data")?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
                        ?: attributes["collectionId"]?.jsonPrimitive?.contentOrNull
                    
                    // Fallback: Parse from URL if possible
                    if (targetAlbumId == null) {
                        val url = attributes["url"]?.jsonPrimitive?.contentOrNull
                        if (url != null) {
                            // URL format: https://music.apple.com/region/album/name/ID?i=songId
                            val albumPart = url.substringAfter("/album/", "").substringBefore("?")
                            val id = albumPart.substringAfterLast("/", "")
                            if (id.isNotBlank() && id.all { it.isDigit() }) {
                                targetAlbumId = id
                            }
                        }
                    }
                    
                    if (targetAlbumId == null) {
                        Timber.d("AppleMusicCanvas: relationships keys for $resultName: ${relationships?.keys}")
                    }
                } else if (type == "albums") {
                    targetAlbumId = obj["id"]?.jsonPrimitive?.contentOrNull
                }

                if (targetAlbumId == null || targetAlbumId.startsWith("pl.")) {
                    Timber.d("AppleMusicCanvas: skipping null or playlist albumId ($targetAlbumId) for $resultName ($resultArtistName)")
                    continue
                }

                Timber.d("AppleMusicCanvas: trying resolve for $targetAlbumId (from ${obj["type"]?.jsonPrimitive?.contentOrNull})")

                // 2. Check for immediate motion in search result
                val ev = attributes["editorialVideo"]?.jsonObject
                if (ev != null) {
                    val hlsUrl = extractEditorialVideoUrl(ev)
                    if (!hlsUrl.isNullOrBlank()) {
                        val name = attributes["name"]?.jsonPrimitive?.contentOrNull
                        val collName = attributes["collectionName"]?.jsonPrimitive?.contentOrNull
                        // If this is a song result, use song name as name and collection as albumName
                        // If this is an album result, use album name as both name and albumName
                        val resolvedAlbumName = if (type == "songs") collName else name
                        Timber.d("AppleMusicCanvas: Found direct editorialVideo for $name (ID: $targetAlbumId)")
                        return@runCatching CanvasArtwork(name, resultArtistName, targetAlbumId, albumName = resolvedAlbumName, animated = hlsUrl)
                    }
                }

                // 3. Full lookup with metadata preservation
                val fetched = fetchMotionArtwork(
                    albumId = targetAlbumId,
                    storefront = storefront,
                    fallbackArtist = resultArtistName,
                    titleOverride = if (type == "songs") attributes["name"]?.jsonPrimitive?.contentOrNull else null,
                    artistOverride = if (type == "songs") resultArtistName else null
                )
                if (fetched != null) return@runCatching fetched
            }
            Timber.d("AppleMusicCanvas: no canvas found in resolution/lookup for $term after ${scoredResults.size} results")
            null
        }.onFailure {
            if (it is CancellationException) throw it
            Timber.e(it, "AppleMusicCanvas: error in searchAndFetchMotion for $term")
        }.getOrNull()
    }

    private suspend fun fetchMotionArtwork(
        albumId: String,
        storefront: String,
        fallbackArtist: String? = null,
        titleOverride: String? = null,
        artistOverride: String? = null,
    ): CanvasArtwork? {
        if (albumId.startsWith("pl.")) {
            Timber.d("AppleMusicCanvas: fetchMotionArtwork: ignoring playlist id $albumId")
            return null
        }
        return runCatching {
            Timber.d("AppleMusicCanvas: fetching album $albumId")
            val url = "$AMP_BASE_URL/v1/catalog/$storefront/albums/$albumId"
            val response = executeWithToken { token ->
                client.get(url) {
                    header("Authorization", "Bearer $token")
                    header("Origin", "https://music.apple.com")
                    header("Referer", "https://music.apple.com/")
                    header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    parameter("extend", "editorialVideo")
                    parameter("include", "tracks")
                }
            }
            if (response.status != HttpStatusCode.OK) {
                Timber.w("AppleMusicCanvas: album fetch failed for $albumId: ${response.status}")
                return@runCatching null
            }

            val root = response.body<JsonObject>()
            val data = root["data"]?.jsonArray
            if (data.isNullOrEmpty()) return@runCatching null
            
            val albumObj = data.firstOrNull()?.jsonObject ?: return@runCatching null
            val attributes = albumObj["attributes"]?.jsonObject
            val albumName = attributes?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
            val artistName = attributes?.get("artistName")?.jsonPrimitive?.contentOrNull ?: fallbackArtist
            
            // --- Playlist/Station Filtering ---
            val nameLower = albumName.lowercase(Locale.ROOT)
            val isBlacklisted = nameLower.contains("playlist") || nameLower.contains("set list") ||
                    nameLower.contains("essentials") || nameLower.contains("dj mix") ||
                    nameLower.contains("mixed") || nameLower.contains("apple music") ||
                    nameLower.contains("today's hits") || nameLower.contains("session")
            
            if (isBlacklisted) {
                Timber.d("AppleMusicCanvas: fetchMotionArtwork: ignoring blacklisted album '$albumName' ($albumId)")
                return@runCatching null
            }

            // titleOverride is the song name (when searching by song), albumName is always the album name
            val finalTitle = titleOverride ?: albumName
            val finalArtist = artistOverride ?: artistName

            // Strategy 1: editorialVideo
            val ev = attributes?.get("editorialVideo")?.jsonObject
            if (ev != null) {
                val url = extractEditorialVideoUrl(ev)
                if (!url.isNullOrBlank()) {
                    Timber.d("AppleMusicCanvas: found editorialVideo for $finalTitle (album: $albumName, id: $albumId)")
                    return@runCatching CanvasArtwork(finalTitle, finalArtist, albumId, albumName = albumName, animated = url)
                }
            }

            Timber.d("AppleMusicCanvas: no editorialVideo for $albumId (available keys: ${attributes?.keys})")
            null
        }.onFailure {
            if (it is CancellationException) throw it
            Timber.e(it, "AppleMusicCanvas: error in fetchMotionArtwork for $albumId")
        }.getOrNull()
    }

    private fun extractEditorialVideoUrl(ev: JsonObject): String? {
        val assets = listOf(
            ev["motionDetailRaw"]?.jsonObject,
            ev["motionDetailSquare"]?.jsonObject,
            ev["motionDetailTall"]?.jsonObject,
            ev["motionDetailStatic"]?.jsonObject // Fallback
        ).filterNotNull()
        
        for (asset in assets) {
            // Try different possible keys for the video URL
            val video = asset["video"]?.jsonPrimitive?.contentOrNull
                ?: asset["videoUrl"]?.jsonPrimitive?.contentOrNull
                ?: asset["hlsUrl"]?.jsonPrimitive?.contentOrNull
                ?: asset["url"]?.jsonPrimitive?.contentOrNull
            
            if (!video.isNullOrBlank()) return video
        }

        Timber.d("AppleMusicCanvas: editorialVideo found but no video link in assets: ${ev.keys}")
        return null
    }

    private fun cacheKey(prefix: String, vararg parts: String): String {
        return "$prefix|" + parts.joinToString("|") { it.trim().lowercase(Locale.ROOT) }
    }
}

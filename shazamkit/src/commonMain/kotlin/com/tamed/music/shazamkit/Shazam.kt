package com.tamed.music.shazamkit

import com.tamed.music.shazamkit.models.RecognitionResult
import com.tamed.music.shazamkit.models.ShazamRequestJson
import com.tamed.music.shazamkit.models.ShazamResponseJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Shazam music recognition with built-in rate limiting and queue management.
 */
object Shazam {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private const val MAX_CONCURRENT_REQUESTS = 2
    private const val MIN_REQUEST_INTERVAL_MS = 1000L
    private const val MAX_RETRIES = 3
    private const val INITIAL_RETRY_DELAY_MS = 2000L
    private const val CACHE_DURATION_MS = 300000L
    private const val MAX_QUEUE_SIZE = 50

    private var activeRequests = 0
    private var lastRequestTime = 0L
    private var nextRequestId = 0L
    private var isProcessingQueue = false

    private val stateMutex = Mutex()
    private val requestQueue = ArrayDeque<PendingRequest>()
    private val resultCache = mutableMapOf<String, CachedResult>()

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            expectSuccess = false
        }
    }

    private val userAgents = listOf(
        "Dalvik/2.1.0 (Linux; U; Android 5.0.2; VS980 4G Build/LRX22G)",
        "Dalvik/1.6.0 (Linux; U; Android 4.4.2; SM-T210 Build/KOT49H)",
        "Dalvik/2.1.0 (Linux; U; Android 5.1.1; SM-P905V Build/LMY47X)",
        "Dalvik/2.1.0 (Linux; U; Android 6.0.1; SM-G920F Build/MMB29K)",
        "Dalvik/2.1.0 (Linux; U; Android 5.0; SM-G900F Build/LRX21T)",
    )

    private val timezones = listOf(
        "Europe/Paris", "Europe/London", "America/New_York",
        "America/Los_Angeles", "Asia/Tokyo", "Asia/Dubai",
    )

    suspend fun recognize(signature: String, sampleDurationMs: Long): Result<RecognitionResult> {
        val cacheKey = generateCacheKey(signature)
        getCachedResult(cacheKey)?.let {
            return Result.success(it)
        }

        return enqueueRequest(signature, sampleDurationMs)
    }

    suspend fun getPendingRequestsCount(): Int = stateMutex.withLock { requestQueue.size }

    suspend fun getActiveRequestsCount(): Int = stateMutex.withLock { activeRequests }

    suspend fun clearCache() {
        stateMutex.withLock {
            resultCache.clear()
        }
    }

    suspend fun cancelPendingRequests() {
        stateMutex.withLock {
            requestQueue.clear()
        }
    }

    suspend fun cleanup() {
        cancelPendingRequests()
        clearCache()
        client.close()
    }

    private suspend fun enqueueRequest(
        signature: String,
        sampleDurationMs: Long,
    ): Result<RecognitionResult> {
        val request = stateMutex.withLock {
            if (requestQueue.size >= MAX_QUEUE_SIZE) {
                return Result.failure(Exception("Request queue is full. Please wait."))
            }

            PendingRequest(
                id = nextRequestId++,
                signature = signature,
                sampleDurationMs = sampleDurationMs,
            ).also { requestQueue.addLast(it) }
        }

        startProcessorIfNeeded()
        return request.awaitResult()
    }

    private suspend fun startProcessorIfNeeded() {
        val shouldStart = stateMutex.withLock {
            if (isProcessingQueue) {
                false
            } else {
                isProcessingQueue = true
                true
            }
        }

        if (shouldStart) {
            scope.launch {
                processQueue()
            }
        }
    }

    private suspend fun processQueue() {
        while (true) {
            val request = waitForNextRequest() ?: break

            scope.launch {
                try {
                    val result = executeRequest(request.signature, request.sampleDurationMs)
                    request.completeWith(result)
                } catch (e: Exception) {
                    request.completeWith(Result.failure(e))
                } finally {
                    stateMutex.withLock {
                        activeRequests--
                    }
                }
            }
        }

        stateMutex.withLock {
            isProcessingQueue = false
            if (requestQueue.isNotEmpty()) {
                isProcessingQueue = true
                scope.launch { processQueue() }
            }
        }
    }

    private suspend fun waitForNextRequest(): PendingRequest? {
        while (true) {
            val request = stateMutex.withLock {
                if (activeRequests < MAX_CONCURRENT_REQUESTS && requestQueue.isNotEmpty()) {
                    activeRequests++
                    requestQueue.removeFirst()
                } else {
                    null
                }
            }
            if (request != null) return request

            val done = stateMutex.withLock {
                requestQueue.isEmpty() && activeRequests == 0
            }
            if (done) return null

            delay(50)
        }
    }

    private suspend fun executeRequest(
        signature: String,
        sampleDurationMs: Long,
    ): Result<RecognitionResult> {
        var lastException: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                enforceRateLimit()

                val result = performRecognition(signature, sampleDurationMs)

                val cacheKey = generateCacheKey(signature)
                cacheResult(cacheKey, result)

                return Result.success(result)
            } catch (e: Exception) {
                lastException = e

                if (
                    e.message?.contains("429") == true ||
                    e.message?.contains("Too many requests", ignoreCase = true) == true
                ) {
                    if (attempt < MAX_RETRIES - 1) {
                        delay(calculateBackoffDelay(attempt))
                    }
                } else {
                    throw e
                }
            }
        }

        throw lastException ?: Exception("Recognition failed after $MAX_RETRIES attempts")
    }

    private suspend fun performRecognition(
        signature: String,
        sampleDurationMs: Long,
    ): RecognitionResult {
        val timestamp = currentTimeMillis() / 1000
        val uuid1 = randomUuid().uppercase()
        val uuid2 = randomUuid()

        val request = ShazamRequestJson(
            geolocation = ShazamRequestJson.Geolocation(
                altitude = Random.nextDouble() * 400 + 100,
                latitude = Random.nextDouble() * 180 - 90,
                longitude = Random.nextDouble() * 360 - 180,
            ),
            signature = ShazamRequestJson.Signature(
                samplems = sampleDurationMs,
                timestamp = timestamp,
                uri = signature,
            ),
            timestamp = timestamp,
            timezone = timezones.random(),
        )

        val response = client.post("https://amp.shazam.com/discovery/v5/en/US/android/-/tag/$uuid1/$uuid2") {
            parameter("sync", "true")
            parameter("webv3", "true")
            parameter("sampling", "true")
            parameter("connected", "")
            parameter("shazamapiversion", "v3")
            parameter("sharehub", "true")
            parameter("video", "v3")
            header("User-Agent", userAgents.random())
            header("Content-Language", "en_US")
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        val responseBody = try {
            response.bodyAsText()
        } catch (e: Exception) {
            "Could not read body: ${e.message}"
        }
        println("[SHAZAM_DEBUG] Response status: ${response.status}, body: $responseBody")

        if (!response.status.isSuccess()) {
            when (val statusCode = response.status.value) {
                429 -> throw Exception("Too many requests")
                404 -> throw Exception("No match found")
                in 500..599 -> throw Exception("Shazam service temporarily unavailable")
                else -> throw Exception("Recognition failed (error $statusCode)")
            }
        }

        val shazamResponse = try {
            json.decodeFromString<ShazamResponseJson>(responseBody)
        } catch (e: Exception) {
            println("[SHAZAM_DEBUG] JSON decode failed: ${e.message}")
            throw e
        }

        return shazamResponse.toRecognitionResult()
            ?: throw Exception("No match found")
    }

    private suspend fun enforceRateLimit() {
        val delayMs = stateMutex.withLock {
            val now = currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < MIN_REQUEST_INTERVAL_MS) {
                MIN_REQUEST_INTERVAL_MS - elapsed
            } else {
                0L
            }
        }

        if (delayMs > 0) {
            delay(delayMs)
        }

        stateMutex.withLock {
            lastRequestTime = currentTimeMillis()
        }
    }

    private fun calculateBackoffDelay(attempt: Int): Long {
        return INITIAL_RETRY_DELAY_MS * (1 shl attempt)
    }

    private fun generateCacheKey(signature: String): String {
        return signature.hashCode().toString()
    }

    private suspend fun getCachedResult(key: String): RecognitionResult? {
        return stateMutex.withLock {
            val cached = resultCache[key] ?: return@withLock null
            val now = currentTimeMillis()
            if (now - cached.timestamp > CACHE_DURATION_MS) {
                resultCache.remove(key)
                null
            } else {
                cached.result
            }
        }
    }

    private suspend fun cacheResult(
        key: String,
        result: RecognitionResult,
    ) {
        stateMutex.withLock {
            resultCache[key] = CachedResult(
                timestamp = currentTimeMillis(),
                result = result,
            )

            if (resultCache.size >= 100) {
                val now = currentTimeMillis()
                val expiredKeys = resultCache.filterValues { now - it.timestamp > CACHE_DURATION_MS }.keys.toList()
                expiredKeys.forEach(resultCache::remove)
            }
        }
    }

    private fun ShazamResponseJson.toRecognitionResult(): RecognitionResult? {
        val track = this.track ?: return null

        val songSection = track.sections?.find { it?.type == "SONG" }
        val metadata = songSection?.metadata
        val album = metadata?.find { it?.title == "Album" }?.text
        val label = metadata?.find { it?.title == "Label" }?.text
        val releaseDate = metadata?.find { it?.title == "Released" }?.text

        val lyricsSection = track.sections?.find { it?.type == "LYRICS" }
        val lyrics = lyricsSection?.text

        val appleAction = track.hub?.options?.firstOrNull {
            it?.providername?.contains("apple", ignoreCase = true) == true
        }?.actions?.firstOrNull()

        val spotifyProvider = track.hub?.providers?.find {
            it?.caption?.contains("spotify", ignoreCase = true) == true
        }

        val youtubeAction = track.hub?.options?.find {
            it?.type?.contains("video", ignoreCase = true) == true
        }?.actions?.firstOrNull()

        val youtubeVideoId = youtubeAction?.uri?.let { uri ->
            uri.substringAfterLast("v=", "").takeIf { it.isNotEmpty() }
                ?: uri.substringAfterLast("/", "").takeIf { it.isNotEmpty() && it.length == 11 }
        }

        return RecognitionResult(
            trackId = track.key ?: tagid ?: "",
            title = track.title ?: "",
            artist = track.subtitle ?: "",
            album = album,
            coverArtUrl = track.images?.coverart,
            coverArtHqUrl = track.images?.coverarthq,
            genre = track.genres?.primary,
            releaseDate = releaseDate,
            label = label,
            lyrics = lyrics,
            shazamUrl = track.url,
            appleMusicUrl = appleAction?.uri,
            spotifyUrl = spotifyProvider?.actions?.firstOrNull()?.uri,
            isrc = track.isrc,
            youtubeVideoId = youtubeVideoId,
        )
    }

    private class PendingRequest(
        val id: Long,
        val signature: String,
        val sampleDurationMs: Long,
    ) {
        private val mutex = Mutex()
        private var result: Result<RecognitionResult>? = null

        suspend fun awaitResult(): Result<RecognitionResult> {
            while (true) {
                mutex.withLock {
                    result?.let { return it }
                }
                delay(50)
            }
        }

        fun completeWith(result: Result<RecognitionResult>) {
            scope.launch {
                mutex.withLock {
                    this@PendingRequest.result = result
                }
            }
        }
    }

    private data class CachedResult(
        val timestamp: Long,
        val result: RecognitionResult,
    )
}

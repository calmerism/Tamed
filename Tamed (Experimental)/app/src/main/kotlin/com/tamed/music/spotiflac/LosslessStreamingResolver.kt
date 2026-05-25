/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.spotiflac

import android.util.Log
import com.tamed.music.constants.LosslessStreamingProvider
import com.tamed.music.constants.LosslessStreamingQuality
import com.tamed.music.constants.QobuzBackend
import com.tamed.music.models.MediaMetadata
import org.json.JSONObject
import java.util.Locale

object LosslessStreamingResolver {
    private const val TAG = "LosslessStreamResolver"

    /** In-memory cache of resolved metadata hints, keyed by "mediaId:providerName". */
    private val metadataHintCache = java.util.concurrent.ConcurrentHashMap<String, SpotiFlacDownloader.ResolvedTrackMetadata>()

    data class ResolvedStream(
        val service: String,
        val url: String,
        val mimeType: String,
        val codecs: String,
        val bitDepth: Int,
        val sampleRate: Int,
    )

    private fun buildRequest(
        provider: LosslessStreamingProvider,
        quality: LosslessStreamingQuality,
        qobuzBackend: QobuzBackend,
        qobuzCountry: String,
        mediaMetadata: MediaMetadata,
        title: String,
        artists: List<String>,
        metadataHint: SpotiFlacDownloader.ResolvedTrackMetadata? = null,
    ): JSONObject =
        JSONObject().apply {
            val normalizedCountry =
                qobuzCountry.trim().uppercase(Locale.US).takeIf { it.matches(Regex("[A-Z]{2}")) } ?: "US"
            put("service", provider.service.orEmpty())
            put("quality", quality.requestValue)
            put("track_name", metadataHint?.name?.takeIf { it.isNotBlank() } ?: title)
            put("artist_name", metadataHint?.artists?.takeIf { it.isNotBlank() } ?: artists.joinToString(", "))
            put("album_name", metadataHint?.albumName?.takeIf { it.isNotBlank() } ?: mediaMetadata.album?.title.orEmpty())
            put("isrc", metadataHint?.isrc.orEmpty())
            put("qobuz_id", metadataHint?.qobuzId.orEmpty())
            put("tidal_id", metadataHint?.tidalId.orEmpty())
            put("duration_ms", mediaMetadata.duration.takeIf { it > 0 }?.times(1000) ?: 0)
            put("qobuz_backend", qobuzBackend.name)
            put("qobuz_country", normalizedCountry)
        }

    private fun parseResolvedStream(
        provider: LosslessStreamingProvider,
        responseJson: String?,
        attemptLabel: String,
    ): ResolvedStream? {
        if (responseJson == null) {
            Log.w(TAG, "resolvePlaybackStreamJSON returned null for $attemptLabel")
            return null
        }

        val response = runCatching { JSONObject(responseJson) }.getOrNull()
        if (response == null) {
            Log.w(TAG, "Could not parse response JSON for $attemptLabel: $responseJson")
            return null
        }
        if (!response.optBoolean("success")) {
            Log.w(TAG, "Backend returned success=false for $attemptLabel: ${response.optString("error", responseJson)}")
            return null
        }

        val streamUrl = response.optString("stream_url").trim()
        if (streamUrl.isBlank()) {
            Log.w(TAG, "Backend returned empty stream_url for $attemptLabel")
            return null
        }

        Log.d(
            TAG,
            "Resolved stream ($attemptLabel): service=${response.optString("service")}, mime=${response.optString("mime_type")}, sampleRate=${response.optInt("sample_rate")}",
        )

        return ResolvedStream(
            service = response.optString("service").ifBlank { provider.service.orEmpty() },
            url = streamUrl,
            mimeType = response.optString("mime_type").ifBlank { "audio/flac" },
            codecs = response.optString("codecs").ifBlank { "flac" },
            bitDepth = response.optInt("bit_depth"),
            sampleRate = response.optInt("sample_rate"),
        )
    }

    private fun resolveRequest(
        backend: SpotiFlacDownloader.Backend,
        provider: LosslessStreamingProvider,
        request: JSONObject,
        attemptLabel: String,
    ): ResolvedStream? {
        Log.d(TAG, "Request JSON ($attemptLabel): $request")
        val responseJson =
            runCatching { backend.resolvePlaybackStreamJSON(request.toString()) }
                .onFailure { Log.e(TAG, "resolvePlaybackStreamJSON threw for $attemptLabel", it) }
                .getOrNull()
        return parseResolvedStream(provider, responseJson, attemptLabel)
    }

    suspend fun resolve(
        mediaMetadata: MediaMetadata,
        provider: LosslessStreamingProvider,
        quality: LosslessStreamingQuality,
        qobuzBackend: QobuzBackend = QobuzBackend.JUMO,
        qobuzCountry: String = "US",
    ): ResolvedStream? {
        if (provider == LosslessStreamingProvider.OFF) return null

        val backend = SpotiFlacDownloader.spotiFlacGoBackend
        if (backend == null) {
            Log.w(TAG, "Go backend is null — gobackend.aar missing or class load failed")
            return null
        }

        val title = mediaMetadata.title.trim()
        val artists = mediaMetadata.artists.map { it.name.trim() }.filter { it.isNotBlank() }
        if (title.isBlank() || artists.isEmpty()) {
            Log.w(TAG, "Skipping: title='$title', artists=$artists")
            return null
        }

        Log.d(TAG, "Resolving: title='$title', artists=${artists.joinToString()}, provider=${provider.name}, quality=${quality.name}")

        // Check in-memory cache first — instant return for replayed songs
        val cacheKey = "${mediaMetadata.id}:${provider.name}"
        val cachedHint = metadataHintCache[cacheKey]

        val metadataHint = if (cachedHint != null) {
            Log.d(TAG, "Cache HIT for '$cacheKey' — skipping network metadata search")
            cachedHint
        } else {
            SpotiFlacDownloader.resolveTrackMetadataHint(
                backend = backend,
                title = title,
                artists = artists,
                albumTitle = mediaMetadata.album?.title.orEmpty(),
                durationSeconds = mediaMetadata.duration,
                preferredProvider = SpotiFlacProvider.QOBUZ,
            )?.also {
                metadataHintCache[cacheKey] = it
                Log.d(TAG, "Cache MISS for '$cacheKey' — stored resolved hint")
            }
        }

        if (metadataHint != null) {
            Log.d(TAG, "Metadata hint: name='${metadataHint.name}', qobuzId='${metadataHint.qobuzId}', isrc='${metadataHint.isrc}'")
        } else {
            Log.d(TAG, "No metadata hint found (score < 85 or no candidates)")
        }
        val directRequest = buildRequest(
            provider = provider,
            quality = quality,
            qobuzBackend = qobuzBackend,
            qobuzCountry = qobuzCountry,
            mediaMetadata = mediaMetadata,
            title = title,
            artists = artists,
        )

        if (metadataHint != null) {
            val hintedRequest = buildRequest(
                provider = provider,
                quality = quality,
                qobuzBackend = qobuzBackend,
                qobuzCountry = qobuzCountry,
                mediaMetadata = mediaMetadata,
                title = title,
                artists = artists,
                metadataHint = metadataHint,
            )
            resolveRequest(
                backend = backend,
                provider = provider,
                request = hintedRequest,
                attemptLabel = "hinted",
            )?.let { return it }

            if (hintedRequest.toString() == directRequest.toString()) {
                return null
            }
        }

        return resolveRequest(
            backend = backend,
            provider = provider,
            request = directRequest,
            attemptLabel = "direct",
        )
    }
}

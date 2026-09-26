package com.tamed.music.playback

import android.media.MediaDataSource
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.ContentMetadata
import java.io.IOException

@UnstableApi
object AudioCache {
    private const val TAG = "TamedAudioCache"

    @Volatile
    private var cache: Cache? = null

    fun init(playerCache: Cache) {
        cache = playerCache
    }

    data class Rendition(val key: String, val contentLength: Long, val cachedPrefix: Long) {
        val isComplete: Boolean get() = contentLength > 0 && cachedPrefix >= contentLength
    }

    fun renditionsOf(uri: Uri): List<Rendition> {
        val c = cache ?: return emptyList()
        val videoId = uri.getQueryParameter("v") ?: uri.toString()
        val matchingKeys = c.keys.filter { it == videoId || it.startsWith("$videoId#") }
        return matchingKeys.mapNotNull { key ->
            val contentLength = ContentMetadata.getContentLength(c.getContentMetadata(key)).coerceAtLeast(0L)
            val probe = if (contentLength > 0) contentLength else 256 * 1024L
            val prefix = c.getCachedLength(key, 0, probe).coerceAtLeast(0L)
            if (prefix <= 0L) null else Rendition(key, contentLength, prefix)
        }.sortedBy { it.contentLength }
    }

    fun cacheKeyOf(uri: Uri): String {
        return uri.getQueryParameter("v") ?: uri.toString()
    }

    fun isUsable(rendition: Rendition): Boolean = rendition.cachedPrefix > 0

    fun cachedPrefix(cacheKey: String): Long {
        val c = cache ?: return 0L
        return c.getCachedLength(cacheKey, 0, 1024 * 1024 * 100L).coerceAtLeast(0L)
    }

    fun contentLength(cacheKey: String): Long {
        val c = cache ?: return 0L
        return ContentMetadata.getContentLength(c.getContentMetadata(cacheKey)).coerceAtLeast(0L)
    }

    fun requestAnalysisHead(uri: Uri, minBytes: Long = 1024 * 1024L) {
        // In Tamed, ExoPlayer and ResolvingDataSource cache audio chunks as played or prefetched.
    }

    fun discardBadRendition(uri: Uri, cacheKey: String): Boolean {
        val c = cache ?: return false
        return runCatching {
            c.removeResource(cacheKey)
            true
        }.getOrDefault(false)
    }

    fun renditionDataSource(uri: Uri, rendition: Rendition): MediaDataSource {
        val c = checkNotNull(cache) { "AudioCache not initialized" }
        return CacheMediaDataSource(
            CacheDataSource.Factory()
                .setCache(c)
                .setUpstreamDataSourceFactory(NoUpstream)
                .setCacheKeyFactory { rendition.key }
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                .createDataSource(),
            uri
        )
    }

    private object NoUpstream : DataSource.Factory {
        override fun createDataSource(): DataSource = object : DataSource {
            override fun addTransferListener(transferListener: TransferListener) {}
            override fun open(dataSpec: DataSpec): Long =
                throw IOException("Automix analysis reads only cached bytes; no upstream is wired up")
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                throw IOException("Automix analysis reads only cached bytes; no upstream is wired up")
            override fun getUri(): Uri? = null
            override fun close() {}
        }
    }

    private class CacheMediaDataSource(
        private val dataSource: DataSource,
        private val uri: Uri,
    ) : MediaDataSource() {
        private var isOpen = false
        private var openPosition = -1L

        override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
            if (size == 0) return 0
            if (!isOpen || position != openPosition) {
                closeUpstream()
                val available = try {
                    dataSource.open(DataSpec.Builder().setUri(uri).setPosition(position).build())
                } catch (error: IOException) {
                    return -1
                }
                isOpen = true
                openPosition = position
                if (available == 0L) return -1
            }
            val count = try {
                dataSource.read(buffer, offset, size)
            } catch (error: IOException) {
                closeUpstream()
                return -1
            }
            if (count == C.RESULT_END_OF_INPUT) {
                closeUpstream()
                return -1
            }
            openPosition += count
            return count
        }

        override fun getSize(): Long {
            closeUpstream()
            return try {
                dataSource.open(DataSpec(uri))
            } catch (error: IOException) {
                -1L
            } finally {
                closeUpstream()
            }
        }

        override fun close() {
            closeUpstream()
        }

        private fun closeUpstream() {
            if (isOpen) {
                runCatching { dataSource.close() }
                isOpen = false
            }
        }
    }
}

package com.tamed.music.utils

import android.content.Context
import android.util.LruCache
import com.tamed.music.innertube.pages.ArtistPage
import com.tamed.music.ui.utils.resize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Fast in-memory cache for artist metadata, pages, and profile thumbnails.
 * Eliminates screen-opening delay and enables instant thumbnail & name rendering.
 */
object ArtistMemoryCache {
    private val pageCache = LruCache<String, ArtistPage>(40)
    private val thumbnailCache = ConcurrentHashMap<String, String>()
    private val nameCache = ConcurrentHashMap<String, String>()

    fun getPage(artistId: String): ArtistPage? = pageCache.get(artistId)

    fun putPage(artistId: String, page: ArtistPage) {
        pageCache.put(artistId, page)
        page.artist.thumbnail?.let { thumbnailCache[artistId] = it }
        page.artist.title.let { nameCache[artistId] = it }
    }

    fun getThumbnail(artistId: String): String? = thumbnailCache[artistId]

    fun putThumbnail(artistId: String?, thumbnailUrl: String?) {
        if (!artistId.isNullOrBlank() && !thumbnailUrl.isNullOrBlank()) {
            thumbnailCache[artistId] = thumbnailUrl
        }
    }

    fun getName(artistId: String): String? = nameCache[artistId]

    fun putName(artistId: String?, name: String?) {
        if (!artistId.isNullOrBlank() && !name.isNullOrBlank()) {
            nameCache[artistId] = name
        }
    }

    fun putArtist(artistId: String?, name: String?, thumbnailUrl: String?) {
        if (artistId.isNullOrBlank()) return
        putName(artistId, name)
        putThumbnail(artistId, thumbnailUrl)
        if (!thumbnailUrl.isNullOrBlank()) {
            com.tamed.music.ui.theme.BackdropCache.put(artistId, thumbnailUrl)
        }
    }

    fun preload(context: Context? = null, artistId: String?, name: String?, thumbnailUrl: String?) {
        if (artistId.isNullOrBlank()) return
        putArtist(artistId, name, thumbnailUrl)
        if (context != null && !thumbnailUrl.isNullOrBlank()) {
            try {
                val resized = thumbnailUrl.resize(800, 800)
                val request = coil3.request.ImageRequest.Builder(context)
                    .data(resized)
                    .build()
                coil3.SingletonImageLoader.get(context).enqueue(request)
            } catch (_: Throwable) {}
        }
        if (!name.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    com.tamed.music.ui.screens.artist.ArtistLogoRegistry.getLogo(name, artistId)
                } catch (_: Throwable) {}
            }
        }
    }
}

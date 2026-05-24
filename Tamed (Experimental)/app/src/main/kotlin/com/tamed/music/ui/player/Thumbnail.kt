/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.ui.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.media3.common.C
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import androidx.compose.material3.Icon
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.R
import com.tamed.music.canvas.CanvasArtwork
import com.tamed.music.constants.PlayerBackgroundStyle
import com.tamed.music.constants.PlayerBackgroundStyleKey
import com.tamed.music.constants.PlayerDesignStyle
import com.tamed.music.constants.PlayerDesignStyleKey
import com.tamed.music.constants.asSupportedPlayerDesignStyle
import com.tamed.music.constants.PlayerHorizontalPadding
import com.tamed.music.constants.SeekExtraSeconds
import com.tamed.music.constants.SwipeThumbnailKey
import com.tamed.music.constants.TamedCanvasKey
import com.tamed.music.constants.MaxCanvasCacheSizeKey
import com.tamed.music.constants.ThumbnailCornerRadiusKey
import com.tamed.music.constants.CropThumbnailToSquareKey
import com.tamed.music.constants.HidePlayerThumbnailKey
import com.tamed.music.extensions.metadata
import com.tamed.music.extensions.toMediaItem
import com.tamed.music.innertube.YouTube
import com.tamed.music.innertube.models.YouTubeClient
import com.tamed.music.utils.rememberEnumPreference
import com.tamed.music.vivimusiccanvas.ViviMusicCanvasProvider
import com.tamed.music.applecanvas.AppleMusicCanvasProvider
import com.tamed.music.utils.rememberPreference
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File
import java.util.LinkedHashMap
import kotlin.math.abs
import android.content.Context
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.ui.viewinterop.AndroidView

object CanvasArtworkPlaybackCache {
    private const val defaultMaxSize = 256
    private const val PERSIST_FILE = "canvas_artwork_cache_v4.json"
    private const val PERSIST_DEBOUNCE_MS = 2_000L

    private val map = LinkedHashMap<String, CanvasArtwork>(defaultMaxSize, 0.75f, true)
    @Volatile private var maxSize = defaultMaxSize
    @Volatile private var cacheFile: File? = null

    private val persistScope = CoroutineScope(Dispatchers.IO)
    private var persistJob: Job? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val mapSerializer = MapSerializer(String.serializer(), CanvasArtwork.serializer())

    /** Returns true if the URL points to the now-dead Vercel deployment. */
    private fun isStaleUrl(url: String?): Boolean =
        url != null && url.contains("vercel.app", ignoreCase = true)

    /** Rewrites a stale Vercel URL to the jsDelivr CDN mirror. */
    private fun rewriteUrl(url: String): String =
        url.replace(
            Regex("https://vivimusicanvas.*?\\.vercel\\.app/"),
            "https://cdn.jsdelivr.net/gh/vivizzz007/vivimusicanvas@main/"
        )

    /** Rewrites stale URLs in an artwork entry; returns null if no fix is possible. */
    private fun sanitizeArtwork(artwork: CanvasArtwork): CanvasArtwork {
        val fixedAnimated = artwork.animated?.let { if (isStaleUrl(it)) rewriteUrl(it) else it }
        val fixedVideoUrl = artwork.videoUrl?.let { if (isStaleUrl(it)) rewriteUrl(it) else it }
        return artwork.copy(animated = fixedAnimated, videoUrl = fixedVideoUrl)
    }

    // All previous cache file names — must be deleted on upgrade to prevent stale data bleeding in.
    private val LEGACY_FILES = listOf("canvas_artwork_cache.json", "canvas_artwork_cache_v2.json", "canvas_artwork_cache_v3.json")

    fun init(context: Context) {
        // Delete legacy cache files so old album-level hallucinations don't survive app updates.
        LEGACY_FILES.forEach { name ->
            runCatching { File(context.filesDir, name).delete() }
        }

        cacheFile = File(context.filesDir, PERSIST_FILE)
        loadFromDisk()
        // Purge/rewrite entries with dead Vercel URLs or known hallucinated album-level canvases.
        synchronized(this) {
            val slowRushSongs = setOf("one more year", "instant destiny", "borderline",
                "posthumous forgiveness", "breathe deeper", "tomorrow's dust", "on track",
                "lost in yesterday", "is it true", "it might be time", "glimmer", "one more hour")
            val toRemove = mutableListOf<String>()
            val toUpdate = mutableListOf<Map.Entry<String, CanvasArtwork>>()
            map.entries.forEach { entry ->
                val v = entry.value
                when {
                    isStaleUrl(v.animated) || isStaleUrl(v.videoUrl) -> toUpdate.add(entry)
                    // Purge any entry using Song/10.m3u8 that isn't actually a Slow Rush track
                    v.animated?.contains("Song/10.m3u8", ignoreCase = true) == true &&
                    !slowRushSongs.contains(v.name?.lowercase()?.trim()) ->
                        toRemove.add(entry.key)
                }
            }
            toUpdate.forEach { (k, v) -> map[k] = sanitizeArtwork(v) }
            toRemove.forEach { map.remove(it) }
            if (toUpdate.isNotEmpty() || toRemove.isNotEmpty()) {
                Timber.d("Canvas cache: rewrote ${toUpdate.size}, purged ${toRemove.size} hallucinated entries")
                schedulePersist()
            }
        }
    }

    @Synchronized
    fun get(mediaId: String): CanvasArtwork? {
        if (maxSize <= 0) return null
        val artwork = map[mediaId] ?: return null
        // On-the-fly fix: rewrite stale URLs if somehow still present
        return if (isStaleUrl(artwork.animated) || isStaleUrl(artwork.videoUrl)) {
            val fixed = sanitizeArtwork(artwork)
            map[mediaId] = fixed
            schedulePersist()
            fixed
        } else artwork
    }

    @Synchronized
    fun put(mediaId: String, artwork: CanvasArtwork) {
        val limit = maxSize
        if (limit <= 0) return
        if (mediaId.isBlank()) return
        map[mediaId] = artwork
        while (map.size > limit) {
            val it = map.entries.iterator()
            if (it.hasNext()) {
                it.next()
                it.remove()
            }
        }
        schedulePersist()
    }

    @Synchronized
    fun size(): Int = map.size

    @Synchronized
    fun clear() {
        map.clear()
        schedulePersist()
    }

    @Synchronized
    fun setMaxSize(value: Int) {
        maxSize = value.coerceAtLeast(0)
        if (maxSize == 0) {
            map.clear()
            schedulePersist()
            return
        }
        var evicted = false
        while (map.size > maxSize) {
            val it = map.entries.iterator()
            if (it.hasNext()) {
                it.next()
                it.remove()
                evicted = true
            } else {
                break
            }
        }
        if (evicted) schedulePersist()
    }

    @Synchronized
    private fun loadFromDisk() {
        val file = cacheFile ?: return
        if (!file.exists()) return
        try {
            val raw = file.readText()
            if (raw.isBlank()) return
            val restored = json.decodeFromString(mapSerializer, raw)
            map.clear()
            map.putAll(restored)
            while (maxSize > 0 && map.size > maxSize) {
                val it = map.entries.iterator()
                if (it.hasNext()) {
                    it.next()
                    it.remove()
                } else {
                    break
                }
            }
            Timber.d("Canvas cache restored: ${map.size} entries from disk")
        } catch (e: Exception) {
            Timber.e(e, "Failed to restore canvas cache from disk")
            runCatching { file.delete() }
        }
    }

    private fun schedulePersist() {
        persistJob?.cancel()
        persistJob = persistScope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            writeToDisk()
        }
    }

    private fun writeToDisk() {
        val file = cacheFile ?: return
        try {
            val snapshot: Map<String, CanvasArtwork>
            synchronized(this@CanvasArtworkPlaybackCache) {
                snapshot = LinkedHashMap(map)
            }
            val raw = json.encodeToString(mapSerializer, snapshot)
            file.writeText(raw)
        } catch (e: Exception) {
            Timber.e(e, "Failed to persist canvas cache to disk")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Thumbnail(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    isPlayerExpanded: Boolean = true, // Add parameter to control swipe based on player state
    embeddedMode: Boolean = false,
    showHeader: Boolean = true,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val currentView = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    // States
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val error by playerConnection.error.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsState()

    val swipeThumbnail by rememberPreference(SwipeThumbnailKey, true)
    val hidePlayerThumbnail by rememberPreference(HidePlayerThumbnailKey, false)
    val tamedCanvasEnabled by rememberPreference(TamedCanvasKey, false)
    val rawPlayerDesignStyle by rememberEnumPreference(
        key = PlayerDesignStyleKey,
        defaultValue = PlayerDesignStyle.V1,
    )
    val playerDesignStyle = rawPlayerDesignStyle.asSupportedPlayerDesignStyle()
    val (maxCanvasCacheSize, _) = rememberPreference(
        key = MaxCanvasCacheSizeKey,
        defaultValue = 256,
    )
    val (thumbnailCornerRadius, _) = rememberPreference(
        key = ThumbnailCornerRadiusKey,
        defaultValue = 16f
    )
    val cropThumbnailToSquare by rememberPreference(CropThumbnailToSquareKey, false)
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    
    // Player background style for consistent theming
    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.GLOW_ANIMATED
    )
    
    val textBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
        PlayerBackgroundStyle.BLUR -> Color.White
        PlayerBackgroundStyle.GRADIENT -> Color.White
        PlayerBackgroundStyle.COLORING -> Color.White
        PlayerBackgroundStyle.BLUR_GRADIENT -> Color.White
        PlayerBackgroundStyle.GLOW -> Color.White
        PlayerBackgroundStyle.GLOW_ANIMATED -> Color.White
        PlayerBackgroundStyle.CUSTOM -> Color.White
        PlayerBackgroundStyle.APPLE_MUSIC -> Color.White
    }

    LaunchedEffect(maxCanvasCacheSize) {
        CanvasArtworkPlaybackCache.setMaxSize(maxCanvasCacheSize)
    }
    
    // Grid state
    val thumbnailLazyGridState = rememberLazyGridState()
    
    // Create a playlist using correct shuffle-aware logic
    val timeline = playerConnection.player.currentTimeline
    val currentIndex = playerConnection.player.currentMediaItemIndex
    val shuffleModeEnabled = playerConnection.player.shuffleModeEnabled
    val previousMediaMetadata = if (swipeThumbnail && !timeline.isEmpty) {
        val previousIndex = timeline.getPreviousWindowIndex(
            currentIndex,
            Player.REPEAT_MODE_OFF,
            shuffleModeEnabled
        )
        if (previousIndex != C.INDEX_UNSET) {
            try {
                playerConnection.player.getMediaItemAt(previousIndex)
            } catch (e: Exception) { null }
        } else null
    } else null

    val nextMediaMetadata = if (swipeThumbnail && !timeline.isEmpty) {
        val nextIndex = timeline.getNextWindowIndex(
            currentIndex,
            Player.REPEAT_MODE_OFF,
            shuffleModeEnabled
        )
        if (nextIndex != C.INDEX_UNSET) {
            try {
                playerConnection.player.getMediaItemAt(nextIndex)
            } catch (e: Exception) { null }
        } else null
    } else null

    val currentMediaItem = remember(mediaMetadata) {
        // Fallback to player's current item if mediaMetadata is null, 
        // but prefer mediaMetadata for immediate updates during crossfade.
        val metadata = mediaMetadata
        if (metadata != null) {
            // Use extension to convert metadata to a proper MediaItem with all fields (uri, artwork, tag)
            metadata.toMediaItem()
        } else {
            try {
                playerConnection.player.currentMediaItem
            } catch (e: Exception) { null }
        }
    }

    val mediaItems = listOfNotNull(previousMediaMetadata, currentMediaItem, nextMediaMetadata)
    val currentMediaIndex = mediaItems.indexOf(currentMediaItem)

    // OuterTune Snap behavior
    val horizontalLazyGridItemWidthFactor = 1f
    val thumbnailSnapLayoutInfoProvider = remember(thumbnailLazyGridState) {
        SnapLayoutInfoProvider(
            lazyGridState = thumbnailLazyGridState,
            positionInLayout = { layoutSize, itemSize ->
                (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
            },
            velocityThreshold = 500f
        )
    }

    // Current item tracking
    val currentItem by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemIndex } }
    val itemScrollOffset by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemScrollOffset } }

    // Handle swipe to change song
    LaunchedEffect(itemScrollOffset) {
        if (!thumbnailLazyGridState.isScrollInProgress || !swipeThumbnail || itemScrollOffset != 0 || currentMediaIndex < 0) return@LaunchedEffect

        if (currentItem > currentMediaIndex && canSkipNext) {
            playerConnection.player.seekToNext()
            if (com.tamed.music.ui.screens.settings.DiscordPresenceManager.isRunning()) {
                try { com.tamed.music.ui.screens.settings.DiscordPresenceManager.restart() } catch (_: Exception) {}
            }
        } else if (currentItem < currentMediaIndex && canSkipPrevious) {
            playerConnection.player.seekToPreviousMediaItem()
            if (com.tamed.music.ui.screens.settings.DiscordPresenceManager.isRunning()) {
                try { com.tamed.music.ui.screens.settings.DiscordPresenceManager.restart() } catch (_: Exception) {}
            }
        }
    }

    // Update position when song changes
    LaunchedEffect(mediaMetadata, currentMediaItem?.mediaId, canSkipPrevious, canSkipNext) {
        val index = maxOf(0, currentMediaIndex)
        if (index >= 0 && index < mediaItems.size) {
            try {
                thumbnailLazyGridState.animateScrollToItem(index)
            } catch (e: Exception) {
                thumbnailLazyGridState.scrollToItem(index)
            }
        }
    }

    LaunchedEffect(playerConnection.player.currentMediaItemIndex, currentMediaItem?.mediaId) {
        val index = mediaItems.indexOf(currentMediaItem)
        if (index >= 0 && index != currentItem) {
            thumbnailLazyGridState.scrollToItem(index)
        }
    }

    // Seek on double tap
    var showSeekEffect by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableStateOf("") }
    val layoutDirection = LocalLayoutDirection.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = modifier) {
        // Error view
        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .padding(32.dp)
                .align(Alignment.Center),
        ) {
            error?.let { playbackError ->
                PlaybackError(
                    error = playbackError,
                    mediaId = currentMediaItem?.mediaId,
                    retry = playerConnection.service::retryCurrentFromFreshStream,
                )
            }
        }

        // Main thumbnail view
        AnimatedVisibility(
            visible = error == null && !(playerBackground == PlayerBackgroundStyle.APPLE_MUSIC && !isLandscape),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .let { base ->
                    if (embeddedMode) base else base.statusBarsPadding()
                },
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!embeddedMode && showHeader) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.now_playing),
                            style = MaterialTheme.typography.titleMedium,
                            color = textBackgroundColor
                        )
                        val playingFrom = queueTitle ?: mediaMetadata?.album?.title
                        if (!playingFrom.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = playingFrom,
                                style = MaterialTheme.typography.titleMedium,
                                color = textBackgroundColor.copy(alpha = 0.8f),
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }
                }

                BoxWithConstraints(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
                    val containerMaxWidth = maxWidth

                    LazyHorizontalGrid(
                        state = thumbnailLazyGridState,
                        rows = GridCells.Fixed(1),
                        flingBehavior = rememberSnapFlingBehavior(thumbnailSnapLayoutInfoProvider),
                        userScrollEnabled = swipeThumbnail && isPlayerExpanded, // Only allow swipe when player is expanded
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = mediaItems,
                            key = { item -> 
                                // Use mediaId with stable fallback to avoid recomposition issues
                                item.mediaId.ifEmpty { "unknown_${item.hashCode()}" }
                            }
                        ) { item ->
                            val incrementalSeekSkipEnabled by rememberPreference(SeekExtraSeconds, defaultValue = false)
                            var skipMultiplier by remember { mutableStateOf(1) }
                            var lastTapTime by remember { mutableLongStateOf(0L) }
                            val itemMetadata = remember(item) { item.metadata }
                            val shouldAnimateCanvas =
                                tamedCanvasEnabled &&
                                    item.mediaId.isNotBlank() &&
                                    item.mediaId == currentMediaItem?.mediaId
                            var canvasArtwork by remember(item.mediaId) { mutableStateOf<CanvasArtwork?>(null) }
                            var canvasFetchedAtMs by remember(item.mediaId) { mutableLongStateOf(0L) }
                            var canvasFetchInFlight by remember(item.mediaId) { mutableStateOf(false) }
                            val storefront = remember {
                                val country = Locale.getDefault().country
                                if (country.length == 2) country.lowercase(Locale.ROOT) else "us"
                            }

                            LaunchedEffect(shouldAnimateCanvas) {
                                if (!shouldAnimateCanvas) {
                                    canvasArtwork = null
                                    canvasFetchedAtMs = 0L
                                    canvasFetchInFlight = false
                                }
                            }

                            LaunchedEffect(shouldAnimateCanvas, item.mediaId) {
                                if (!shouldAnimateCanvas) return@LaunchedEffect

                                // Clear previous canvas immediately when song changes to prevent "sticking"
                                canvasArtwork = null
                                canvasFetchedAtMs = 0L
                                canvasFetchInFlight = false

                                CanvasArtworkPlaybackCache.get(item.mediaId)?.let { cached ->
                                    canvasArtwork = cached
                                    canvasFetchedAtMs = System.currentTimeMillis()
                                    canvasFetchInFlight = false
                                    return@LaunchedEffect
                                }

                                val songTitleRaw =
                                    itemMetadata?.title
                                        ?.takeIf { it.isNotBlank() }
                                        ?: item.mediaMetadata.title?.toString()
                                        ?: return@LaunchedEffect

                                val artistNameRaw =
                                    itemMetadata?.artists?.firstOrNull()?.name
                                        ?.takeIf { it.isNotBlank() }
                                        ?: item.mediaMetadata.artist?.toString()
                                        ?: item.mediaMetadata.subtitle?.toString()
                                        ?: ""

                                val albumNameRaw =
                                    itemMetadata?.album?.title
                                        ?.takeIf { it.isNotBlank() }
                                        ?: item.mediaMetadata.albumTitle?.toString()
                                        ?: ""

                                val now = System.currentTimeMillis()
                                if (canvasFetchInFlight) return@LaunchedEffect
                                canvasFetchInFlight = true

                                val fetched =
                                    withContext(Dispatchers.IO) {
                                        val songTitle = normalizeCanvasSongTitle(songTitleRaw)
                                        val artistName = normalizeCanvasArtistName(artistNameRaw)
                                        val albumName = normalizeCanvasSongTitle(albumNameRaw)
                                        
                                        // Strategy: 
                                         // 1. Try Song-Specific matches (Vivi -> Apple)
                                         val songCandidates = linkedSetOf(
                                             songTitle to artistName,
                                             songTitleRaw to artistName,
                                             songTitle to artistNameRaw,
                                             songTitleRaw to artistNameRaw,
                                         ).filter { it.first.isNotBlank() && it.second.isNotBlank() }

                                         for ((name, artist) in songCandidates) {
                                             ViviMusicCanvasProvider.getBySongArtist(
                                                 song = name,
                                                 artist = artist,
                                                 album = albumName
                                             )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }?.let { return@withContext it }

                                             AppleMusicCanvasProvider.getBySongArtist(
                                                 song = name,
                                                 artist = artist,
                                                 album = albumName,
                                                 storefront = storefront
                                             )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }?.let { return@withContext it }
                                         }

                                         // 2. Try Album-Specific matches (Vivi -> Apple)
                                         if (albumName.isNotBlank()) {
                                             ViviMusicCanvasProvider.getByAlbumArtist(
                                                 album = albumName,
                                                 artist = artistName
                                             )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }?.let { return@withContext it }

                                             AppleMusicCanvasProvider.getByAlbumArtist(
                                                 album = albumName,
                                                 artist = artistName,
                                                 storefront = storefront
                                             )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }?.let { return@withContext it }
                                         }

                                         // 3. Last Resort: Monochrome (with validation already added)
                                         for ((name, artist) in songCandidates) {
                                             com.tamed.music.applecanvas.MonochromeApiCanvas.getForSong(
                                                 title = name,
                                                 artist = artist,
                                                 album = albumName
                                             )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }?.let { return@withContext it }
                                         }
                                         
                                         null
                                     }
                                if (fetched != null) {
                                    // Final safety check: if provider returned artwork for a different artist or album, skip it
                                    val resultArtist = fetched.artist
                                    val resultAlbum = fetched.albumName
                                        
                                    val artistValid = if (resultArtist != null && item.mediaMetadata.artist?.toString()?.isNotBlank() == true) {
                                        val normalizedResultArtist = normalizeCanvasArtistName(resultArtist)
                                        val normalizedRequestedArtist = normalizeCanvasArtistName(item.mediaMetadata.artist.toString())
                                            
                                        normalizedResultArtist == normalizedRequestedArtist ||
                                        resultArtist.equals(item.mediaMetadata.artist.toString(), ignoreCase = true)
                                    } else true

                                    val albumValid = if (resultAlbum != null && !item.mediaMetadata.albumTitle.isNullOrBlank()) {
                                        val normalizedResultAlbum = normalizeCanvasSongTitle(resultAlbum)
                                        val normalizedRequestedAlbum = normalizeCanvasSongTitle(item.mediaMetadata.albumTitle.toString())
                                            
                                        normalizedResultAlbum == normalizedRequestedAlbum ||
                                        resultAlbum.contains(item.mediaMetadata.albumTitle.toString(), ignoreCase = true) ||
                                        item.mediaMetadata.albumTitle.toString().contains(resultAlbum, ignoreCase = true)
                                    } else if (resultAlbum != null && item.mediaMetadata.albumTitle.isNullOrBlank()) {
                                        // If we don't have an album but result does, maybe it's fine, but let's be cautious
                                        true
                                    } else {
                                        // If result doesn't have an album, it's probably a song-specific canvas
                                        true
                                    }

                                    if (artistValid && albumValid) {
                                        canvasArtwork = fetched
                                        canvasFetchedAtMs = System.currentTimeMillis()
                                        CanvasArtworkPlaybackCache.put(item.mediaId, fetched)
                                    } else {
                                        Timber.w("Canvas validation failed: Result($resultArtist, $resultAlbum) vs Requested(${item.mediaMetadata.artist}, ${item.mediaMetadata.albumTitle})")
                                    }
                                }
                                canvasFetchInFlight = false
                            }

                            Box(
                                modifier = Modifier
                                    .width(horizontalLazyGridItemWidth)
                                    .fillMaxSize()
                                    .padding(horizontal = if (embeddedMode) 0.dp else PlayerHorizontalPadding)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = { offset ->
                                                val currentPosition = playerConnection.player.currentPosition
                                                val duration = playerConnection.player.duration

                                                val now = System.currentTimeMillis()
                                                if (incrementalSeekSkipEnabled && now - lastTapTime < 1000) {
                                                    skipMultiplier++
                                                } else {
                                                    skipMultiplier = 1
                                                }
                                                lastTapTime = now

                                                val skipAmount = 5000 * skipMultiplier

                                                if ((layoutDirection == LayoutDirection.Ltr && offset.x < size.width / 2) ||
                                                    (layoutDirection == LayoutDirection.Rtl && offset.x > size.width / 2)
                                                ) {
                                                    playerConnection.player.seekTo(
                                                        (currentPosition - skipAmount).coerceAtLeast(0)
                                                    )
                                                    seekDirection =
                                                        context.getString(R.string.seek_backward_dynamic, skipAmount / 1000)
                                                } else {
                                                    playerConnection.player.seekTo(
                                                        (currentPosition + skipAmount).coerceAtMost(duration)
                                                    )
                                                    seekDirection = context.getString(R.string.seek_forward_dynamic, skipAmount / 1000)
                                                }
                                                // If a user double-tap skip lands on a new media item, restart presence manager to pick up artwork quickly
                                                if (com.tamed.music.ui.screens.settings.DiscordPresenceManager.isRunning()) {
                                                    try { com.tamed.music.ui.screens.settings.DiscordPresenceManager.restart() } catch (_: Exception) {}
                                                }

                                                showSeekEffect = true
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val artworkPadding = if (embeddedMode) 0.dp else PlayerHorizontalPadding
                                Box(
                                    modifier = Modifier
                                        .size(containerMaxWidth - (artworkPadding * 2))
                                        .clip(RoundedCornerShape(thumbnailCornerRadius.dp))
                                ) {
                                    if (hidePlayerThumbnail) {
                                        // Show app logo when thumbnail is hidden
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.about_splash),
                                                contentDescription = stringResource(R.string.hide_player_thumbnail),
                                                tint = textBackgroundColor.copy(alpha = 0.7f),
                                                modifier = Modifier.size(120.dp)
                                            )
                                        }
                                    } else {
                                        val primaryCanvasUrl = canvasArtwork?.animated
                                        val fallbackCanvasUrl = canvasArtwork?.videoUrl
                                        val highResArtwork = item.mediaMetadata.artworkUri?.toString()?.replace(Regex("=w\\d+-h\\d+"), "=w1200-h1200")?.replace(Regex("=s\\d+"), "=s1200")
                                        
                                        AsyncImage(
                                            model = highResArtwork,
                                            contentDescription = null,
                                            contentScale = ContentScale.FillBounds,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .let { if (cropThumbnailToSquare) it.aspectRatio(1f) else it }
                                                .graphicsLayer(
                                                    renderEffect = BlurEffect(radiusX = 60f, radiusY = 60f),
                                                    alpha = 0.6f
                                                )
                                        )

                                        AsyncImage(
                                            model = highResArtwork,
                                            contentDescription = null,
                                            contentScale = if (cropThumbnailToSquare) ContentScale.Crop else ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .let { if (cropThumbnailToSquare) it.aspectRatio(1f) else it }
                                        )

                                        if (shouldAnimateCanvas && (!primaryCanvasUrl.isNullOrBlank() || !fallbackCanvasUrl.isNullOrBlank())) {
                                            key(item.mediaId) {
                                                CanvasArtworkPlayer(
                                                    primaryUrl = primaryCanvasUrl,
                                                    fallbackUrl = fallbackCanvasUrl,
                                                    isPlaying = isPlaying,
                                                    modifier = Modifier.fillMaxSize(),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Seek effect
        LaunchedEffect(showSeekEffect) {
            if (showSeekEffect) {
                delay(1000)
                showSeekEffect = false
            }
        }

        AnimatedVisibility(
            visible = showSeekEffect,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = seekDirection,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            )
        }
    }
}


internal fun normalizeCanvasSongTitle(raw: String): String {
    val stripped =
        raw
            .replace(Regex("\\s*\\[[^]]*]"), "")
            .replace(
                Regex(
                    "\\s*\\((?:feat\\.?|ft\\.?|featuring|with)\\b[^)]*\\)",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            .replace(
                Regex(
                    "\\s*\\((?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)[^)]*\\)",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            .replace(
                Regex(
                    "\\s*-\\s*(?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)\\b.*$",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            .replace(Regex("\\s+"), " ")
            .trim()

    return stripped
        .trim('-')
        .replace(Regex("\\s+"), " ")
        .trim()
}

internal fun normalizeCanvasArtistName(raw: String): String {
    val first =
        raw
            .split(
                Regex(
                    "(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)",
                    RegexOption.IGNORE_CASE,
                ),
                limit = 2,
            ).firstOrNull().orEmpty()

    return first.replace(Regex("\\s+"), " ").trim()
}

/*
 * Copyright (C) OuterTune Project
 * Custom SnapLayoutInfoProvider idea belongs to OuterTune
 */

// SnapLayoutInfoProvider
@ExperimentalFoundationApi
fun SnapLayoutInfoProvider(
    lazyGridState: LazyGridState,
    positionInLayout: (layoutSize: Float, itemSize: Float) -> Float = { layoutSize, itemSize ->
        (layoutSize / 2f - itemSize / 2f)
    },
    velocityThreshold: Float = 1000f,
): SnapLayoutInfoProvider = object : SnapLayoutInfoProvider {
    private val layoutInfo: LazyGridLayoutInfo
        get() = lazyGridState.layoutInfo

    override fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float = 0f
    override fun calculateSnapOffset(velocity: Float): Float {
        val bounds = calculateSnappingOffsetBounds()

        // Only snap when velocity exceeds threshold
        if (abs(velocity) < velocityThreshold) {
            if (abs(bounds.start) < abs(bounds.endInclusive))
                return bounds.start

            return bounds.endInclusive
        }

        return when {
            velocity < 0 -> bounds.start
            velocity > 0 -> bounds.endInclusive
            else -> 0f
        }
    }

    fun calculateSnappingOffsetBounds(): ClosedFloatingPointRange<Float> {
        var lowerBoundOffset = Float.NEGATIVE_INFINITY
        var upperBoundOffset = Float.POSITIVE_INFINITY

        layoutInfo.visibleItemsInfo.fastForEach { item ->
            val offset = calculateDistanceToDesiredSnapPosition(layoutInfo, item, positionInLayout)

            // Find item that is closest to the center
            if (offset <= 0 && offset > lowerBoundOffset) {
                lowerBoundOffset = offset
            }

            // Find item that is closest to center, but after it
            if (offset >= 0 && offset < upperBoundOffset) {
                upperBoundOffset = offset
            }
        }

        return lowerBoundOffset.rangeTo(upperBoundOffset)
    }
}

fun calculateDistanceToDesiredSnapPosition(
    layoutInfo: LazyGridLayoutInfo,
    item: LazyGridItemInfo,
    positionInLayout: (layoutSize: Float, itemSize: Float) -> Float,
): Float {
    val containerSize =
        layoutInfo.singleAxisViewportSize - layoutInfo.beforeContentPadding - layoutInfo.afterContentPadding

    val desiredDistance = positionInLayout(containerSize.toFloat(), item.size.width.toFloat())
    val itemCurrentPosition = item.offset.x.toFloat()

    return itemCurrentPosition - desiredDistance
}

private val LazyGridLayoutInfo.singleAxisViewportSize: Int
    get() = if (orientation == Orientation.Vertical) viewportSize.height else viewportSize.width

/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.ui.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.text.format.Formatter
import android.util.LruCache
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamed.music.LocalDatabase
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.R
import com.tamed.music.db.entities.FormatEntity
import com.tamed.music.db.entities.Song
import com.tamed.music.innertube.YouTube
import com.tamed.music.innertube.models.MediaInfo
import com.tamed.music.models.AudioQualityInfo
import com.tamed.music.ui.component.AudioQualityBadge
import com.tamed.music.ui.component.MenuSurfaceSection
import com.tamed.music.ui.theme.SfProFontFamily
import com.tamed.music.ui.utils.smoothFadingEdge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

object MediaInfoRepository {
    private val cache = LruCache<String, MediaInfo>(100)

    fun getCached(videoId: String): MediaInfo? = cache.get(videoId)

    suspend fun getOrFetch(videoId: String): MediaInfo? {
        cache.get(videoId)?.let { return it }
        return try {
            YouTube.getMediaInfo(videoId).getOrNull()?.also {
                cache.put(videoId, it)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun prefetch(videoId: String) {
        if (videoId.isNotBlank() && cache.get(videoId) == null) {
            CoroutineScope(Dispatchers.IO).launch {
                getOrFetch(videoId)
            }
        }
    }
}

@Composable
private fun DetailsHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp, start = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.info),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.details),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = SfProFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp
                )
            )
            Text(
                text = "Track & audio details",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = SfProFontFamily,
                    fontSize = 12.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ShowMediaInfo(videoId: String) {
    if (videoId.isBlank() || videoId.isEmpty()) return

    val windowInsets = WindowInsets.systemBars

    val cached = remember(videoId) { MediaInfoRepository.getCached(videoId) }
    var info by remember(videoId) { mutableStateOf<MediaInfo?>(cached) }
    var isLoading by remember(videoId) { mutableStateOf(cached == null) }

    val database = LocalDatabase.current
    var song by remember { mutableStateOf<Song?>(null) }
    var currentFormat by remember { mutableStateOf<FormatEntity?>(null) }

    val playerConnection = LocalPlayerConnection.current
    val context = LocalContext.current

    val activeSource by playerConnection?.activeSourceInfo?.collectAsState(null) ?: remember { mutableStateOf(null) }
    val currentAudioQuality by playerConnection?.currentAudioQuality?.collectAsState(null) ?: remember { mutableStateOf(null) }

    LaunchedEffect(videoId) {
        if (cached == null) {
            isLoading = true
            withTimeoutOrNull(2500L) {
                info = MediaInfoRepository.getOrFetch(videoId)
            }
            isLoading = false
        }
    }
    LaunchedEffect(videoId) {
        database.song(videoId).collect {
            song = it
        }
    }
    LaunchedEffect(videoId) {
        database.format(videoId).collect {
            currentFormat = it
        }
    }

    val currentMediaMeta = playerConnection?.mediaMetadata?.value?.takeIf { it.id == videoId }
    val resolvedQuality = remember(currentAudioQuality, currentFormat, activeSource, song, videoId) {
        if (currentAudioQuality != null && (currentAudioQuality?.itag != null || currentAudioQuality?.codec?.isNotBlank() == true)) {
            currentAudioQuality
        } else {
            AudioQualityInfo.resolve(
                mediaId = videoId,
                dbFormat = currentFormat,
                sourceInfo = activeSource?.takeIf { it.mediaId == videoId },
                exoFormat = playerConnection?.player?.audioFormat,
                metadata = currentMediaMeta,
            )
        }
    }

    val displayTitle = song?.title ?: currentMediaMeta?.title ?: info?.title
    val displayArtists = song?.artists?.joinToString { it.name }
        ?: currentMediaMeta?.artists?.joinToString { it.name }
        ?: info?.author

    val listState = rememberLazyListState()
    val showTopFade by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    val topFadeDp by animateDpAsState(
        targetValue = if (showTopFade) 28.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "topFade"
    )

    val showBottomFade by remember {
        derivedStateOf {
            listState.canScrollForward
        }
    }
    val bottomFadeDp by animateDpAsState(
        targetValue = if (showBottomFade) 44.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "bottomFade"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Crossfade(
            targetState = isLoading,
            animationSpec = tween(durationMillis = 180),
            label = "mediaInfoLoading"
        ) { loading ->
            if (loading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    DetailsHeader()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        top = 6.dp,
                        bottom = 40.dp + windowInsets.asPaddingValues().calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .smoothFadingEdge(
                            top = topFadeDp,
                            bottom = bottomFadeDp,
                        )
                ) {
                    // Header
                    item(contentType = "Header") {
                        DetailsHeader()
                    }

            // Track Metadata Section
            if (displayTitle != null) {
                item(contentType = "TrackCard") {
                    MenuSurfaceSection {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val copyText = if (!displayArtists.isNullOrEmpty()) "$displayTitle • $displayArtists" else displayTitle
                                    cm.setPrimaryClip(ClipData.newPlainText("Track Info", copyText))
                                    Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = SfProFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!displayArtists.isNullOrEmpty()) {
                                Text(
                                    text = displayArtists,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = SfProFontFamily,
                                        fontSize = 14.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Consolidated Audio Quality Section
            resolvedQuality?.let { quality ->
                item(contentType = "QualityBanner") {
                    MenuSurfaceSection {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.graphic_eq),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Audio Quality",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = SfProFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        )
                                    )
                                }
                                AudioQualityBadge(
                                    quality = quality,
                                    textColor = MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                thickness = 0.5.dp
                            )

                            val stats: List<Pair<String, String>> = buildList {
                                add("Source" to quality.sourceName)
                                val formatStr = buildString {
                                    append(quality.codec)
                                    quality.bitrate?.takeIf { it > 0 }?.let {
                                        append(" · ${it / 1000} kbps")
                                    }
                                }
                                add("Format" to (quality.subtitle?.takeIf { it.isNotBlank() } ?: formatStr))
                                val sampleRateStr = quality.sampleRate?.let {
                                    if (it >= 1000) "${it / 1000.0}".trimEnd('0').trimEnd('.') + " kHz" else "$it Hz"
                                } ?: "44.1 kHz"
                                add(stringResource(R.string.sample_rate) to sampleRateStr)
                                val bitDepthStr = quality.bitDepth?.let { "$it-bit" } ?: "16-bit"
                                add("Bit Depth" to bitDepthStr)
                                val channelsStr = if (quality.channels == 1) "Mono" else "Stereo"
                                add("Channels" to channelsStr)
                                currentFormat?.contentLength?.takeIf { it > 0 }?.let {
                                    add(stringResource(R.string.file_size) to Formatter.formatShortFileSize(context, it))
                                }
                                currentFormat?.loudnessDb?.let {
                                    add(stringResource(R.string.loudness) to "$it dB")
                                }
                            }

                            stats.forEachIndexed { index, (label, value) ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                                        thickness = 0.5.dp
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText(label, value))
                                            Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 11.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = SfProFontFamily,
                                            fontSize = 14.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = SfProFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (quality.isLossless) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                                    thickness = 0.5.dp
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.check),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Lossless audio • Bit-exact dynamics preserved",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = SfProFontFamily,
                                            fontSize = 12.5.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Description Section
            val description = info?.description?.trim()
            if (!description.isNullOrBlank()) {
                item(contentType = "DescriptionCard") {
                    MenuSurfaceSection {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.description),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = SfProFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                                thickness = 0.5.dp
                            )

                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = SfProFontFamily,
                                    fontSize = 13.5.sp,
                                    lineHeight = 20.sp,
                                    letterSpacing = 0.1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
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



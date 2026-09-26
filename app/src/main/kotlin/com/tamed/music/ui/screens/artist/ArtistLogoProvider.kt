package com.tamed.music.ui.screens.artist

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tamed.music.R
import com.tamed.music.ui.theme.TamedAppleTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Metadata defining an artist logo wordmark asset.
 *
 * @param model A @DrawableRes Int, a decoded Bitmap, or a remote image URL String
 * @param height Optional custom height. If null, dynamically sizes based on aspect ratio.
 * @param tintable Whether the logo should be tinted with foreground/text color (for bundled monochrome vector drawables)
 */
data class ArtistLogoInfo(
    val model: Any,
    val height: Dp? = null,
    val tintable: Boolean = false,
)

/**
 * Registry managing artist logo wordmarks with dynamic online discovery, auto-crop, solid white vector rendering, and local overrides.
 */
object ArtistLogoRegistry {
    private val NO_LOGO = ArtistLogoInfo(model = Unit)

    // Local overrides for bespoke custom wordmarks
    private val localLogos = mapOf<String, ArtistLogoInfo>(
        "tame impala" to ArtistLogoInfo(
            model = R.drawable.tame_impala_logo_stacked,
            height = 96.dp,
            tintable = true,
        ),
        "dua lipa" to ArtistLogoInfo(
            model = R.drawable.dua_lipa_logo,
            height = 58.dp,
            tintable = true,
        ),
        "the weeknd" to ArtistLogoInfo(
            model = R.drawable.the_weeknd_logo,
            height = 54.dp,
            tintable = true,
        ),
    )

    private val memoryCache = ConcurrentHashMap<String, ArtistLogoInfo>()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Check if a local bundled logo or cached logo exists synchronously.
     */
    fun getLocalOrCached(artistName: String?): ArtistLogoInfo? {
        if (artistName.isNullOrBlank()) return null
        val normalized = artistName.trim().lowercase()
        localLogos[normalized]?.let { return it }
        val cached = memoryCache[normalized]
        return if (cached != null && cached !== NO_LOGO) cached else null
    }

    /**
     * Check if we've already resolved (or attempted to resolve) this artist.
     */
    fun isResolved(artistName: String?): Boolean {
        if (artistName.isNullOrBlank()) return true
        val normalized = artistName.trim().lowercase()
        return localLogos.containsKey(normalized) || memoryCache.containsKey(normalized)
    }

    /**
     * Trims transparent margins around the bitmap so the wordmark fills the full visual container.
     */
    private fun trimTransparentEdges(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                val alpha = android.graphics.Color.alpha(pixels[rowOffset + x])
                if (alpha > 20) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (minX > maxX || minY > maxY) return src

        val cropX = (minX - 2).coerceAtLeast(0)
        val cropY = (minY - 2).coerceAtLeast(0)
        val cropW = (maxX - cropX + 3).coerceAtMost(width - cropX)
        val cropH = (maxY - cropY + 3).coerceAtMost(height - cropY)

        if (cropW <= 0 || cropH <= 0 || (cropW == width && cropH == height)) return src
        return Bitmap.createBitmap(src, cropX, cropY, cropW, cropH)
    }

    /**
     * Converts the typography wordmark to a solid, filled white vector asset.
     * Preserves smooth anti-aliased edge transparency while turning letter strokes into solid white.
     */
    private fun convertToSolidWhiteVector(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outPixels = IntArray(width * height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = android.graphics.Color.alpha(pixel)
            if (a > 20) {
                outPixels[i] = android.graphics.Color.argb(a, 255, 255, 255)
            } else {
                outPixels[i] = 0
            }
        }

        outBitmap.setPixels(outPixels, 0, width, 0, 0, width, height)
        return outBitmap
    }

    /**
     * Fetches the artist logo strictly from official typography wordmarks (strArtistLogo).
     * Extracts solid white vector variant and trims margins.
     */
    suspend fun getLogo(artistName: String?, artistId: String? = null): ArtistLogoInfo? {
        if (artistName.isNullOrBlank()) return null
        val normalized = artistName.trim().lowercase()

        // 1. Check local overrides
        localLogos[normalized]?.let { return it }

        // 2. Check cache
        if (memoryCache.containsKey(normalized)) {
            val cached = memoryCache[normalized]
            return if (cached != null && cached !== NO_LOGO) cached else null
        }

        // 3. Query dynamic artist logo online
        return withContext(Dispatchers.IO) {
            try {
                val encodedName = URLEncoder.encode(artistName.trim(), "UTF-8")
                val url = "https://www.theaudiodb.com/api/v1/json/2/search.php?s=$encodedName"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "TamedMusic/1.0")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val artists = json.optJSONArray("artists")
                            if (artists != null && artists.length() > 0) {
                                val firstArtist = artists.getJSONObject(0)
                                // Strictly use strArtistLogo (the typography font logo)
                                val logoUrl = firstArtist.optString("strArtistLogo").takeIf { it.isNotBlank() && it != "null" }

                                if (!logoUrl.isNullOrBlank()) {
                                    val imgRequest = Request.Builder()
                                        .url(logoUrl)
                                        .header("User-Agent", "TamedMusic/1.0")
                                        .build()

                                    httpClient.newCall(imgRequest).execute().use { imgResponse ->
                                        if (imgResponse.isSuccessful) {
                                            imgResponse.body?.byteStream()?.use { stream ->
                                                val decoded = BitmapFactory.decodeStream(stream)
                                                if (decoded != null) {
                                                    val trimmed = trimTransparentEdges(decoded)
                                                    val solidWhite = convertToSolidWhiteVector(trimmed)
                                                    val finalTrimmed = trimTransparentEdges(solidWhite)
                                                    val logoInfo = ArtistLogoInfo(
                                                        model = finalTrimmed,
                                                        height = null,
                                                        tintable = false,
                                                    )
                                                    memoryCache[normalized] = logoInfo
                                                    return@withContext logoInfo
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Network error, gracefully fall back
            }

            memoryCache[normalized] = NO_LOGO
            null
        }
    }
}

private sealed interface ArtistHeaderDisplayState {
    data object Resolving : ArtistHeaderDisplayState
    data class ShowingLogo(val info: ArtistLogoInfo) : ArtistHeaderDisplayState
    data class ShowingText(val name: String) : ArtistHeaderDisplayState
}

/**
 * Header title component for artist screens.
 * Displays high-quality logo wordmark if available, otherwise seamlessly falls back to standard typography.
 * Always renders dead center horizontally with zero layout jitter and no premature text flashing.
 */
@Composable
fun ArtistHeaderTitle(
    artistName: String?,
    artistId: String? = null,
    modifier: Modifier = Modifier,
) {
    val initialLogo = ArtistLogoRegistry.getLocalOrCached(artistName)
    var displayState by remember(artistName) {
        mutableStateOf(
            when {
                artistName.isNullOrBlank() -> ArtistHeaderDisplayState.Resolving
                initialLogo != null -> ArtistHeaderDisplayState.ShowingLogo(initialLogo)
                ArtistLogoRegistry.isResolved(artistName) -> ArtistHeaderDisplayState.ShowingText(artistName)
                else -> ArtistHeaderDisplayState.Resolving
            }
        )
    }

    LaunchedEffect(artistName) {
        if (!artistName.isNullOrBlank()) {
            val cached = ArtistLogoRegistry.getLocalOrCached(artistName)
            if (cached != null) {
                displayState = ArtistHeaderDisplayState.ShowingLogo(cached)
            } else if (ArtistLogoRegistry.isResolved(artistName)) {
                displayState = ArtistHeaderDisplayState.ShowingText(artistName)
            } else {
                displayState = ArtistHeaderDisplayState.Resolving
                val resolved = ArtistLogoRegistry.getLogo(artistName, artistId)
                displayState = if (resolved != null) {
                    ArtistHeaderDisplayState.ShowingLogo(resolved)
                } else {
                    ArtistHeaderDisplayState.ShowingText(artistName)
                }
            }
        } else {
            displayState = ArtistHeaderDisplayState.Resolving
        }
    }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val tintColor = if (isDarkTheme) Color.White else Color(0xFF1C1C1E)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp, max = 132.dp),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = displayState,
            animationSpec = tween(durationMillis = 200),
            label = "ArtistTitleCrossfade",
            modifier = Modifier.fillMaxWidth(),
        ) { state ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp, max = 132.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (state) {
                    is ArtistHeaderDisplayState.Resolving -> {
                        // Keep layout space stable while resolving online
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp, max = 132.dp),
                            contentAlignment = Alignment.Center,
                        ) {}
                    }
                    is ArtistHeaderDisplayState.ShowingLogo -> {
                        val logo = state.info
                        val dynamicMaxHeight = remember(logo) {
                            val model = logo.model
                            if (model is Bitmap && model.height > 0) {
                                val ratio = model.width.toFloat() / model.height.toFloat()
                                when {
                                    ratio >= 5.0f -> 68.dp   // Very wide: Foster The People, Queens of the Stone Age -> full width, untouched
                                    ratio >= 3.8f -> 92.dp   // Moderately wide: Gracie Abrams -> slightly bigger
                                    ratio >= 2.8f -> 105.dp  // Medium: Gorillaz, Justice, Sabrina Carpenter -> slightly bigger
                                    ratio >= 1.8f -> 118.dp  // Compact: Olivia Rodrigo, Arctic Monkeys -> prominent & bold
                                    else -> 128.dp          // Stacked / square: Daft Punk, MGMT -> bold hero presence, not tiny
                                }
                            } else {
                                92.dp
                            }
                        }

                        val sizingModifier = if (logo.height != null) {
                            Modifier.height(logo.height)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp, max = dynamicMaxHeight)
                        }

                        when (val model = logo.model) {
                            is Int -> {
                                Image(
                                    painter = painterResource(model),
                                    contentDescription = artistName,
                                    colorFilter = if (logo.tintable) ColorFilter.tint(tintColor) else null,
                                    modifier = sizingModifier,
                                    contentScale = ContentScale.Fit,
                                    alignment = Alignment.Center,
                                )
                            }
                            is Bitmap -> {
                                Image(
                                    bitmap = model.asImageBitmap(),
                                    contentDescription = artistName,
                                    colorFilter = if (logo.tintable) ColorFilter.tint(tintColor) else null,
                                    modifier = sizingModifier,
                                    contentScale = ContentScale.Fit,
                                    alignment = Alignment.Center,
                                )
                            }
                            else -> {
                                Text(
                                    text = state.info.model.toString(),
                                    style = TamedAppleTypography.largeTitle(),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    is ArtistHeaderDisplayState.ShowingText -> {
                        Text(
                            text = state.name,
                            style = TamedAppleTypography.largeTitle(),
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

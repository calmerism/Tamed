/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.ui.theme

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.constants.HomeBackgroundStyle
import com.tamed.music.constants.HomeBackgroundStyleKey
import com.tamed.music.constants.ArtistBackgroundStyleKey
import androidx.datastore.preferences.core.Preferences
import com.tamed.music.utils.rememberEnumPreference
import androidx.compose.foundation.Image
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import coil3.imageLoader
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TamedAppleColors {
    val Background = Color(0xFF0A0A0A)
    val BackgroundElevated = Color(0xFF111111)
    val Surface = Color(0xFF151515)
    val SurfaceStrong = Color(0xFF212121)
    val Glass = Color(0xFF242424).copy(alpha = 0.82f)
    val GlassBorder = Color.White.copy(alpha = 0.08f)
    val PrimaryText = Color.White
    val SecondaryText = Color.White.copy(alpha = 0.66f)
    val TertiaryText = Color.White.copy(alpha = 0.46f)
    val Divider = Color.White.copy(alpha = 0.08f)
    val SearchBubble = Color(0xFF242424)
    val AccentFallback = Color(0xFFFF6B81)
}

object TamedAppleShapes {
    val card = RoundedCornerShape(14.dp)
    val panel = RoundedCornerShape(22.dp)
    val miniPlayer = RoundedCornerShape(26.dp)
    val pill = RoundedCornerShape(999.dp)
}

object TamedAppleTypography {
    @Composable
    fun largeTitle(): TextStyle =
        MaterialTheme.typography.headlineLarge.copy(
            fontSize = 30.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
            color = applePrimaryTextColor(),
        )

    @Composable
    fun sectionTitle(): TextStyle =
        MaterialTheme.typography.headlineSmall.copy(
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
            color = applePrimaryTextColor(),
        )

    @Composable
    fun cardTitle(): TextStyle =
        MaterialTheme.typography.titleLarge.copy(
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp,
            color = applePrimaryTextColor(),
        )

    @Composable
    fun cardSubtitle(): TextStyle =
        MaterialTheme.typography.bodyMedium.copy(
            fontSize = 14.sp,
            lineHeight = 18.sp,
            color = appleSecondaryTextColor(),
        )

    @Composable
    fun headerSubtitle(): TextStyle =
        MaterialTheme.typography.bodyMedium.copy(
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = appleSecondaryTextColor(),
        )

    @Composable
    fun metadata(): TextStyle =
        MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp,
            lineHeight = 15.sp,
            color = appleTertiaryTextColor(),
        )
}

@Composable
fun appleBackgroundColor(): Color = MaterialTheme.colorScheme.background

@Composable
fun appleSurfaceColor(): Color = MaterialTheme.colorScheme.surfaceContainerLow

@Composable
fun appleSurfaceStrongColor(): Color = MaterialTheme.colorScheme.surfaceContainerHigh

@Composable
fun appleGlassColor(): Color {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (darkTheme) {
        Color.White.copy(alpha = 0.08f)
    } else {
        appleSurfaceStrongColor().copy(alpha = 0.18f)
    }
}

@Composable
fun appleMiniPlayerGlassColor(): Color {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (darkTheme) {
        Color.Black.copy(alpha = 0.35f)
    } else {
        Color.White.copy(alpha = 0.45f)
    }
}

@Composable
fun appleGlassBorderColor(): Color {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (darkTheme) 0.35f else 0.60f)
}

@Composable
fun applePrimaryTextColor(): Color = MaterialTheme.colorScheme.onBackground

@Composable
fun appleSecondaryTextColor(): Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)

@Composable
fun appleTertiaryTextColor(): Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.54f)

@Composable
fun appleDividerColor(): Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.56f)

@Composable
fun appleSearchBubbleColor(): Color {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (darkTheme) 0.42f else 0.56f)
}

@Composable
fun appleSelectedContainerColor(): Color {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return MaterialTheme.colorScheme.primary.copy(alpha = if (darkTheme) 0.16f else 0.12f)
}

fun extractDominantColor(bitmap: Bitmap): Color {
    val palette = Palette.from(bitmap)
        .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
        .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
        .generate()

    val swatchColor = listOfNotNull(
        palette.vibrantSwatch?.rgb,
        palette.dominantSwatch?.rgb,
        palette.mutedSwatch?.rgb,
        palette.darkVibrantSwatch?.rgb,
        palette.darkMutedSwatch?.rgb,
    ).firstOrNull()

    return swatchColor?.let(::Color)?.let(::normalizeAccentColor)
        ?: TamedAppleColors.AccentFallback
}

fun normalizeAccentColor(color: Color): Color {
    val alphaFixed = color.copy(alpha = 1f)
    val baseline = if (ColorUtils.calculateLuminance(alphaFixed.toArgb()) < 0.12f) {
        lerp(alphaFixed, Color.White, 0.18f)
    } else {
        alphaFixed
    }
    return lerp(baseline, TamedAppleColors.AccentFallback, 0.12f)
}

fun artworkBackdropBrush(accent: Color): Brush = Brush.verticalGradient(
    colors = listOf(
        lerp(accent, Color.Black, 0.32f).copy(alpha = 0.88f),
        lerp(accent, TamedAppleColors.Background, 0.64f).copy(alpha = 0.92f),
        TamedAppleColors.Background,
    ),
)

fun artworkCardOverlay(accent: Color): Brush = Brush.verticalGradient(
    colors = listOf(
        Color.Transparent,
        Color.Transparent,
        Color.Black.copy(alpha = 0.12f),
        lerp(accent, Color.Black, 0.68f).copy(alpha = 0.92f),
    ),
    startY = 0f,
    endY = Float.POSITIVE_INFINITY,
)

object BackdropCache {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, String>()
    @Volatile
    var lastThumbnail: String? = null

    fun put(id: String?, thumbnailUrl: String?) {
        if (!id.isNullOrEmpty() && !thumbnailUrl.isNullOrEmpty()) {
            cache[id] = thumbnailUrl
            lastThumbnail = thumbnailUrl
        }
    }

    fun get(id: String?): String? {
        if (id.isNullOrEmpty()) return null
        return cache[id]
    }
}

object BackdropPaletteCache {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, List<Color>>()
    fun get(url: String?): List<Color>? {
        if (url.isNullOrBlank()) return null
        return cache[url]
    }
    fun put(url: String?, colors: List<Color>) {
        if (!url.isNullOrBlank() && colors.isNotEmpty()) {
            cache[url] = colors
        }
    }
}

class PageBackdropState {
    var activeColor by mutableStateOf<Color?>(null)
    private var activeOwnerId: Any? = null

    fun setActive(ownerId: Any, color: Color?) {
        activeOwnerId = ownerId
        activeColor = color
    }

    fun clear(ownerId: Any) {
        if (activeOwnerId == ownerId) {
            activeOwnerId = null
            activeColor = null
        }
    }
}

val LocalPageBackdropState = staticCompositionLocalOf { PageBackdropState() }

val LocalBackdropColors = staticCompositionLocalOf<List<Color>> {
    emptyList()
}

val LocalPlayerIsPlaying = staticCompositionLocalOf<Boolean> {
    false
}

@Composable
fun AmbientBackdrop(
    modifier: Modifier = Modifier,
    thumbnailUrl: String? = null,
    styleKey: Preferences.Key<String> = HomeBackgroundStyleKey,
    forceStyle: HomeBackgroundStyle? = null,
    useNowPlayingFallback: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val providedColors = LocalBackdropColors.current
    val isPlaying = LocalPlayerIsPlaying.current
    
    val primary = providedColors.getOrNull(0) ?: MaterialTheme.colorScheme.primary
    val secondary = providedColors.getOrNull(1) ?: MaterialTheme.colorScheme.secondary
    val tertiary = providedColors.getOrNull(2) ?: MaterialTheme.colorScheme.tertiary
    val background = MaterialTheme.colorScheme.background

    val (prefStyle) = rememberEnumPreference(
        styleKey,
        defaultValue = HomeBackgroundStyle.DEFAULT
    )
    val homeBackgroundStyle = forceStyle ?: prefStyle

    val playerConnection = LocalPlayerConnection.current
    val mediaMetadataState = playerConnection?.mediaMetadata?.collectAsState()
    val effectiveThumbnailUrl = thumbnailUrl ?: if (useNowPlayingFallback) mediaMetadataState?.value?.thumbnailUrl else null

    val context = LocalContext.current
    var extractedBackdropColors by remember {
        mutableStateOf(BackdropPaletteCache.get(effectiveThumbnailUrl) ?: emptyList())
    }

    LaunchedEffect(effectiveThumbnailUrl, homeBackgroundStyle) {
        if (homeBackgroundStyle != HomeBackgroundStyle.DEFAULT && effectiveThumbnailUrl != null) {
            val cached = BackdropPaletteCache.get(effectiveThumbnailUrl)
            if (cached != null) {
                extractedBackdropColors = cached
                return@LaunchedEffect
            }
            withContext(Dispatchers.IO) {
                try {
                    val request = ImageRequest.Builder(context)
                        .data(effectiveThumbnailUrl)
                        .size(100, 100)
                        .allowHardware(false)
                        .build()
                    val result = context.imageLoader.execute(request)
                    val bitmap = result.image?.toBitmap()
                    if (bitmap != null) {
                        val palette = withContext(Dispatchers.Default) {
                            Palette.from(bitmap)
                                .maximumColorCount(16)
                                .resizeBitmapArea(100 * 100)
                                .generate()
                        }
                        val dominantSwatch = palette.dominantSwatch
                            ?: palette.swatches.maxByOrNull { it.population }
                            ?: palette.mutedSwatch
                            ?: palette.vibrantSwatch

                        val dominantColor = dominantSwatch?.rgb?.let { Color(it) }
                        val secondaryColor = palette.mutedSwatch?.rgb?.let { Color(it) }
                            ?: palette.darkMutedSwatch?.rgb?.let { Color(it) }
                            ?: dominantColor
                        val tertiaryColor = palette.darkVibrantSwatch?.rgb?.let { Color(it) }
                            ?: dominantColor

                        val colors = listOfNotNull(dominantColor, secondaryColor, tertiaryColor)
                        if (colors.isNotEmpty()) {
                            BackdropPaletteCache.put(effectiveThumbnailUrl, colors)
                            extractedBackdropColors = colors
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else if (homeBackgroundStyle == HomeBackgroundStyle.DEFAULT) {
            extractedBackdropColors = emptyList()
        }
    }

    val activeColors = if (extractedBackdropColors.isNotEmpty()) extractedBackdropColors else providedColors

    val pageBackdropState = LocalPageBackdropState.current
    val ownerId = remember { Any() }
    DisposableEffect(ownerId, homeBackgroundStyle, activeColors) {
        if (homeBackgroundStyle != HomeBackgroundStyle.DEFAULT && activeColors.isNotEmpty()) {
            pageBackdropState.setActive(ownerId, activeColors.firstOrNull())
        } else {
            pageBackdropState.clear(ownerId)
        }
        onDispose {
            pageBackdropState.clear(ownerId)
        }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    if (homeBackgroundStyle == HomeBackgroundStyle.DEFAULT) {
        Box(
            modifier = modifier.fillMaxSize().background(background),
            content = content
        )
    } else if (homeBackgroundStyle == HomeBackgroundStyle.BACKDROP || homeBackgroundStyle == HomeBackgroundStyle.LIVE_MESH) {
        val baseBackdropColor = if (isDark) Color(0xFF0F0F11) else Color(0xFF141416)
        Box(modifier = modifier.fillMaxSize().background(baseBackdropColor)) {
            if (effectiveThumbnailUrl != null) {
                val infiniteTransition = rememberInfiniteTransition(label = "homeMeshRotation")
                
                val anchorRotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = -360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(80000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "anchorRotation"
                )
                
                val fastRotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(40000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "fastRotation"
                )
                
                val slowRotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(60000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "slowRotation"
                )

                AnimatedContent(
                    targetState = effectiveThumbnailUrl,
                    transitionSpec = {
                        fadeIn(tween(800)) togetherWith fadeOut(tween(800))
                    },
                    label = "homeMeshBackground",
                    modifier = Modifier.fillMaxSize()
                ) { targetUrl ->
                    val downscale = 6
                    BoxWithConstraints(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .requiredSize(maxWidth / downscale, maxHeight / downscale)
                                .graphicsLayer {
                                    scaleX = 2.2f * downscale
                                    scaleY = 2.2f * downscale
                                    alpha = if (isDark) 0.88f else 0.72f
                                }
                                .blur((100f / downscale).dp)
                        ) {
                            val matrix = remember { 
                                val m = ColorMatrix()
                                m.setToSaturation(1.8f)
                                m
                            }
                            val colorFilter = remember(matrix) { ColorFilter.colorMatrix(matrix) }
                            val painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(context)
                                    .data(targetUrl)
                                    .size(128, 128)
                                    .crossfade(true)
                                    .allowHardware(true)
                                    .build()
                            )

                            // Layer 1: The Anchor
                            Image(
                                painter = painter,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                colorFilter = colorFilter,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { rotationZ = anchorRotation }
                            )

                            // Layer 2: Fast Rotating Crop
                            Image(
                                painter = painter,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                colorFilter = colorFilter,
                                alignment = Alignment.TopStart,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { 
                                        rotationZ = fastRotation
                                        alpha = 0.6f
                                    }
                            )

                            // Layer 3: Slow Rotating Crop
                            Image(
                                painter = painter,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                colorFilter = colorFilter,
                                alignment = Alignment.BottomEnd,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { 
                                        rotationZ = slowRotation
                                        alpha = 0.5f
                                    }
                            )
                        }
                    }
                }
            } else if (homeBackgroundStyle == HomeBackgroundStyle.LIVE_MESH) {
                var progress by remember { mutableStateOf(0f) }
                LaunchedEffect(isPlaying) {
                    if (isPlaying) {
                        val startPlayTime = System.nanoTime()
                        val startProgress = progress
                        while (true) {
                            withFrameNanos { frameTimeNanos ->
                                val elapsedSeconds = (frameTimeNanos - startPlayTime) / 1_000_000_000f
                                progress = (startProgress + elapsedSeconds / 24f) % 1f
                            }
                        }
                    }
                }

                fun oscillate(min: Float, max: Float, phase: Float): Float {
                    val v = kotlin.math.sin(2f * kotlin.math.PI.toFloat() * (progress + phase)).toFloat()
                    return min + (max - min) * ((v + 1f) * 0.5f)
                }

                val fallbackModifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val alphaFactor = 1.0f
                        
                        val b1cx = oscillate(0.8f, 1.1f, 0.0f) * size.width
                        val b1cy = oscillate(-0.1f, 0.2f, 0.25f) * size.height
                        val b1r = oscillate(1.0f, 1.4f, 0.5f) * size.width
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(primary.copy(alpha = 0.28f * alphaFactor), Color.Transparent),
                                center = Offset(b1cx, b1cy),
                                radius = b1r
                            ),
                            radius = b1r,
                            center = Offset(b1cx, b1cy)
                        )
                        
                        val b2cx = oscillate(-0.1f, 0.2f, 0.33f) * size.width
                        val b2cy = oscillate(0.7f, 1.0f, 0.58f) * size.height
                        val b2r = oscillate(1.1f, 1.6f, 0.08f) * size.width
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(tertiary.copy(alpha = 0.25f * alphaFactor), Color.Transparent),
                                center = Offset(b2cx, b2cy),
                                radius = b2r
                            ),
                            radius = b2r,
                            center = Offset(b2cx, b2cy)
                        )

                        val b3cx = oscillate(0.1f, 0.4f, 0.66f) * size.width
                        val b3cy = oscillate(0.2f, 0.5f, 0.16f) * size.height
                        val b3r = oscillate(0.9f, 1.3f, 0.83f) * size.width
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(secondary.copy(alpha = 0.24f * alphaFactor), Color.Transparent),
                                center = Offset(b3cx, b3cy),
                                radius = b3r
                            ),
                            radius = b3r,
                            center = Offset(b3cx, b3cy)
                        )
                    }
                Box(modifier = Modifier.fillMaxSize().then(fallbackModifier))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(baseBackdropColor)
                )
            }
            if (activeColors.isNotEmpty()) {
                val themeColor = activeColors.first()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    themeColor.copy(alpha = if (isDark) 0.22f else 0.16f),
                                    themeColor.copy(alpha = if (isDark) 0.15f else 0.11f),
                                    themeColor.copy(alpha = if (isDark) 0.14f else 0.10f),
                                    themeColor.copy(alpha = if (isDark) 0.20f else 0.15f)
                                )
                            )
                        )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = if (isDark) 0.08f else 0.05f),
                                Color.Black.copy(alpha = if (isDark) 0.16f else 0.10f),
                                Color.Black.copy(alpha = if (isDark) 0.26f else 0.16f)
                            )
                        )
                    )
            )
            CompositionLocalProvider(
                LocalBackdropColors provides activeColors,
                LocalContentColor provides Color.White,
            ) {
                val currentScheme = MaterialTheme.colorScheme
                val ambientScheme = remember(currentScheme, activeColors, isDark) {
                    val rawPrimary = activeColors.firstOrNull() ?: currentScheme.primary
                    val primaryColor = if (isDark) {
                        val hsl = FloatArray(3)
                        ColorUtils.colorToHSL(rawPrimary.toArgb(), hsl)
                        if (hsl[2] < 0.60f) {
                            hsl[2] = 0.68f
                            hsl[1] = hsl[1].coerceAtLeast(0.55f)
                            Color(ColorUtils.HSLToColor(hsl))
                        } else {
                            rawPrimary
                        }
                    } else {
                        rawPrimary
                    }
                    currentScheme.copy(
                        primary = primaryColor,
                        background = baseBackdropColor,
                        surface = baseBackdropColor,
                        onBackground = Color.White,
                        onSurface = Color.White,
                        onSurfaceVariant = Color.White.copy(alpha = 0.78f),
                        secondaryContainer = primaryColor.copy(alpha = 0.25f),
                        onSecondaryContainer = Color.White,
                        surfaceContainer = primaryColor.copy(alpha = 0.18f),
                        surfaceContainerHigh = primaryColor.copy(alpha = 0.28f),
                        surfaceContainerHighest = primaryColor.copy(alpha = 0.38f),
                        surfaceVariant = primaryColor.copy(alpha = 0.16f),
                        outline = Color.White.copy(alpha = 0.35f),
                        outlineVariant = Color.White.copy(alpha = 0.20f),
                    )
                }
                MaterialTheme(colorScheme = ambientScheme) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        content()
                    }
                }
            }
        }
    } else {
        var progress by remember { mutableStateOf(0f) }

        LaunchedEffect(isPlaying) {
            if (isPlaying) {
                val startPlayTime = System.nanoTime()
                val startProgress = progress
                while (true) {
                    withFrameNanos { frameTimeNanos ->
                        val elapsedSeconds = (frameTimeNanos - startPlayTime) / 1_000_000_000f
                        progress = (startProgress + elapsedSeconds / 24f) % 1f
                    }
                }
            }
        }

        fun oscillate(min: Float, max: Float, phase: Float): Float {
            val v = kotlin.math.sin(2f * kotlin.math.PI.toFloat() * (progress + phase)).toFloat()
            return min + (max - min) * ((v + 1f) * 0.5f)
        }

        val drawingModifier = Modifier
            .fillMaxSize()
            .background(background)
            .drawBehind {
                val alphaFactor = if (isDark) 1.0f else 0.8f
                
                val b1cx = oscillate(0.8f, 1.1f, 0.0f) * size.width
                val b1cy = oscillate(-0.1f, 0.2f, 0.25f) * size.height
                val b1r = oscillate(1.0f, 1.4f, 0.5f) * size.width
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.65f * alphaFactor), Color.Transparent),
                        center = Offset(b1cx, b1cy),
                        radius = b1r
                    ),
                    radius = b1r,
                    center = Offset(b1cx, b1cy)
                )
                
                val b2cx = oscillate(-0.1f, 0.2f, 0.33f) * size.width
                val b2cy = oscillate(0.7f, 1.0f, 0.58f) * size.height
                val b2r = oscillate(1.1f, 1.6f, 0.08f) * size.width
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tertiary.copy(alpha = 0.55f * alphaFactor), Color.Transparent),
                        center = Offset(b2cx, b2cy),
                        radius = b2r
                    ),
                    radius = b2r,
                    center = Offset(b2cx, b2cy)
                )

                val b3cx = oscillate(0.1f, 0.4f, 0.66f) * size.width
                val b3cy = oscillate(0.2f, 0.5f, 0.16f) * size.height
                val b3r = oscillate(0.9f, 1.3f, 0.83f) * size.width
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(secondary.copy(alpha = 0.50f * alphaFactor), Color.Transparent),
                        center = Offset(b3cx, b3cy),
                        radius = b3r
                    ),
                    radius = b3r,
                    center = Offset(b3cx, b3cy)
                )
            }

        Box(
            modifier = modifier.fillMaxSize().then(drawingModifier),
            content = content
        )
    }
}

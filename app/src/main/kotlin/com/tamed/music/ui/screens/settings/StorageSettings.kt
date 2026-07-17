/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.tamed.music.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.annotation.ExperimentalCoilApi
import coil3.imageLoader
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.R
import com.tamed.music.constants.MaxCanvasCacheSizeKey
import com.tamed.music.constants.MaxImageCacheSizeKey
import com.tamed.music.constants.MaxSongCacheSizeKey
import com.tamed.music.constants.SmartTrimmerKey
import com.tamed.music.extensions.directorySizeBytes
import com.tamed.music.extensions.tryOrNull
import com.tamed.music.ui.component.ActionPromptDialog
import com.tamed.music.ui.component.DefaultDialog
import com.tamed.music.ui.component.IconButton
import com.tamed.music.ui.component.ListPreference
import com.tamed.music.ui.component.PreferenceEntry
import com.tamed.music.ui.component.SwitchPreference
import com.tamed.music.ui.component.PreferenceGroupTitle
import com.tamed.music.ui.player.CanvasArtworkPlaybackCache
import com.tamed.music.ui.utils.backToMain
import com.tamed.music.ui.utils.formatFileSize
import com.tamed.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StorageSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val imageDiskCache = context.imageLoader.diskCache ?: return
    val playerCache = LocalPlayerConnection.current?.service?.playerCache ?: return
    val downloadCache = LocalPlayerConnection.current?.service?.downloadCache ?: return
    
    val downloadCacheDir = remember { context.filesDir.resolve("download") }
    val playerCacheDir = remember { context.filesDir.resolve("exoplayer") }

    val coroutineScope = rememberCoroutineScope()
    val (smartTrimmer, onSmartTrimmerChange) = rememberPreference(
        key = SmartTrimmerKey,
        defaultValue = false
    )
    val (maxImageCacheSize, onMaxImageCacheSizeChange) = rememberPreference(
        key = MaxImageCacheSizeKey,
        defaultValue = 512
    )
    val (maxSongCacheSize, onMaxSongCacheSizeChange) = rememberPreference(
        key = MaxSongCacheSizeKey,
        defaultValue = 1024
    )
    val (maxCanvasCacheSize, onMaxCanvasCacheSizeChange) = rememberPreference(
        key = MaxCanvasCacheSizeKey,
        defaultValue = 256,
    )
    var clearCacheDialog by remember { mutableStateOf(false) }
    var clearDownloads by remember { mutableStateOf(false) }
    var clearImageCacheDialog by remember { mutableStateOf(false) }
    var clearCanvasCacheDialog by remember { mutableStateOf(false) }

    var imageCacheSize by remember {
        mutableStateOf(imageDiskCache.size)
    }
    var playerCacheSize by remember {
        mutableStateOf(0L)
    }
    var downloadCacheSize by remember {
        mutableStateOf(0L)
    }
    var canvasCacheSize by remember {
        mutableStateOf(CanvasArtworkPlaybackCache.size())
    }
    val imageCacheProgress by animateFloatAsState(
        targetValue = if (imageDiskCache.maxSize > 0) {
            (imageCacheSize.toFloat() / imageDiskCache.maxSize).coerceIn(0f, 1f)
        } else 0f,
        label = "imageCacheProgress",
    )
    val maxSongCacheSizeBytes = if (maxSongCacheSize > 0) maxSongCacheSize * 1024 * 1024L else 0L
    val playerCacheProgress by animateFloatAsState(
        targetValue = if (maxSongCacheSizeBytes > 0) {
            (playerCacheSize.toFloat() / maxSongCacheSizeBytes).coerceIn(0f, 1f)
        } else 0f,
        label = "playerCacheProgress",
    )
    val canvasCacheProgress by animateFloatAsState(
        targetValue = if (maxCanvasCacheSize > 0) {
            (canvasCacheSize.toFloat() / maxCanvasCacheSize).coerceIn(0f, 1f)
        } else 0f,
        label = "canvasCacheProgress",
    )

    val isSmartTrimmerAvailable = maxImageCacheSize != 0 || maxSongCacheSize != 0
    LaunchedEffect(isSmartTrimmerAvailable) {
        if (!isSmartTrimmerAvailable && smartTrimmer) onSmartTrimmerChange(false)
    }

    LaunchedEffect(maxImageCacheSize) {
        if (maxImageCacheSize == 0) {
            coroutineScope.launch(Dispatchers.IO) {
                imageDiskCache.clear()
                com.tamed.music.utils.ArtworkStorage.clear(context)
            }
        }
    }
    LaunchedEffect(maxSongCacheSize) {
        if (maxSongCacheSize == 0) {
            coroutineScope.launch(Dispatchers.IO) {
                playerCache.keys.forEach { key ->
                    playerCache.removeResource(key)
                }
            }
        }
    }
    LaunchedEffect(maxCanvasCacheSize) {
        CanvasArtworkPlaybackCache.setMaxSize(maxCanvasCacheSize)
        if (maxCanvasCacheSize == 0) {
            CanvasArtworkPlaybackCache.clear()
        }
    }

    LaunchedEffect(imageDiskCache) {
        while (isActive) {
            delay(500)
            imageCacheSize = imageDiskCache.size
        }
    }
    LaunchedEffect(playerCache, playerCacheDir) {
        while (isActive) {
            delay(500)
            playerCacheSize =
                withContext(Dispatchers.IO) {
                    val cacheSpace = tryOrNull { playerCache.cacheSpace } ?: 0L
                    if (cacheSpace == 0L) playerCacheDir.directorySizeBytes() else cacheSpace
                }
        }
    }
    LaunchedEffect(downloadCache, downloadCacheDir) {
        while (isActive) {
            delay(500)
            downloadCacheSize =
                withContext(Dispatchers.IO) {
                    val cacheSpace = tryOrNull { downloadCache.cacheSpace } ?: 0L
                    if (cacheSpace == 0L) downloadCacheDir.directorySizeBytes() else cacheSpace
                }
        }
    }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(500)
            canvasCacheSize = CanvasArtworkPlaybackCache.size()
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )

        ExpressiveSwitchRow(
            icon = painterResource(R.drawable.motion_photos_on),
            title = stringResource(R.string.smart_trimmer),
            subtitle = stringResource(R.string.smart_trimmer_description),
            checked = smartTrimmer && isSmartTrimmerAvailable,
            onCheckedChange = onSmartTrimmerChange,
        )

        Spacer(Modifier.height(16.dp))

        // --- Section: Downloads ---
        ExpressiveSectionCard(title = stringResource(R.string.downloaded_songs)) {
            CacheRow(
                icon = R.drawable.ic_download,
                title = stringResource(R.string.downloaded_songs),
                description = stringResource(R.string.size_used, formatFileSize(downloadCacheSize)),
                progress = null,
                actions = {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.clear_all_downloads)) },
                        onClick = { clearDownloads = true },
                    )
                }
            )
        }

        if (clearDownloads) {
            ActionPromptDialog(
                title = stringResource(R.string.clear_all_downloads),
                onDismiss = { clearDownloads = false },
                onConfirm = {
                    coroutineScope.launch(Dispatchers.IO) {
                        downloadCache.keys.forEach { key ->
                            downloadCache.removeResource(key)
                        }
                    }
                    clearDownloads = false
                },
                onCancel = { clearDownloads = false },
                content = {
                    Text(text = stringResource(R.string.clear_downloads_dialog))
                }
            )
        }

        // --- Section: Song cache ---
        ExpressiveSectionCard(title = stringResource(R.string.song_cache)) {
            CacheRow(
                icon = R.drawable.ic_music,
                title = stringResource(R.string.song_cache),
                description = if (maxSongCacheSize == -1) {
                    stringResource(R.string.size_used, formatFileSize(playerCacheSize))
                } else {
                    "${formatFileSize(playerCacheSize)} / ${formatFileSize(maxSongCacheSize * 1024 * 1024L)}"
                },
                progress = if (maxSongCacheSize > 0) playerCacheProgress else null,
                actions = {
                    ListPreference(
                        title = { Text(stringResource(R.string.max_cache_size)) },
                        selectedValue = maxSongCacheSize,
                        values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192, -1),
                        valueText = {
                            when (it) {
                                0 -> stringResource(R.string.disable)
                                -1 -> stringResource(R.string.unlimited)
                                else -> formatFileSize(it * 1024 * 1024L)
                            }
                        },
                        onValueSelected = onMaxSongCacheSizeChange,
                    )
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.clear_song_cache)) },
                        onClick = { clearCacheDialog = true },
                    )
                }
            )
        }

        if (clearCacheDialog) {
            ActionPromptDialog(
                title = stringResource(R.string.clear_song_cache),
                onDismiss = { clearCacheDialog = false },
                onConfirm = {
                    coroutineScope.launch(Dispatchers.IO) {
                        playerCache.keys.forEach { key ->
                            playerCache.removeResource(key)
                        }
                    }
                    clearCacheDialog = false
                },
                onCancel = { clearCacheDialog = false },
                content = {
                    Text(text = stringResource(R.string.clear_song_cache_dialog))
                }
            )
        }

        // --- Section: Image cache ---
        ExpressiveSectionCard(title = stringResource(R.string.image_cache)) {
            CacheRow(
                icon = R.drawable.image,
                title = stringResource(R.string.image_cache),
                description = if (maxImageCacheSize > 0) {
                    "${formatFileSize(imageCacheSize)} / ${formatFileSize(imageDiskCache.maxSize)}"
                } else {
                    stringResource(R.string.disable)
                },
                progress = if (maxImageCacheSize > 0) imageCacheProgress else null,
                actions = {
                    ListPreference(
                        title = { Text(stringResource(R.string.max_cache_size)) },
                        selectedValue = maxImageCacheSize,
                        values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192),
                        valueText = {
                            when (it) {
                                0 -> stringResource(R.string.disable)
                                else -> formatFileSize(it * 1024 * 1024L)
                            }
                        },
                        onValueSelected = onMaxImageCacheSizeChange,
                    )
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.clear_image_cache)) },
                        onClick = { clearImageCacheDialog = true },
                    )
                }
            )
        }

        if (clearImageCacheDialog) {
            ActionPromptDialog(
                title = stringResource(R.string.clear_image_cache),
                onDismiss = { clearImageCacheDialog = false },
                onConfirm = {
                    coroutineScope.launch(Dispatchers.IO) {
                        imageDiskCache.clear()
                        com.tamed.music.utils.ArtworkStorage.clear(context)
                    }
                    clearImageCacheDialog = false
                },
                onCancel = { clearImageCacheDialog = false },
                content = {
                    Text(text = stringResource(R.string.clear_image_cache_dialog))
                }
            )
        }

        // --- Section: Canvas cache ---
        ExpressiveSectionCard(title = stringResource(R.string.canvas_cache)) {
            CacheRow(
                icon = R.drawable.motion_photos_on,
                title = stringResource(R.string.canvas_cache),
                description = if (maxCanvasCacheSize > 0) {
                    stringResource(
                        R.string.canvas_cache_usage,
                        stringResource(R.string.canvas_cache_items, canvasCacheSize),
                        stringResource(R.string.canvas_cache_items, maxCanvasCacheSize),
                    )
                } else {
                    stringResource(R.string.disable)
                },
                progress = if (maxCanvasCacheSize > 0) canvasCacheProgress else null,
                actions = {
                    ListPreference(
                        title = { Text(stringResource(R.string.max_cache_size)) },
                        selectedValue = maxCanvasCacheSize,
                        values = listOf(0, 64, 128, 256, 512, 1024),
                        valueText = {
                            when (it) {
                                0 -> stringResource(R.string.disable)
                                else -> stringResource(R.string.canvas_cache_items, it)
                            }
                        },
                        onValueSelected = onMaxCanvasCacheSizeChange,
                    )
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.clear_canvas_cache)) },
                        onClick = { clearCanvasCacheDialog = true },
                    )
                }
            )
        }

        if (clearCanvasCacheDialog) {
            ActionPromptDialog(
                title = stringResource(R.string.clear_canvas_cache),
                onDismiss = { clearCanvasCacheDialog = false },
                onConfirm = {
                    CanvasArtworkPlaybackCache.clear()
                    clearCanvasCacheDialog = false
                },
                onCancel = { clearCanvasCacheDialog = false },
                content = {
                    Text(text = stringResource(R.string.clear_canvas_cache_dialog))
                }
            )
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.storage)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    )
}

@Composable
fun CacheRow(
    icon: Int,
    title: String,
    description: String,
    progress: Float?,
    actions: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = SettingsDimensions.RowHorizontalPadding,
                vertical = 8.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveRowIcon(
                icon = painterResource(icon),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.width(SettingsDimensions.RowIconTextSpacing))
            
            Column {
                Text(
                    text = title, 
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (progress != null) {
            Row(
                modifier = Modifier.padding(
                    horizontal = SettingsDimensions.RowHorizontalPadding + 8.dp,
                    vertical = 8.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearWavyProgressIndicator(
                    progress = { progress ?: 0f },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(Modifier.height(4.dp))
        
        actions()
    }
}

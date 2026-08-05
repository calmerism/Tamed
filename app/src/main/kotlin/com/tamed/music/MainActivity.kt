/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.tamed.music

import android.annotation.SuppressLint
import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.OpenableColumns
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.graphicsLayer
import com.tamed.music.ui.component.AppFloatingNavBar
import com.tamed.music.ui.component.LocalAppBackdrop
import com.tamed.music.ui.component.LocalNavSearchState
import com.tamed.music.ui.component.NavSearchState
import com.tamed.music.ui.component.backdrop.backdrops.layerBackdrop
import com.tamed.music.ui.component.backdrop.backdrops.rememberLayerBackdrop
import com.tamed.music.ui.component.floatingtabbar.rememberFloatingTabBarScrollConnection
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.core.content.ContextCompat
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.valentinilk.shimmer.LocalShimmerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.tamed.music.utils.PreferenceStore
import kotlinx.coroutines.withContext
import com.tamed.music.constants.AppBarHeight
import com.tamed.music.constants.AppLanguageKey
import com.tamed.music.constants.CustomThemeColorKey
import com.tamed.music.constants.DarkModeKey
import com.tamed.music.constants.DefaultOpenTabKey
import com.tamed.music.constants.DisableScreenshotKey
import com.tamed.music.constants.DynamicThemeKey
import com.tamed.music.constants.FloatingToolbarBottomPadding
import com.tamed.music.constants.FloatingToolbarHeight
import com.tamed.music.constants.FloatingToolbarHorizontalPadding
import com.tamed.music.constants.MiniPlayerBottomSpacing
import com.tamed.music.constants.MiniPlayerHeight
import com.tamed.music.constants.MiniPlayerLastAnchorKey
import com.tamed.music.constants.NavigationBarAnimationSpec
import com.tamed.music.constants.PauseSearchHistoryKey
import com.tamed.music.constants.PureBlackKey
import com.tamed.music.constants.SYSTEM_DEFAULT
import com.tamed.music.constants.SearchSource
import com.tamed.music.constants.SearchSourceKey
import com.tamed.music.constants.StopMusicOnTaskClearKey
import com.tamed.music.constants.UseSystemFontKey
import com.tamed.music.constants.SelectedFontKey
import com.tamed.music.db.MusicDatabase
import com.tamed.music.db.entities.SearchHistory
import com.tamed.music.db.entities.Album
import com.tamed.music.db.entities.Artist
import com.tamed.music.db.entities.Playlist
import com.tamed.music.db.entities.Song
import com.tamed.music.innertube.YouTube
import com.tamed.music.innertube.models.AlbumItem
import com.tamed.music.innertube.models.ArtistItem
import com.tamed.music.innertube.models.PlaylistItem
import com.tamed.music.innertube.models.SongItem
import com.tamed.music.extensions.toMediaItem
import com.tamed.music.models.MediaMetadata
import com.tamed.music.models.toMediaMetadata
import com.tamed.music.playback.DownloadUtil
import com.tamed.music.playback.MusicService
import com.tamed.music.playback.MusicService.MusicBinder
import com.tamed.music.playback.PlayerConnection
import com.tamed.music.playback.queues.LocalAlbumRadio
import com.tamed.music.playback.queues.ListQueue
import com.tamed.music.playback.queues.YouTubeAlbumRadio
import com.tamed.music.playback.queues.YouTubeQueue
import com.tamed.music.ui.component.BottomSheetMenu
import com.tamed.music.ui.component.BottomSheetPage
import com.tamed.music.ui.component.COLLAPSED_ANCHOR
import com.tamed.music.ui.component.CreatePlaylistDialog
import com.tamed.music.ui.component.DISMISSED_ANCHOR
import com.tamed.music.ui.component.EXPANDED_ANCHOR
import com.tamed.music.ui.component.GlassBottomBar
import com.tamed.music.ui.component.GlassBottomBarItem
import com.tamed.music.ui.component.GlassContainer
import com.tamed.music.ui.theme.appleGlassColor
import com.tamed.music.ui.component.LocalGlassScope
import com.tamed.music.ui.component.IconButton
import com.tamed.music.ui.component.LocalBottomSheetPageState
import com.tamed.music.ui.component.LocalMenuState
import com.tamed.music.ui.component.NetworkStatusBanner
import com.mikepenz.markdown.m3.Markdown
import com.tamed.music.ui.component.TopSearch
import com.tamed.music.ui.component.rememberBottomSheetState
import com.tamed.music.ui.component.shimmer.ShimmerTheme
import com.tamed.music.ui.menu.YouTubeSongMenu
import com.tamed.music.ui.player.BottomSheetPlayer
import com.tamed.music.ui.screens.LOGIN_URL_ARGUMENT
import com.tamed.music.ui.screens.Screens
import com.tamed.music.ui.screens.buildLoginRoute
import com.tamed.music.ui.screens.navigationBuilder
import com.tamed.music.ui.screens.search.LocalSearchScreen
import com.tamed.music.ui.screens.search.OnlineSearchScreen
import com.tamed.music.ui.screens.settings.DarkMode
import com.tamed.music.ui.screens.settings.DiscordPresenceManager
import com.tamed.music.ui.screens.settings.NavigationTab
import com.tamed.music.ui.theme.TamedTheme
import com.tamed.music.ui.theme.ColorSaver
import com.tamed.music.ui.theme.DefaultThemeColor
import com.tamed.music.ui.theme.extractThemeColor
import com.tamed.music.ui.theme.extractGradientColors
import com.tamed.music.ui.utils.appBarScrollBehavior
import com.tamed.music.ui.utils.backToMain
import com.tamed.music.ui.utils.resetHeightOffset
import com.tamed.music.utils.SyncUtils
import com.tamed.music.utils.Updater
import com.tamed.music.utils.dataStore
import com.tamed.music.utils.get
import com.tamed.music.utils.getAsync
import com.tamed.music.utils.rememberEnumPreference
import com.tamed.music.utils.rememberPreference
import com.tamed.music.utils.reportException
import com.tamed.music.utils.setAppLocale
import com.tamed.music.viewmodels.HomeViewModel
import com.tamed.music.viewmodels.NetworkBannerViewModel
import java.net.URLDecoder
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random
import kotlin.time.Duration.Companion.days

@Suppress("DEPRECATION", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils

    private lateinit var navController: NavHostController
    private var pendingIntent: Intent? = null
    private var pendingDeepLinkSong: PendingDeepLinkSong? = null
    private var pendingTogetherJoinLink: String? = null
    private var latestVersionName by mutableStateOf(BuildConfig.VERSION_NAME)

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private var isMusicServiceBound = false
    
    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                isMusicServiceBound = true
                if (service is MusicBinder) {
                    playerConnection =
                        PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                    playPendingDeepLinkSongIfReady()
                    joinPendingTogetherIfReady()
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                isMusicServiceBound = false
                playerConnection?.dispose()
                playerConnection = null
            }
        }

    private data class PendingDeepLinkSong(
        val mediaItem: MediaItem,
    )

    private fun playPendingDeepLinkSongIfReady() {
        val pending = pendingDeepLinkSong ?: return
        val connection = playerConnection ?: return
        pendingDeepLinkSong = null
        connection.playQueue(ListQueue(items = listOf(pending.mediaItem)))
    }

    private fun joinPendingTogetherIfReady() {
        val pending = pendingTogetherJoinLink ?: return
        val connection = playerConnection ?: return
        pendingTogetherJoinLink = null
        lifecycleScope.launch(Dispatchers.IO) {
            val displayName =
                runCatching { dataStore.data.first()[com.tamed.music.constants.TogetherDisplayNameKey] }
                    .getOrNull()
                    ?.trim()
                    .orEmpty()
                    .ifBlank { Build.MODEL ?: getString(R.string.app_name) }
            withContext(Dispatchers.Main) {
                connection.service.joinTogether(pending, displayName)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isMusicServiceBound =
            bindService(
                Intent(this, MusicService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        playPendingDeepLinkSongIfReady()
    }

    private fun safeUnbindMusicService() {
        if (!isMusicServiceBound) return
        try {
            unbindService(serviceConnection)
        } catch (e: IllegalArgumentException) {
        } catch (e: Exception) {
            reportException(e)
        } finally {
            isMusicServiceBound = false
        }
    }

    override fun onStop() {
        safeUnbindMusicService()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Only clear/stop presence when the activity is actually finishing (not on rotation)
        // and do not clear it for transient configuration changes.
        if (isFinishing && !isChangingConfigurations) {
            try { DiscordPresenceManager.stop() } catch (_: Exception) {}
        }

        val shouldStopOnTaskClear =
            if (!isFinishing) {
                false
            } else {
                dataStore.get(StopMusicOnTaskClearKey, true)
            }

        if (shouldStopOnTaskClear) {
            safeUnbindMusicService()
            stopService(Intent(this, MusicService::class.java))
            playerConnection = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::navController.isInitialized) {
            handleDeepLinkIntent(intent, navController)
        } else {
            pendingIntent = intent
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val initialLocale = PreferenceStore.get(AppLanguageKey)
                ?.takeUnless { it == SYSTEM_DEFAULT }
                ?.let { Locale.forLanguageTag(it) }
                ?: Locale.getDefault()
            setAppLocale(this, initialLocale)

            lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    dataStore.data.first()[AppLanguageKey]
                }.onSuccess { lang ->
                    val targetLocale = lang
                        ?.takeUnless { it == SYSTEM_DEFAULT }
                        ?.let { Locale.forLanguageTag(it) }
                        ?: Locale.getDefault()
                    if (targetLocale != initialLocale) {
                        withContext(Dispatchers.Main) {
                            setAppLocale(this@MainActivity, targetLocale)
                            recreate()
                        }
                    }
                }
            }
        }
        
        lifecycleScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    withContext(Dispatchers.Main) {
                        if (it) {
                            window.setFlags(
                                WindowManager.LayoutParams.FLAG_SECURE,
                                WindowManager.LayoutParams.FLAG_SECURE,
                            )
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    }
                }
        }

        setContent {
            val notificationPermissionLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    if (isGranted) {
                        playerConnection?.service?.refreshPlaybackNotification()
                    }
                }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                if (System.currentTimeMillis() - Updater.lastCheckTime > 1.days.inWholeMilliseconds) {
                    Updater.getLatestVersionName().onSuccess {
                        latestVersionName = it
                    }
                }
                com.tamed.music.utils.UpdateNotificationManager.checkForUpdates(this@MainActivity)
            }

                    // Use remembered instances so the same state object is used everywhere
                    // (previously retrieving the composition local directly created different
                    // instances in different composition scopes which caused the update
                    // bottom sheet to not appear and overlay interactions to be blocked).
                    val bottomSheetPageState = remember { com.tamed.music.ui.component.BottomSheetPageState() }
                    val menuState = remember { com.tamed.music.ui.component.MenuState() }
                    val uriHandler = LocalUriHandler.current
                    val releaseNotesState = remember { mutableStateOf<String?>(null) }
                    val updateSheetContent: @Composable ColumnScope.() -> Unit = { // receiver: ColumnScope
                        Text(
                            text = stringResource(R.string.new_update_available),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(top = 16.dp)
                        )

                        Spacer(Modifier.height(8.dp))

                        androidx.compose.material3.OutlinedButton(
                            onClick = {},
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 5.dp,
                                vertical = 5.dp
                            ),
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(text = latestVersionName, style = MaterialTheme.typography.labelLarge)
                        }

                        Spacer(Modifier.height(12.dp))

                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                        ) {
                            val notes = releaseNotesState.value
                            if (notes != null && notes.isNotBlank()) {
                                Markdown(
                                    content = notes,
                                    modifier = Modifier
                                        .fillMaxWidth().padding(end = 8.dp)
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.release_notes_unavailable),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        androidx.compose.material3.Button(
                            onClick = {
                                try {
                                    uriHandler.openUri(Updater.getLatestDownloadUrl())
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(text = stringResource(R.string.update_text))
                        }
                    }

                    // fetch release notes and show sheet when a new version is detected
                    LaunchedEffect(latestVersionName) {
                        if (!Updater.isSameVersion(latestVersionName, BuildConfig.VERSION_NAME)) {
                            Updater.getLatestReleaseNotes().onSuccess {
                                releaseNotesState.value = it
                            }.onFailure {
                                releaseNotesState.value = null
                            }

                            bottomSheetPageState.show(updateSheetContent)
                        }
                    }

            val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
            val customThemeColorValue by rememberPreference(CustomThemeColorKey, defaultValue = "default")
            val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
            val useSystemFont by rememberPreference(UseSystemFontKey, defaultValue = false)
            val selectedFont by rememberPreference(SelectedFontKey, defaultValue = com.tamed.music.constants.AppFont.SYSTEM.value)
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val useDarkTheme =
                remember(darkTheme, isSystemInDarkTheme) {
                    if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
                }
            LaunchedEffect(useDarkTheme) {
                setSystemBarAppearance(useDarkTheme)
            }
            val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
            val pureBlack = pureBlackEnabled && useDarkTheme

            val customThemeSeedPalette = remember(customThemeColorValue) {
                if (customThemeColorValue.startsWith("#")) {
                    null
                } else if (customThemeColorValue.startsWith("seedPalette:")) {
                    com.tamed.music.ui.theme.ThemeSeedPaletteCodec.decodeFromPreference(customThemeColorValue)
                } else {
                    com.tamed.music.ui.screens.settings.ThemePalettes
                        .findById(customThemeColorValue)
                        ?.let {
                            com.tamed.music.ui.theme.ThemeSeedPalette(
                                primary = it.primary,
                                secondary = it.secondary,
                                tertiary = it.tertiary,
                                neutral = it.neutral,
                            )
                        }
                }
            }

            val customThemeColor = remember(customThemeColorValue, customThemeSeedPalette) {
                if (customThemeColorValue.startsWith("#")) {
                    try {
                        val colorString = customThemeColorValue.removePrefix("#")
                        Color(android.graphics.Color.parseColor("#$colorString"))
                    } catch (e: Exception) {
                        DefaultThemeColor
                    }
                } else {
                    customThemeSeedPalette?.primary ?: DefaultThemeColor
                }
            }

            var themeColor by rememberSaveable(stateSaver = ColorSaver) {
                mutableStateOf(DefaultThemeColor)
            }
            var currentBackdropColors by remember {
                mutableStateOf<List<Color>>(emptyList())
            }

            LaunchedEffect(playerConnection, enableDynamicTheme, isSystemInDarkTheme, customThemeColor) {
                val playerConnection = playerConnection
                if (!enableDynamicTheme || playerConnection == null) {
                    themeColor = if (!enableDynamicTheme) customThemeColor else DefaultThemeColor
                    currentBackdropColors = emptyList()
                    return@LaunchedEffect
                }
                playerConnection.service.currentMediaMetadata.collectLatest { song ->
                    if (song != null) {
                        withContext(Dispatchers.Default) {
                            try {
                                val result = imageLoader.execute(
                                    ImageRequest
                                        .Builder(this@MainActivity)
                                        .data(song.thumbnailUrl)
                                        .allowHardware(false)
                                        .build(),
                                )
                                val bitmap = result.image?.toBitmap()
                                val extractedColor = bitmap?.extractThemeColor()
                                val gradientColors = bitmap?.extractGradientColors() ?: emptyList()
                                withContext(Dispatchers.Main) {
                                    themeColor = extractedColor ?: DefaultThemeColor
                                    currentBackdropColors = gradientColors
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    themeColor = DefaultThemeColor
                                    currentBackdropColors = emptyList()
                                }
                            }
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            themeColor = DefaultThemeColor
                        } else {
                            themeColor = customThemeColor
                        }
                        currentBackdropColors = emptyList()
                    }
                }
            }

            TamedTheme(
                darkTheme = useDarkTheme,
                pureBlack = pureBlack,
                themeColor = themeColor,
                seedPalette = if (!enableDynamicTheme) customThemeSeedPalette else null,
                useSystemFont = useSystemFont,
                selectedFont = selectedFont,
                backdropColors = currentBackdropColors,
            ) {
                    BoxWithConstraints(
                        modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                if(pureBlack) Color.Black else MaterialTheme.colorScheme.surface
                            )
                    ) {
                    val focusManager = LocalFocusManager.current
                    val density = LocalDensity.current
                    val windowsInsets = WindowInsets.systemBars
                    val topInset = with(density) { windowsInsets.getTop(density).toDp() }
                    val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
                    val bottomInsetDp = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
                        
                    val useRail = currentWindowAdaptiveInfo().windowSizeClass
                        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

                    val navController = rememberNavController()
                    val homeViewModel: HomeViewModel = hiltViewModel()
                    val networkBannerViewModel: NetworkBannerViewModel = hiltViewModel()
                    val allLocalItems by homeViewModel.allLocalItems.collectAsState()
                    val allYtItems by homeViewModel.allYtItems.collectAsState()
                    val networkBannerState by networkBannerViewModel.bannerState.collectAsStateWithLifecycle()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }
                    val (previousTab) = rememberSaveable { mutableStateOf("home") }
                    val currentRoute = navBackStackEntry?.destination?.route
                    val isYearInMusicScreen = currentRoute == "year_in_music"

                    val navigationItems = remember { Screens.MainScreens }
                    val (savedMiniPlayerAnchor, setSavedMiniPlayerAnchor) = rememberPreference(
                        MiniPlayerLastAnchorKey,
                        defaultValue = COLLAPSED_ANCHOR
                    )
                    val defaultOpenTab by rememberEnumPreference(DefaultOpenTabKey, NavigationTab.HOME)
                    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)
                    val tabOpenedFromShortcut =
                        remember {
                            when (intent?.action) {
                                ACTION_LIBRARY -> NavigationTab.LIBRARY
                                ACTION_SEARCH -> NavigationTab.SEARCH
                                else -> null
                            }
                        }

                    val topLevelScreens =
                        listOf(
                            Screens.Home.route,
                            Screens.Search.route,
                            Screens.MoodAndGenres.route,
                            Screens.Library.route,
                            "settings",
                        )

                    val (query, onQueryChange) =
                        rememberSaveable(stateSaver = TextFieldValue.Saver) {
                            mutableStateOf(TextFieldValue())
                        }

                    var active by rememberSaveable {
                        mutableStateOf(false)
                    }

                    val onActiveChange: (Boolean) -> Unit = { newActive ->
                        active = newActive
                        if (!newActive) {
                            focusManager.clearFocus()
                            if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                onQueryChange(TextFieldValue())
                            }
                        }
                    }

                    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)

                    val searchBarFocusRequester = remember { FocusRequester() }

                    val onSearch: (String) -> Unit = {
                        if (it.isNotEmpty()) {
                            onActiveChange(false)
                            navController.navigate("search/${URLEncoder.encode(it, "UTF-8")}")
                            if (!pauseSearchHistory) {
                                database.query {
                                    insert(SearchHistory(query = it))
                                }
                            }
                        }
                    }

                    var openSearchImmediately: Boolean by remember {
                        mutableStateOf(intent?.action == ACTION_SEARCH)
                    }

                    val shouldShowSearchBar =
                        remember(active, navBackStackEntry) {
                            active ||
                                    navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                    navBackStackEntry?.destination?.route?.startsWith("search/") == true
                        }

                    var searchKeyboardActive by rememberSaveable { mutableStateOf(false) }
                    val searchFocusRequester = remember { FocusRequester() }
                    var searchVisualOverride by remember { mutableStateOf<Boolean?>(null) }

                    val inSearchScreen by remember {
                        derivedStateOf {
                            currentRoute?.startsWith("search/") == true ||
                                currentRoute == Screens.Search.route
                        }
                    }
                    val inSearchInputScreen by remember {
                        derivedStateOf { currentRoute == Screens.Search.route }
                    }
                    LaunchedEffect(inSearchScreen) {
                        if (!inSearchScreen) searchKeyboardActive = false
                    }

                    val enterSearch: () -> Unit = remember(navController, lifecycleScope) {
                        {
                            searchKeyboardActive = true
                            searchVisualOverride = true
                            lifecycleScope.launch {
                                delay(220L)
                                navController.navigate(Screens.Search.route) { launchSingleTop = true }
                                delay(150L)
                                searchVisualOverride = null
                            }
                        }
                    }
                    val exitSearch: () -> Unit = remember(navController, lifecycleScope) {
                        {
                            searchKeyboardActive = false
                            searchVisualOverride = false
                            onQueryChange(TextFieldValue(""))
                            lifecycleScope.launch {
                                delay(100L)
                                navController.navigate(Screens.Home.route) {
                                    popUpTo(Screens.Home.route) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                                delay(150L)
                                searchVisualOverride = null
                            }
                        }
                    }

                    val navSearchState = NavSearchState(
                        visualActive = searchVisualOverride ?: inSearchScreen,
                        keyboardActive = searchKeyboardActive,
                        query = query,
                        onQueryChange = onQueryChange,
                        onSubmit = onSearch,
                        searchSource = searchSource,
                        onToggleSource = {
                            searchSource = if (searchSource == SearchSource.ONLINE) SearchSource.LOCAL else SearchSource.ONLINE
                        },
                        onTapSearchIcon = enterSearch,
                        onTapBar = {
                            if (inSearchScreen && !inSearchInputScreen) {
                                navController.popBackStack(Screens.Search.route, inclusive = false)
                            }
                            searchKeyboardActive = true
                        },
                        onExit = exitSearch,
                        onCloseKeyboard = { searchKeyboardActive = false },
                        focusRequester = searchFocusRequester,
                    )

                    val shouldShowNavigationBar =
                        remember(currentRoute, navBackStackEntry) {
                            when {
                                currentRoute == "settings" || currentRoute?.startsWith("settings") == true -> false
                                currentRoute in setOf("login", "equalizer", "wrapped", "update", "listen_together/chat") -> false
                                else -> true
                            }
                        }

                    fun getBottomNavPadding(): Dp {
                        return if (shouldShowNavigationBar && !useRail) {
                            FloatingToolbarHeight
                        } else {
                            0.dp
                        }
                    }

                    val floatingBarsBottomPadding = FloatingToolbarBottomPadding
                    val navVisibleHeight = FloatingToolbarHeight
                    val floatingNavBarScrollConnection = rememberFloatingTabBarScrollConnection()
                    val appBackdrop = rememberLayerBackdrop()

                    val bottomNavigationBarHeight by animateDpAsState(
                        targetValue = if (shouldShowNavigationBar && !useRail) navVisibleHeight else 0.dp,
                        animationSpec = NavigationBarAnimationSpec,
                        label = "",
                    )

                    val playerBottomSheetState =
                        rememberBottomSheetState(
                            dismissedBound = 0.dp,
                            collapsedBound = 0.dp,
                            expandedBound = maxHeight,
                        )

                    val miniPlayerAnchor by remember {
                        derivedStateOf {
                            when {
                                playerBottomSheetState.isExpanded -> EXPANDED_ANCHOR
                                playerBottomSheetState.isDismissed -> DISMISSED_ANCHOR
                                else -> COLLAPSED_ANCHOR
                            }
                        }
                    }

                    var miniPlayerAnchorPersistenceEnabled by remember(playerConnection) {
                        mutableStateOf(false)
                    }

                    LaunchedEffect(miniPlayerAnchor, isYearInMusicScreen, miniPlayerAnchorPersistenceEnabled) {
                        if (!isYearInMusicScreen && miniPlayerAnchorPersistenceEnabled) {
                            setSavedMiniPlayerAnchor(miniPlayerAnchor)
                        }
                        }

                    var yearInMusicSavedPlayerAnchor by rememberSaveable { mutableStateOf(-1) }

                    LaunchedEffect(isYearInMusicScreen) {
                        val controller = WindowCompat.getInsetsController(window, window.decorView)
                        if (isYearInMusicScreen) {
                            controller.systemBarsBehavior =
                                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            controller.hide(WindowInsetsCompat.Type.statusBars())
                        } else {
                            controller.show(WindowInsetsCompat.Type.statusBars())
                        }
                    }

                    LaunchedEffect(isYearInMusicScreen, playerConnection) {
                        val player = playerConnection?.player ?: return@LaunchedEffect

                        if (isYearInMusicScreen) {
                            if (yearInMusicSavedPlayerAnchor == -1) {
                                yearInMusicSavedPlayerAnchor =
                                    when {
                                        playerBottomSheetState.isExpanded -> EXPANDED_ANCHOR
                                        playerBottomSheetState.isCollapsed -> COLLAPSED_ANCHOR
                                        playerBottomSheetState.isDismissed -> DISMISSED_ANCHOR
                                        else -> COLLAPSED_ANCHOR
                                    }
                            }

                            if (!playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.dismiss()
                            }
                        } else if (yearInMusicSavedPlayerAnchor != -1) {
                            val anchorToRestore = yearInMusicSavedPlayerAnchor
                            yearInMusicSavedPlayerAnchor = -1

                            if (player.currentMediaItem == null) {
                                playerBottomSheetState.dismiss()
                            } else {
                                when (anchorToRestore) {
                                    EXPANDED_ANCHOR -> playerBottomSheetState.expandSoft()
                                    COLLAPSED_ANCHOR -> playerBottomSheetState.collapseSoft()
                                    DISMISSED_ANCHOR -> playerBottomSheetState.dismiss()
                                    else -> playerBottomSheetState.collapseSoft()
                                }
                            }
                        }
                    }

                    val playerAwareWindowInsets =
                        remember(
                            useRail,
                            bottomInset,
                            shouldShowNavigationBar,
                            playerBottomSheetState.isDismissed,
                        ) {
                            var bottom = bottomInset
                            if (shouldShowNavigationBar && !useRail) bottom += getBottomNavPadding()
                            if (!playerBottomSheetState.isDismissed) bottom += MiniPlayerHeight
                            windowsInsets
                                .only((if(useRail) {
                                    WindowInsetsSides.Right
                                } else WindowInsetsSides.Horizontal) + WindowInsetsSides.Top)
                                .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                        }

                    appBarScrollBehavior(
                        canScroll = {
                            navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                        }
                    )

                    val searchBarScrollBehavior =
                        appBarScrollBehavior(
                            canScroll = {
                                navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                        (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                            },
                        )
                    val topAppBarScrollBehavior =
                        appBarScrollBehavior(
                            canScroll = {
                                navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                        (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                            },
                        )

                    var previousRoute by rememberSaveable { mutableStateOf<String?>(null) }

                    LaunchedEffect(navBackStackEntry) {
                        val currentRoute = navBackStackEntry?.destination?.route
                        val wasOnNonTopLevelScreen = previousRoute != null &&
                            previousRoute !in topLevelScreens &&
                            previousRoute?.startsWith("search/") != true
                        val isReturningToHomeOrLibrary = currentRoute == Screens.Home.route ||
                            currentRoute == Screens.Library.route

                        if (wasOnNonTopLevelScreen && isReturningToHomeOrLibrary) {
                            searchBarScrollBehavior.state.resetHeightOffset()
                            topAppBarScrollBehavior.state.resetHeightOffset()
                        }

                        previousRoute = currentRoute

                        if (navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
                            val searchQuery =
                                withContext(Dispatchers.IO) {
                                    if (navBackStackEntry
                                            ?.arguments
                                            ?.getString(
                                                "query",
                                            )!!
                                            .contains(
                                                "%",
                                            )
                                    ) {
                                        navBackStackEntry?.arguments?.getString(
                                            "query",
                                        )!!
                                    } else {
                                        URLDecoder.decode(
                                            navBackStackEntry?.arguments?.getString("query")!!,
                                            "UTF-8"
                                        )
                                    }
                                }
                            onQueryChange(
                                TextFieldValue(
                                    searchQuery,
                                    TextRange(searchQuery.length)
                                )
                            )
                        } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } || navBackStackEntry?.destination?.route in topLevelScreens) {
                            onQueryChange(TextFieldValue())
                            if (navBackStackEntry?.destination?.route != Screens.Home.route) {
                                searchBarScrollBehavior.state.resetHeightOffset()
                                topAppBarScrollBehavior.state.resetHeightOffset()
                            }
                        }
                    }
                    LaunchedEffect(active) {
                        if (active) {
                            searchBarScrollBehavior.state.resetHeightOffset()
                            topAppBarScrollBehavior.state.resetHeightOffset()
                            searchBarFocusRequester.requestFocus()
                        }
                    }

                    var restoredMiniPlayerAnchor by remember(playerConnection) { mutableStateOf(false) }

                    LaunchedEffect(playerConnection, savedMiniPlayerAnchor, isYearInMusicScreen) {
                        if (restoredMiniPlayerAnchor) return@LaunchedEffect
                        val player = playerConnection?.player ?: return@LaunchedEffect
                        val connection = playerConnection ?: return@LaunchedEffect
                        connection.queueRestoreCompleted.first { it }
                        if (player.currentMediaItem == null) {
                            if (!playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.dismiss()
                            }
                        } else {
                            if (!isYearInMusicScreen) {
                                when (savedMiniPlayerAnchor) {
                                    EXPANDED_ANCHOR -> playerBottomSheetState.expandSoft()
                                    COLLAPSED_ANCHOR -> playerBottomSheetState.collapseSoft()
                                    DISMISSED_ANCHOR -> playerBottomSheetState.dismiss()
                                    else -> playerBottomSheetState.collapseSoft()
                                }
                            }
                        }
                        restoredMiniPlayerAnchor = true
                        miniPlayerAnchorPersistenceEnabled = true
                    }

                    DisposableEffect(playerConnection, playerBottomSheetState) {
                        val player =
                            playerConnection?.player ?: return@DisposableEffect onDispose { }
                        val listener =
                            object : Player.Listener {
                                override fun onMediaItemTransition(
                                    mediaItem: MediaItem?,
                                    reason: Int,
                                ) {
                                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                                        mediaItem != null &&
                                        playerBottomSheetState.isDismissed &&
                                        !isYearInMusicScreen
                                    ) {
                                        playerBottomSheetState.collapseSoft()
                                    }
                                }
                            }
                        player.addListener(listener)
                        onDispose {
                            player.removeListener(listener)
                        }
                    }

                    var shouldShowTopBar by rememberSaveable { mutableStateOf(false) }

                    LaunchedEffect(navBackStackEntry, active) {
                        shouldShowTopBar =
                            !active &&
                                navBackStackEntry?.destination?.route in topLevelScreens &&
                                navBackStackEntry?.destination?.route !in listOf(
                                    "settings",
                                    Screens.Home.route,
                                    Screens.Search.route,
                                    Screens.MoodAndGenres.route,
                                    Screens.Library.route,
                                )
                    }

                    val coroutineScope = rememberCoroutineScope()
                    var sharedSong: SongItem? by remember {
                        mutableStateOf(null)
                    }

                    LaunchedEffect(Unit) {
                        if (pendingIntent != null) {
                            handleDeepLinkIntent(pendingIntent!!, navController)
                            pendingIntent = null
                        } else {
                            handleDeepLinkIntent(intent, navController)
                        }
                    }

                    CompositionLocalProvider(
                        LocalDatabase provides database,
                        LocalNavSearchState provides navSearchState,
                        LocalAppBackdrop provides appBackdrop,
                        LocalContentColor provides if (pureBlack) Color.White else contentColorFor(MaterialTheme.colorScheme.surface),
                        LocalPlayerConnection provides playerConnection,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalDownloadUtil provides downloadUtil,
                        LocalShimmerTheme provides ShimmerTheme,
                        LocalSyncUtils provides syncUtils,
                        com.tamed.music.ui.component.LocalBottomSheetPageState provides bottomSheetPageState,
                        com.tamed.music.ui.component.LocalMenuState provides menuState,
                    ) {
                        GlassContainer(
                            modifier = Modifier.fillMaxSize(),
                            useShader = true,
                            content = {
                                Row {
                            AnimatedVisibility(useRail && shouldShowNavigationBar) {
                                NavigationRail(
                                    containerColor = if(pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = if(pureBlack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    header = { Spacer(Modifier.height(24.dp)) }
                                ) {
                                    navigationItems.fastForEach { screen ->
                                        val isSelected =
                                            navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true

                                        NavigationRailItem(
                                            selected = isSelected,
                                            icon = {
                                                Icon(
                                                    painter = painterResource(
                                                        id = if (isSelected) screen.iconIdActive else screen.iconIdInactive
                                                    ),
                                                    contentDescription = null,
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = stringResource(screen.titleId),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            onClick = {
                                                val wasPlayerActive = playerBottomSheetState.isExpanded
                                                
                                                if(wasPlayerActive) {
                                                    playerBottomSheetState.collapse(spring())
                                                }
                                                
                                                if (isSelected) {
                                                    if(wasPlayerActive) return@NavigationRailItem
                                                    
                                                    navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                                    coroutineScope.launch {
                                                        searchBarScrollBehavior.state.resetHeightOffset()
                                                    }
                                                } else {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.startDestinationId) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                            
                            Scaffold(
                                topBar = {
                                    if (shouldShowTopBar) {
                                        val shouldUseFloatingTopBar = remember(navBackStackEntry) {
                                            navBackStackEntry?.destination?.route == Screens.MoodAndGenres.route
                                        }
                                        val shouldShowBlurBackground = remember(navBackStackEntry) {
                                            shouldUseFloatingTopBar
                                        }

                                        val surfaceColor = MaterialTheme.colorScheme.surface
                                        val currentScrollBehavior = if (shouldUseFloatingTopBar) searchBarScrollBehavior else topAppBarScrollBehavior

                                        Box(
                                            modifier = Modifier.offset {
                                                IntOffset(
                                                    x = 0,
                                                    y = currentScrollBehavior.state.heightOffset.toInt()
                                                )
                                            }
                                        ) {
                                            // Gradient shadow background
                                            if (shouldShowBlurBackground) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(AppBarHeight + with(LocalDensity.current) {
                                                            WindowInsets.systemBars.getTop(LocalDensity.current).toDp()
                                                        })
                                                        .background(
                                                            Brush.verticalGradient(
                                                                colors = listOf(
                                                                    surfaceColor.copy(alpha = 0.95f),
                                                                    surfaceColor.copy(alpha = 0.85f),
                                                                    surfaceColor.copy(alpha = 0.6f),
                                                                    Color.Transparent
                                                                )
                                                            )
                                                        )
                                                )
                                            }

                                            TopAppBar(
                                                windowInsets = WindowInsets.safeDrawing.only((if(useRail) {
                                                    WindowInsetsSides.Right
                                                } else WindowInsetsSides.Horizontal) + WindowInsetsSides.Top),
                                                title = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        // app icon
                                                        Image(
                                                            painter = painterResource(R.drawable.about_appbar),
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .size(35.dp)
                                                                .padding(end = 3.dp)
                                                        )

                                                        Text(
                                                            text = stringResource(R.string.app_name),
                                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                },
                                                actions = {
                                                    IconButton(onClick = { navController.navigate("history") }) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.history),
                                                            contentDescription = stringResource(R.string.history)
                                                        )
                                                    }
                                                    IconButton(onClick = { navController.navigate("stats") }) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.stats),
                                                            contentDescription = stringResource(R.string.stats)
                                                        )
                                                    }
                                                    IconButton(onClick = { navController.navigate("settings") }) {
                                                        BadgedBox(badge = {
                                                            if (!Updater.isSameVersion(latestVersionName, BuildConfig.VERSION_NAME)) {
                                                                Badge()
                                                            }
                                                        }) {
                                                            Icon(
                                                                painter = painterResource(R.drawable.settings),
                                                                contentDescription = stringResource(R.string.settings),
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }
                                                    }
                                                },
                                                scrollBehavior = if (shouldUseFloatingTopBar) searchBarScrollBehavior else topAppBarScrollBehavior,
                                                colors = TopAppBarDefaults.topAppBarColors(
                                                    containerColor = if (shouldUseFloatingTopBar) Color.Transparent else if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface,
                                                    scrolledContainerColor = if (shouldUseFloatingTopBar) Color.Transparent else if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface,
                                                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                                                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                    val glassScope = LocalGlassScope.current
                                    val searchBarId = remember { kotlin.random.Random.nextLong() }
                                    val searchBarGlassModifier = if (glassScope != null) {
                                        with(glassScope) {
                                            Modifier.glassBackground(
                                                id = searchBarId,
                                                scale = 0.02f,
                                                blur = 0.6f,
                                                centerDistortion = 0.01f,
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                                                elevation = 4.dp,
                                                tint = appleGlassColor(),
                                                darkness = 0.05f
                                            )
                                        }
                                    } else {
                                        Modifier
                                    }

                                    AnimatedVisibility(
                                        visible = false,
                                        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                                        exit = fadeOut(animationSpec = tween(durationMillis = 200))
                                    ) {
                                        TopSearch(
                                            query = query,
                                            onQueryChange = onQueryChange,
                                            onSearch = onSearch,
                                            active = active,
                                            onActiveChange = onActiveChange,
                                            placeholder = {
                                                Text(
                                                    text = stringResource(
                                                        when (searchSource) {
                                                            SearchSource.LOCAL -> R.string.search_library
                                                            SearchSource.ONLINE -> R.string.search_yt_music
                                                        }
                                                    ),
                                                )
                                            },
                                            leadingIcon = {
                                                IconButton(
                                                    onClick = {
                                                        when {
                                                            active -> onActiveChange(false)
                                                            !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                                navController.navigateUp()
                                                            }

                                                            else -> onActiveChange(true)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        when {
                                                            active -> {}
                                                            !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                                navController.backToMain()
                                                            }
                                                            else -> {}
                                                        }
                                                    },
                                                ) {
                                                    Icon(
                                                        painterResource(
                                                            if (active ||
                                                                !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }
                                                            ) {
                                                                R.drawable.arrow_back
                                                            } else {
                                                                R.drawable.search
                                                            },
                                                        ),
                                                        contentDescription = null,
                                                    )
                                                }
                                            },
                                            trailingIcon = {
                                                Row {
                                                    if (active) {
                                                        if (query.text.isNotEmpty()) {
                                                            IconButton(
                                                                onClick = {
                                                                    onQueryChange(
                                                                        TextFieldValue(
                                                                            ""
                                                                        )
                                                                    )
                                                                },
                                                            ) {
                                                                Icon(
                                                                    painter = painterResource(R.drawable.close),
                                                                    contentDescription = null,
                                                                )
                                                            }
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                searchSource =
                                                                    if (searchSource == SearchSource.ONLINE) SearchSource.LOCAL else SearchSource.ONLINE
                                                            },
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(
                                                                    when (searchSource) {
                                                                        SearchSource.LOCAL -> R.drawable.library_music
                                                                        SearchSource.ONLINE -> R.drawable.language
                                                                    },
                                                                ),
                                                                contentDescription = null,
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            modifier =
                                                Modifier
                                                    .focusRequester(searchBarFocusRequester)
                                                    .let { with(this@BoxWithConstraints) { it.align(Alignment.TopCenter) } }
                                                    .then(searchBarGlassModifier),
                                            focusRequester = searchBarFocusRequester,
                                            colors = if (pureBlack && active) {
                                                SearchBarDefaults.colors(
                                                    containerColor = Color.Black,
                                                    dividerColor = Color.Transparent,
                                                    inputFieldColors = TextFieldDefaults.colors(
                                                        focusedTextColor = Color.White,
                                                        unfocusedTextColor = Color.Gray,
                                                        focusedContainerColor = Color.Transparent,
                                                        unfocusedContainerColor = Color.Transparent,
                                                        cursorColor = Color.White,
                                                        focusedIndicatorColor = Color.Transparent,
                                                        unfocusedIndicatorColor = Color.Transparent,
                                                    )
                                                )
                                            } else {
                                                SearchBarDefaults.colors(
                                                    containerColor = Color.Transparent,
                                                    dividerColor = Color.Transparent
                                                )
                                            },
                                        ) {
                                            Crossfade(
                                                targetState = searchSource,
                                                label = "",
                                                modifier =
                                                    Modifier
                                                        .fillMaxSize()
                                                        .padding(bottom = if(!playerBottomSheetState.isDismissed) MiniPlayerHeight else 0.dp)
                                                        .navigationBarsPadding(),
                                            ) { searchSource ->
                                                when (searchSource) {
                                                    SearchSource.LOCAL ->
                                                        LocalSearchScreen(
                                                            query = query.text,
                                                            navController = navController,
                                                            onDismiss = { onActiveChange(false) },
                                                            pureBlack = pureBlack,
                                                        )

                                                    SearchSource.ONLINE ->
                                                        OnlineSearchScreen(
                                                            query = query.text,
                                                            onQueryChange = onQueryChange,
                                                            navController = navController,
                                                            onSearch = {
                                                                navController.navigate(
                                                                    "search/${
                                                                        URLEncoder.encode(
                                                                            it,
                                                                            "UTF-8"
                                                                        )
                                                                    }"
                                                                )
                                                                if (!pauseSearchHistory) {
                                                                    database.query {
                                                                        insert(SearchHistory(query = it))
                                                                    }
                                                                }
                                                            },
                                                            onDismiss = { onActiveChange(false) },
                                                            pureBlack = pureBlack
                                                        )
                                                }
                                            }
                                        }
                                    }
                                },
                                bottomBar = {},
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(searchBarScrollBehavior.nestedScrollConnection)
                            ) {
                                var transitionDirection =
                                    AnimatedContentTransitionScope.SlideDirection.Left

                                if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                    if (navigationItems.fastAny { it.route == previousTab }) {
                                        val curIndex = navigationItems.indexOf(
                                            navigationItems.fastFirstOrNull {
                                                it.route == navBackStackEntry?.destination?.route
                                            }
                                        )

                                        val prevIndex = navigationItems.indexOf(
                                            navigationItems.fastFirstOrNull {
                                                it.route == previousTab
                                            }
                                        )

                                        if (prevIndex > curIndex)
                                            AnimatedContentTransitionScope.SlideDirection.Right.also {
                                                transitionDirection = it
                                            }
                                    }
                                }

                                NavHost(
                                    navController = navController,
                                    startDestination = when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                        NavigationTab.HOME -> Screens.Home
                                        NavigationTab.LIBRARY -> Screens.Library
                                        else -> Screens.Home
                                    }.route,
                                    enterTransition = {
                                         fadeIn(animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing))
                                     },
                                     exitTransition = {
                                         fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing))
                                     },
                                     popEnterTransition = {
                                         fadeIn(animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing))
                                     },
                                     popExitTransition = {
                                         fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing))
                                     },
                                    modifier = Modifier
                                        .layerBackdrop(appBackdrop)
                                        .nestedScroll(
                                            if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                                navBackStackEntry?.destination?.route?.startsWith("search/") == true
                                            ) {
                                                searchBarScrollBehavior.nestedScrollConnection
                                            } else {
                                                topAppBarScrollBehavior.nestedScrollConnection
                                            }
                                        )
                                        .nestedScroll(floatingNavBarScrollConnection)
                                ) {
                                    navigationBuilder(
                                        navController,
                                        topAppBarScrollBehavior,
                                        latestVersionName
                                    )
                                }
                            }
                        }
                            },
                            glassContent = {
                                CompositionLocalProvider(LocalGlassScope provides this) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        // Bottom bar and mini player
                                        Box {
                                        BottomSheetPlayer(
                                            state = playerBottomSheetState,
                                            navController = navController,
                                            pureBlack = pureBlack
                                        )

                                         if (useRail) return@Box

                                         val isHomeOrLibrary = currentRoute == Screens.Home.route || currentRoute == Screens.Library.route
                                         if (isHomeOrLibrary && navBackStackEntry != null) {
                                             Box(
                                                 modifier = Modifier
                                                     .align(Alignment.BottomCenter)
                                                     .fillMaxWidth()
                                                     .height(180.dp)
                                                     .graphicsLayer {
                                                         val progress = playerBottomSheetState.progress.coerceIn(0f, 1f)
                                                         alpha = 1f - progress
                                                     }
                                                     .background(
                                                         Brush.verticalGradient(
                                                             colors = listOf(
                                                                 Color.Transparent,
                                                                 MaterialTheme.colorScheme.background.copy(alpha = 0.15f),
                                                                 MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                                                                 MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
                                                                 MaterialTheme.colorScheme.background.copy(alpha = 0.88f),
                                                             )
                                                         )
                                                     )
                                             )
                                         }

                                         val currentNavSearchState = LocalNavSearchState.current
                                         val inSettingsScreen = currentRoute == "settings" || currentRoute?.startsWith("settings") == true
                                         val showMiniPlayerInSettings = inSettingsScreen && mediaMetadata != null
                                         val hasDockedPlayerAccessory = mediaMetadata != null && !useRail &&
                                             (shouldShowNavigationBar || inSettingsScreen) &&
                                             (!inSearchScreen || !currentNavSearchState.keyboardActive)

                                         if (navBackStackEntry != null && (shouldShowNavigationBar || showMiniPlayerInSettings)) {
                                             AppFloatingNavBar(
                                                 navigationItems = if (inSettingsScreen) emptyList() else navigationItems,
                                                 currentRoute = currentRoute,
                                                 onItemClick = { screen, isSelected ->
                                                     if (playerBottomSheetState.isExpanded) {
                                                         playerBottomSheetState.collapseSoft()
                                                     }
                                                     if (currentNavSearchState.visualActive) {
                                                         currentNavSearchState.onExit()
                                                     }
                                                     if (isSelected) {
                                                         navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                                         coroutineScope.launch {
                                                             searchBarScrollBehavior.state.resetHeightOffset()
                                                         }
                                                     } else {
                                                         navController.navigate(screen.route) {
                                                             popUpTo(navController.graph.startDestinationId) {
                                                                 saveState = true
                                                             }
                                                             launchSingleTop = true
                                                             restoreState = true
                                                         }
                                                     }
                                                 },
                                                 scrollConnection = floatingNavBarScrollConnection,
                                                 pureBlack = pureBlack,
                                                 showPlayerAccessory = hasDockedPlayerAccessory,
                                                 onAccessoryClick = { playerBottomSheetState.expandSoft() },
                                                 onAccessoryLyricsClick = null,
                                                 onAccessoryQueueClick = null,
                                                 modifier = Modifier
                                                     .align(Alignment.BottomCenter)
                                                     .widthIn(max = 500.dp)
                                                     .padding(horizontal = 16.dp)
                                                     .padding(bottom = bottomInset + 8.dp)
                                                     .graphicsLayer {
                                                         val bottomMarginPx = with(density) { (bottomInset + 8.dp).toPx() }
                                                         val navBarHeightPx = with(density) { bottomNavigationBarHeight.toPx() }
                                                         val navVisibleHeightPx = with(density) { navVisibleHeight.toPx() }
                                                         val hiddenOffset = size.height + bottomMarginPx
                                                         translationY = if (navBarHeightPx == 0f && !showMiniPlayerInSettings) {
                                                             hiddenOffset
                                                         } else {
                                                             val progress = playerBottomSheetState.progress.coerceIn(0f, 1f)
                                                             val slideOffset = hiddenOffset * progress
                                                             val effectiveRatio = if (showMiniPlayerInSettings) 1f else (navBarHeightPx / navVisibleHeightPx)
                                                             val hideOffset = hiddenOffset * (1 - effectiveRatio)
                                                             slideOffset + hideOffset
                                                         }
                                                     }
                                             )
                                         }
                                    }
                                        
                                        // Overlays, bottom sheets, menus, dialogs
                                        BottomSheetMenu(
                            state = LocalMenuState.current,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        BottomSheetPage(
                            state = LocalBottomSheetPageState.current,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        sharedSong?.let { song ->
                            playerConnection?.let {
                                Dialog(
                                    onDismissRequest = { sharedSong = null },
                                    properties = DialogProperties(usePlatformDefaultWidth = false),
                                ) {
                                    Surface(
                                        modifier = Modifier.padding(24.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = AlertDialogDefaults.containerColor,
                                        tonalElevation = AlertDialogDefaults.TonalElevation,
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            YouTubeSongMenu(
                                                song = song,
                                                navController = navController,
                                                onDismiss = { sharedSong = null },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        NetworkStatusBanner(
                            state = networkBannerState,
                            modifier =
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(
                                        top = if (shouldShowTopBar) topInset + AppBarHeight + 8.dp else topInset + 8.dp,
                                        start = 16.dp,
                                        end = 16.dp,
                                    )
                                    .zIndex(10f),
                        )
                                    }
                                }
                            }
                        )
                    }

                    LaunchedEffect(shouldShowSearchBar, openSearchImmediately) {
                        if (shouldShowSearchBar && openSearchImmediately) {
                            onActiveChange(true)
                            try {
                                delay(100)
                                searchBarFocusRequester.requestFocus()
                            } catch (_: Exception) {
                            }
                            openSearchImmediately = false
                        }
                    }
                }
            }
        }
    }

    private fun handleDeepLinkIntent(intent: Intent, navController: NavHostController) {
        val uri = intent.data ?: intent.sharedStreamUri() ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return
        val coroutineScope = lifecycleScope

        val authority = uri.authority?.lowercase()
        if (uri.scheme.equals("tamed", ignoreCase = true) && authority == "together") {
            pendingTogetherJoinLink = uri.toString()
            startMusicServiceSafely()
            joinPendingTogetherIfReady()
            return
        }

        if (uri.scheme.equals("tamed", ignoreCase = true) && authority == "login") {
            navController.navigate(buildLoginRoute(uri.getQueryParameter(LOGIN_URL_ARGUMENT)))
            return
        }

        if (isPotentialExternalAudioIntent(intent, uri)) {
            coroutineScope.launch {
                val mediaItem =
                    withContext(Dispatchers.IO) {
                        resolveExternalAudioMediaItem(intent, uri)
                    }

                if (mediaItem != null) {
                    pendingDeepLinkSong =
                        PendingDeepLinkSong(
                            mediaItem = mediaItem,
                        )
                    startMusicServiceSafely()
                    playPendingDeepLinkSongIfReady()
                }
            }
            return
        }

        when (val path = uri.pathSegments.firstOrNull()) {
            "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
                if (playlistId.startsWith("OLAK5uy_")) {
                    coroutineScope.launch {
                        YouTube.albumSongs(playlistId).onSuccess { songs ->
                            songs.firstOrNull()?.album?.id?.let { browseId ->
                                navController.navigate("album/$browseId")
                            }
                        }.onFailure { reportException(it) }
                    }
                } else {
                    navController.navigate("online_playlist/$playlistId")
                }
            }

            "browse" -> uri.lastPathSegment?.let { browseId ->
                navController.navigate("album/$browseId")
            }

            "channel", "c" -> uri.lastPathSegment?.let { artistId ->
                navController.navigate("artist/$artistId")
            }

            else -> {
                val videoId = when {
                    path == "watch" -> uri.getQueryParameter("v")
                    uri.host == "youtu.be" -> uri.pathSegments.firstOrNull()
                    else -> null
                }
                
                val playlistId = uri.getQueryParameter("list")

                videoId?.let { vid ->
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.IO) {
                            YouTube.queue(listOf(vid), playlistId)
                        }

                        result.onSuccess { queued ->
                            val mediaItem =
                                queued.firstOrNull { it.id == vid }?.toMediaItem()
                                    ?: queued.firstOrNull()?.toMediaItem()
                                    ?: MediaItem
                                        .Builder()
                                        .setMediaId(vid)
                                        .setUri(vid)
                                        .setCustomCacheKey(vid)
                                        .build()
                            pendingDeepLinkSong =
                                PendingDeepLinkSong(
                                    mediaItem = mediaItem,
                                )
                            startMusicServiceSafely()
                            playPendingDeepLinkSongIfReady()
                        }.onFailure {
                            reportException(it)
                        }
                    }
                }
            }
        }
    }

    private fun Intent.sharedStreamUri(): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(Intent.EXTRA_STREAM)
        }

    private fun isPotentialExternalAudioIntent(intent: Intent, uri: Uri): Boolean {
        if (uri.host?.lowercase() in YouTubeHosts) return false

        val scheme = uri.scheme?.lowercase()
        if (scheme == "content" || scheme == "file") return true
        if (intent.type?.startsWith("audio/") == true) return true
        if (inferAudioMimeTypeFromUri(uri) != null) return true

        return scheme in setOf("http", "https") &&
            KnownDirectAudioHosts.any { knownHost ->
                val host = uri.host?.lowercase().orEmpty()
                host == knownHost || host.endsWith(".$knownHost")
            }
    }

    private fun resolveExternalAudioMediaItem(intent: Intent, uri: Uri): MediaItem? {
        val sourceType =
            when (uri.scheme?.lowercase()) {
                "content", "file" -> MediaMetadata.SourceType.LOCAL
                "http", "https" -> MediaMetadata.SourceType.DIRECT
                else -> return null
            }

        val sourceMimeType = resolveSourceMimeType(intent, uri, sourceType) ?: return null
        val sourceLabel = resolveSourceLabel(uri, sourceType)

        if (sourceType == MediaMetadata.SourceType.LOCAL) {
            rememberUriPermission(uri, intent.flags)
        }

        var title = resolveDisplayTitle(uri)
        var artist = sourceLabel
        var albumTitle: String? = null
        var duration = -1

        val retriever = MediaMetadataRetriever()
        runCatching {
            retriever.setDataSource(this, uri)
            val extractedTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            if (!extractedTitle.isNullOrBlank()) {
                title = extractedTitle
            }
            val extractedArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            if (!extractedArtist.isNullOrBlank()) {
                artist = extractedArtist
            }
            val extractedAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            if (!extractedAlbum.isNullOrBlank()) {
                albumTitle = extractedAlbum
            }
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            if (durationMs != null && durationMs > 0) {
                duration = (durationMs / 1000L).toInt()
            }
        }.also {
            runCatching { retriever.release() }
        }

        val mediaMetadata =
            MediaMetadata(
                id = uri.toString(),
                title = title,
                artists = listOf(MediaMetadata.Artist(id = null, name = artist)),
                album = albumTitle?.let { MediaMetadata.Album(id = uri.toString(), title = it) },
                duration = duration,
                sourceType = sourceType,
                sourceUri = uri.toString(),
                sourceMimeType = sourceMimeType,
                sourceLabel = sourceLabel,
            )

        return mediaMetadata.toMediaItem()
    }

    private fun resolveSourceMimeType(
        intent: Intent,
        uri: Uri,
        sourceType: MediaMetadata.SourceType,
    ): String? {
        val explicitMimeType = intent.type?.substringBefore(";")?.takeIf(::isAudioMimeType)
        val probedMimeType =
            when (sourceType) {
                MediaMetadata.SourceType.LOCAL -> contentResolver.getType(uri)
                MediaMetadata.SourceType.DIRECT -> fetchRemoteContentType(uri)
                MediaMetadata.SourceType.YOUTUBE -> null
            }?.substringBefore(";")?.takeIf(::isAudioMimeType)

        val inferredFromUri = inferAudioMimeTypeFromUri(uri)

        if (probedMimeType != null && !probedMimeType.startsWith("audio/")) {
            return inferredFromUri ?: probedMimeType
        }

        val validExplicitMimeType = if (explicitMimeType?.startsWith("audio/") == true) explicitMimeType else null
        return probedMimeType ?: validExplicitMimeType ?: inferredFromUri
    }

    private fun isAudioMimeType(mimeType: String?): Boolean {
        if (mimeType == null) return false
        val normalized = mimeType.substringBefore(';').trim().lowercase()
        return normalized.startsWith("audio/") ||
            normalized in setOf(
                "application/ogg",
                "application/x-flac",
                "application/x-ogg",
                "application/octet-stream",
                "video/mp4",
                "video/3gpp",
                "video/quicktime",
                "video/x-matroska",
            )
    }

    private fun inferAudioMimeTypeFromUri(uri: Uri): String? {
        val normalizedPath = uri.lastPathSegment?.substringBefore('?')?.lowercase().orEmpty()
        val extension = normalizedPath.substringAfterLast('.', "")

        return when (extension) {
            "flac" -> "audio/flac"
            "wav", "wave" -> "audio/wav"
            "alac", "m4a", "m4b", "m4p", "mp4", "mp4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "mp3", "mp2", "mp1", "mpga" -> "audio/mpeg"
            "ogg", "oga" -> "audio/ogg"
            "opus" -> "audio/opus"
            "mka", "mkv" -> "audio/x-matroska"
            "aif", "aiff", "aifc" -> "audio/aiff"
            "wma" -> "audio/x-ms-wma"
            "ape" -> "audio/x-ape"
            "wv" -> "audio/x-wavpack"
            "dsf", "dff" -> "audio/x-dsd"
            "amr", "3gp", "3gpp" -> "audio/amr"
            else -> null
        }
    }

    private fun fetchRemoteContentType(uri: Uri): String? {
        fun openConnection(requestMethod: String): HttpURLConnection? {
            val url = runCatching { java.net.URL(uri.toString()) }.getOrNull() ?: return null
            return (url.openConnection() as? HttpURLConnection)?.apply {
                instanceFollowRedirects = true
                this.requestMethod = requestMethod
                connectTimeout = 5_000
                readTimeout = 5_000
                if (requestMethod == "GET") {
                    setRequestProperty("Range", "bytes=0-0")
                }
            }
        }

        listOf("HEAD", "GET").forEach { method ->
            val connection = openConnection(method) ?: return@forEach
            val contentType =
                runCatching {
                    connection.connect()
                    connection.contentType
                }.getOrNull()
            connection.disconnect()
            if (isAudioMimeType(contentType)) return contentType
        }

        return null
    }

    private fun resolveSourceLabel(
        uri: Uri,
        sourceType: MediaMetadata.SourceType,
    ): String =
        when (sourceType) {
            MediaMetadata.SourceType.LOCAL -> "Local file"
            MediaMetadata.SourceType.DIRECT -> {
                val host = uri.host?.lowercase().orEmpty().removePrefix("www.")
                when {
                    "monochrome.tf" in host -> "Monochrome"
                    "squid.wtf" in host -> "Squid"
                    host.isNotBlank() -> host
                    else -> "Direct stream"
                }
            }
            MediaMetadata.SourceType.YOUTUBE -> getString(R.string.app_name)
        }

    private fun resolveDisplayTitle(uri: Uri): String {
        val contentName =
            if (uri.scheme.equals("content", ignoreCase = true)) {
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
                }
            } else {
                null
            }

        val rawName = contentName ?: uri.lastPathSegment ?: uri.host ?: uri.toString()
        return URLDecoder.decode(rawName.substringBeforeLast('.'), "UTF-8").ifBlank { "Unknown title" }
    }

    private fun resolveDurationSeconds(uri: Uri): Int {
        val retriever = MediaMetadataRetriever()
        return runCatching {
            retriever.setDataSource(this, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            durationMs?.div(1000L)?.toInt() ?: -1
        }.getOrDefault(-1).also {
            runCatching { retriever.release() }
        }
    }

    private fun rememberUriPermission(uri: Uri, intentFlags: Int) {
        if (!uri.scheme.equals("content", ignoreCase = true)) return

        val persistedFlags = intentFlags and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        if (persistedFlags == 0) return

        runCatching {
            contentResolver.takePersistableUriPermission(uri, persistedFlags)
        }
    }

    private fun startMusicServiceSafely() {
        runCatching { startService(Intent(this, com.tamed.music.playback.MusicService::class.java)) }
            .onFailure { reportException(it) }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            window.navigationBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }

    companion object {
        const val ACTION_SEARCH = "com.tamed.music.action.SEARCH"
        const val ACTION_LIBRARY = "com.tamed.music.action.LIBRARY"
        private val KnownDirectAudioHosts = setOf("monochrome.tf", "squid.wtf")
        private val YouTubeHosts = setOf("youtube.com", "m.youtube.com", "music.youtube.com", "www.youtube.com", "youtu.be")
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerConnection =
    staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets =
    compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }
val LocalIsPlayerExpanded = compositionLocalOf { false }

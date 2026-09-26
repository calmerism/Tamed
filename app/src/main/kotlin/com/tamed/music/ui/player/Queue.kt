/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.ui.player

import androidx.activity.compose.BackHandler
import android.annotation.SuppressLint
import android.content.Context
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Checkbox
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ripple
import com.tamed.music.ui.component.ExplicitTag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.navigation.NavController
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.R
import com.tamed.music.constants.ListItemHeight
import com.tamed.music.constants.PlayerDesignStyle
import com.tamed.music.constants.PlayerDesignStyleKey
import com.tamed.music.constants.asSupportedPlayerDesignStyle
import com.tamed.music.constants.PlayerButtonsStyle
import com.tamed.music.constants.PlayerButtonsStyleKey
import com.tamed.music.constants.QueueEditLockKey
import com.tamed.music.constants.AutoLoadMoreKey
import com.tamed.music.extensions.metadata
import com.tamed.music.extensions.move
import com.tamed.music.extensions.togglePlayPause
import com.tamed.music.extensions.toggleRepeatMode
import com.tamed.music.models.MediaMetadata
import com.tamed.music.ui.component.ActionPromptDialog
import com.tamed.music.ui.component.BottomSheet
import com.tamed.music.ui.component.BottomSheetState
import com.tamed.music.ui.component.LocalBottomSheetPageState
import com.tamed.music.ui.component.LocalMenuState
import com.tamed.music.ui.component.MediaMetadataListItem
import com.tamed.music.ui.menu.PlayerMenu
import com.tamed.music.ui.menu.SelectionMediaMetadataMenu
import com.tamed.music.ui.utils.ShowMediaInfo
import com.tamed.music.utils.makeTimeString
import com.tamed.music.utils.rememberEnumPreference
import com.tamed.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.roundToInt
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Queue(
    state: BottomSheetState,
    playerBottomSheetState: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    onBackgroundColor: Color,
    TextBackgroundColor: Color,
    textButtonColor: Color,
    iconButtonColor: Color,
    onShowLyrics: () -> Unit = {},
    pureBlack: Boolean,
    backdrop: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboard.current
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()

    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)

    val selectedSongs = remember { mutableStateListOf<MediaMetadata>() }
    val selectedItems = remember { mutableStateListOf<Timeline.Window>() }
    var selection by remember { mutableStateOf(false) }

    if (selection) {
        BackHandler {
            selection = false
        }
    }

    var locked by rememberPreference(QueueEditLockKey, defaultValue = false)
    var infiniteQueueEnabled by rememberPreference(AutoLoadMoreKey, defaultValue = true)
    val togetherSessionState by playerConnection.service.togetherSessionState.collectAsState()
    val togetherForcesLock =
        togetherSessionState is com.tamed.music.together.TogetherSessionState.Joined &&
            (togetherSessionState as com.tamed.music.together.TogetherSessionState.Joined).role is com.tamed.music.together.TogetherRole.Guest
    val effectiveLocked = locked || togetherForcesLock

    val rawPlayerDesignStyle by rememberEnumPreference(
        key = PlayerDesignStyleKey,
        defaultValue = PlayerDesignStyle.V1
    )
    val playerDesignStyle = rawPlayerDesignStyle.asSupportedPlayerDesignStyle()

    val snackbarHostState = remember { SnackbarHostState() }
    var dismissJob: Job? by remember { mutableStateOf(null) }

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sleepTimerValue by remember { mutableStateOf(30f) }
    val sleepTimerEnabled = remember(
        playerConnection.service.sleepTimer.triggerTime,
        playerConnection.service.sleepTimer.pauseWhenSongEnd
    ) {
        playerConnection.service.sleepTimer.isActive
    }
    var sleepTimerTimeLeft by remember { mutableStateOf(0L) }
    
    val (showCodecOnPlayer) = rememberPreference(
        key = booleanPreferencesKey("show_codec_on_player"),
        defaultValue = false
    )

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft = if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                    playerConnection.player.duration - playerConnection.player.currentPosition
                } else {
                    playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                }
                delay(1000L)
            }
        }
    }

    BottomSheet(
        state = state,
        backgroundColor = Color.Unspecified,
        modifier = modifier,
        collapsedContent = {
            when (playerDesignStyle) {
                PlayerDesignStyle.V2 -> {
                    QueueCollapsedContentV2(
                        showCodecOnPlayer = showCodecOnPlayer,
                        currentFormat = currentFormat,
                        textBackgroundColor = TextBackgroundColor,
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        sleepTimerEnabled = sleepTimerEnabled,
                        sleepTimerTimeLeft = sleepTimerTimeLeft,
                        repeatMode = repeatMode,
                        mediaMetadata = mediaMetadata,
                        onExpandQueue = { state.expandSoft() },
                        onSleepTimerClick = {
                            if (sleepTimerEnabled) {
                                playerConnection.service.sleepTimer.clear()
                            } else {
                                showSleepTimerDialog = true
                            }
                        },
                        onShowLyrics = onShowLyrics,
                        onRepeatModeClick = { playerConnection.player.toggleRepeatMode() },
                        onMenuClick = {
                            menuState.show {
                                PlayerMenu(
                                    mediaMetadata = mediaMetadata,
                                    navController = navController,
                                    playerBottomSheetState = playerBottomSheetState,
                                    onShowDetailsDialog = {
                                        mediaMetadata?.id?.let {
                                            bottomSheetPageState.show {
                                                ShowMediaInfo(it)
                                            }
                                        }
                                    },
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
                }

                PlayerDesignStyle.V3 -> {
                    QueueCollapsedContentV3(
                        showCodecOnPlayer = showCodecOnPlayer,
                        currentFormat = currentFormat,
                        textBackgroundColor = TextBackgroundColor,
                        sleepTimerEnabled = sleepTimerEnabled,
                        sleepTimerTimeLeft = sleepTimerTimeLeft,
                        onExpandQueue = { state.expandSoft() },
                        onSleepTimerClick = {
                            if (sleepTimerEnabled) {
                                playerConnection.service.sleepTimer.clear()
                            } else {
                                showSleepTimerDialog = true
                            }
                        },
                        onShowLyrics = onShowLyrics,
                        onMenuClick = {
                            menuState.show {
                                PlayerMenu(
                                    mediaMetadata = mediaMetadata,
                                    navController = navController,
                                    playerBottomSheetState = playerBottomSheetState,
                                    onShowDetailsDialog = {
                                        mediaMetadata?.id?.let {
                                            bottomSheetPageState.show {
                                                ShowMediaInfo(it)
                                            }
                                        }
                                    },
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
                }

                PlayerDesignStyle.V5 -> {
                    QueueCollapsedContentV3(
                        showCodecOnPlayer = showCodecOnPlayer,
                        currentFormat = currentFormat,
                        textBackgroundColor = TextBackgroundColor,
                        sleepTimerEnabled = sleepTimerEnabled,
                        sleepTimerTimeLeft = sleepTimerTimeLeft,
                        onExpandQueue = { state.expandSoft() },
                        onSleepTimerClick = {
                            if (sleepTimerEnabled) {
                                playerConnection.service.sleepTimer.clear()
                            } else {
                                showSleepTimerDialog = true
                            }
                        },
                        onShowLyrics = onShowLyrics,
                        onMenuClick = {
                            menuState.show {
                                PlayerMenu(
                                    mediaMetadata = mediaMetadata,
                                    navController = navController,
                                    playerBottomSheetState = playerBottomSheetState,
                                    onShowDetailsDialog = {
                                        mediaMetadata?.id?.let {
                                            bottomSheetPageState.show {
                                                ShowMediaInfo(it)
                                            }
                                        }
                                    },
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
                }
                
                PlayerDesignStyle.V4 -> {
                    QueueCollapsedContentV4(
                        showCodecOnPlayer = showCodecOnPlayer,
                        currentFormat = currentFormat,
                        textBackgroundColor = TextBackgroundColor,
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        sleepTimerEnabled = sleepTimerEnabled,
                        sleepTimerTimeLeft = sleepTimerTimeLeft,
                        mediaMetadata = mediaMetadata,
                        onExpandQueue = { state.expandSoft() },
                        onSleepTimerClick = {
                            if (sleepTimerEnabled) {
                                playerConnection.service.sleepTimer.clear()
                            } else {
                                showSleepTimerDialog = true
                            }
                        },
                        onShowLyrics = onShowLyrics
                    )
                }
                
                PlayerDesignStyle.V1 -> {
                    QueueCollapsedContentV1(
                        showCodecOnPlayer = showCodecOnPlayer,
                        currentFormat = currentFormat,
                        textBackgroundColor = TextBackgroundColor,
                        sleepTimerEnabled = sleepTimerEnabled,
                        sleepTimerTimeLeft = sleepTimerTimeLeft,
                        onExpandQueue = { state.expandSoft() },
                        onSleepTimerClick = {
                            if (sleepTimerEnabled) {
                                playerConnection.service.sleepTimer.clear()
                            } else {
                                showSleepTimerDialog = true
                            }
                        },
                        onShowLyrics = onShowLyrics
                    )
                }

                PlayerDesignStyle.V6 -> {
                    QueueCollapsedContentV4(
                        showCodecOnPlayer = showCodecOnPlayer,
                        currentFormat = currentFormat,
                        textBackgroundColor = TextBackgroundColor,
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        sleepTimerEnabled = sleepTimerEnabled,
                        sleepTimerTimeLeft = sleepTimerTimeLeft,
                        mediaMetadata = mediaMetadata,
                        onExpandQueue = { state.expandSoft() },
                        onSleepTimerClick = {
                            if (sleepTimerEnabled) {
                                playerConnection.service.sleepTimer.clear()
                            } else {
                                showSleepTimerDialog = true
                            }
                        },
                        onShowLyrics = onShowLyrics
                    )
                }

                PlayerDesignStyle.V7 -> {
                    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
                    val activeDevice = remember(audioManager) {
                        audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
                            .firstOrNull { it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO || it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET }
                            ?.productName?.toString() ?: "Speaker"
                    }
                    QueueCollapsedContentV7(
                        showCodecOnPlayer = showCodecOnPlayer,
                        currentFormat = currentFormat,
                        textBackgroundColor = TextBackgroundColor,
                        onExpandQueue = { state.expandSoft() },
                        onShowLyrics = onShowLyrics,
                        onDeviceClick = {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        },
                        deviceName = activeDevice
                    )
                }
            }

            if (showSleepTimerDialog) {
                SleepTimerDialog(
                    onDismiss = { showSleepTimerDialog = false },
                    onConfirm = { minutes ->
                        showSleepTimerDialog = false
                        playerConnection.service.sleepTimer.start(minutes)
                    },
                    onEndOfSong = {
                        showSleepTimerDialog = false
                        playerConnection.service.sleepTimer.start(-1)
                    },
                    initialValue = sleepTimerValue
                )
            }
        },
    ) {
        val queueTitle by playerConnection.queueTitle.collectAsState()
        val queueWindows by playerConnection.queueWindows.collectAsState()
        val canRestoreQueue by playerConnection.canRestorePreviousQueue.collectAsState()
        val mutableQueueWindows = remember { mutableStateListOf<Timeline.Window>() }
        val queueLength by remember {
            derivedStateOf {
                queueWindows.sumOf { it.mediaItem.metadata?.duration ?: 0 }
            }
        }

        val coroutineScope = rememberCoroutineScope()



        val headerItems = if (selection) 1 else 0
        val lazyListState = rememberLazyListState()
        var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }

        val currentPlayingUid = remember(currentWindowIndex, queueWindows) {
            if (currentWindowIndex in queueWindows.indices) {
                queueWindows[currentWindowIndex].uid
            } else null
        }

        val isMultiArtistQueue by remember {
            derivedStateOf {
                val firstArtist = mutableQueueWindows.firstOrNull()?.mediaItem?.metadata?.artists?.joinToString { it.name }
                mutableQueueWindows.any { window ->
                    val a = window.mediaItem.metadata?.artists?.joinToString { it.name }
                    !a.isNullOrBlank() && a != firstArtist
                }
            }
        }

        val reorderableState = rememberReorderableLazyListState(
            lazyListState = lazyListState,
            scrollThresholdPadding = WindowInsets.systemBars.add(
                WindowInsets(
                    top = ListItemHeight,
                    bottom = ListItemHeight
                )
            ).asPaddingValues()
        ) { from, to ->
            if (to.index >= headerItems && from.index >= headerItems) {
                val fromIdx = from.index - headerItems
                val toIdx = to.index - headerItems
                if (fromIdx in mutableQueueWindows.indices && toIdx in mutableQueueWindows.indices) {
                    val currentDragInfo = dragInfo
                    dragInfo = if (currentDragInfo == null) {
                        fromIdx to toIdx
                    } else {
                        currentDragInfo.first to toIdx
                    }

                    mutableQueueWindows.move(fromIdx, toIdx)

                    if (selection && currentWindowIndex in mutableQueueWindows.indices) {
                        val draggedItemUid = mutableQueueWindows[if (toIdx > fromIdx) toIdx else fromIdx].uid
                        val currentItem = queueWindows.getOrNull(currentWindowIndex)

                        if (currentItem?.uid == draggedItemUid) {
                            val newIndex = mutableQueueWindows.indexOfFirst { it.uid == draggedItemUid }
                            if (newIndex != -1) {
                                selectedSongs.clear()
                                selectedItems.clear()
                                mutableQueueWindows.getOrNull(newIndex)?.let { window ->
                                    window.mediaItem.metadata?.let { metadata ->
                                        selectedSongs.add(metadata)
                                        selectedItems.add(window)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        LaunchedEffect(currentPlayingUid) {
            if (!state.isCollapsed && !reorderableState.isAnyItemDragging && currentPlayingUid != null) {
                val indexInMutableList = mutableQueueWindows.indexOfFirst { it.uid == currentPlayingUid }
                if (indexInMutableList != -1) {
                    val targetIndex = (indexInMutableList + headerItems).coerceAtLeast(0)
                    lazyListState.animateScrollToItem(targetIndex)
                }
            }
        }

        LaunchedEffect(reorderableState.isAnyItemDragging) {
            if (!reorderableState.isAnyItemDragging) {
                dragInfo?.let { (from, to) ->
                    val safeFrom = from.coerceIn(0, queueWindows.lastIndex)
                    val safeTo = to.coerceIn(0, queueWindows.lastIndex)

                    if (safeFrom != safeTo) {
                        if (!playerConnection.player.shuffleModeEnabled) {
                            playerConnection.player.moveMediaItem(safeFrom, safeTo)
                        } else {
                            playerConnection.player.setShuffleOrder(
                                DefaultShuffleOrder(
                                    queueWindows.map { it.firstPeriodIndex }
                                        .toMutableList()
                                        .move(safeFrom, safeTo)
                                        .toIntArray(),
                                    System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    dragInfo = null
                }
            }
        }

        LaunchedEffect(queueWindows) {
            if (!reorderableState.isAnyItemDragging) {
                val currentUids = mutableQueueWindows.map { it.uid }
                val newUids = queueWindows.map { it.uid }
                if (currentUids != newUids) {
                    mutableQueueWindows.apply {
                        clear()
                        addAll(queueWindows)
                    }
                }
            }
        }

        LaunchedEffect(state.isCollapsed) {
            if (!state.isCollapsed && currentPlayingUid != null) {
                val indexInMutableList = mutableQueueWindows.indexOfFirst { it.uid == currentPlayingUid }
                if (indexInMutableList != -1) {
                    val targetIndex = (indexInMutableList + headerItems).coerceAtLeast(0)
                    lazyListState.scrollToItem(targetIndex)
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (backdrop != null) {
                backdrop()
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                CurrentSongHeader(
                    sheetState = state,
                    mediaMetadata = mediaMetadata,
                    isPlaying = isPlaying,
                    repeatMode = repeatMode,
                    shuffleModeEnabled = playerConnection.player.shuffleModeEnabled,
                    locked = effectiveLocked,
                    songCount = queueWindows.size,
                    queueDuration = queueLength,
                    infiniteQueueEnabled = infiniteQueueEnabled,
                    backgroundColor = if (backdrop != null) Color.Transparent else backgroundColor,
                    onBackgroundColor = onBackgroundColor,
                    onToggleLike = {
                        playerConnection.service.toggleLike()
                    },
                    onMenuClick = {
                        menuState.show {
                            PlayerMenu(
                                mediaMetadata = mediaMetadata,
                                navController = navController,
                                playerBottomSheetState = playerBottomSheetState,
                                onShowDetailsDialog = {
                                    mediaMetadata?.id?.let {
                                        bottomSheetPageState.show {
                                            ShowMediaInfo(it)
                                        }
                                    }
                                },
                                onDismiss = menuState::dismiss
                            )
                        }
                    },
                    onRepeatClick = { playerConnection.player.toggleRepeatMode() },
                    onShuffleClick = {
                        coroutineScope.launch(Dispatchers.Main) {
                            playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
                        }
                    },
                    onLockClick = {
                        if (togetherForcesLock) {
                            Toast.makeText(context, R.string.not_allowed, Toast.LENGTH_SHORT).show()
                        } else {
                            locked = !locked
                        }
                    },
                    onInfiniteQueueClick = {
                        val nextInfiniteQueueEnabled = !infiniteQueueEnabled
                        infiniteQueueEnabled = nextInfiniteQueueEnabled
                        if (nextInfiniteQueueEnabled) {
                            playerConnection.service.onInfiniteQueueEnabled()
                        } else {
                            playerConnection.service.onInfiniteQueueDisabled()
                        }
                    }
                )

                AnimatedVisibility(
                    visible = canRestoreQueue,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    androidx.compose.material3.Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                        onClick = {
                            val restored = playerConnection.restorePreviousQueue()
                            if (restored) {
                                Toast.makeText(context, R.string.queue_restored, Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.replay),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.restore_previous_queue),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            TextButton(
                                onClick = {
                                    val restored = playerConnection.restorePreviousQueue()
                                    if (restored) {
                                        Toast.makeText(context, R.string.queue_restored, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.undo),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                val shouldLoadImages by remember {
                    derivedStateOf {
                        state.value > state.collapsedBound + 80.dp
                    }
                }

                LazyColumn(
                    state = lazyListState,
                    contentPadding =
                    WindowInsets.systemBars
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                        .add(
                            WindowInsets(
                                bottom = ListItemHeight + 8.dp,
                            ),
                        ).asPaddingValues(),
                    modifier = Modifier
                        .weight(1f)
                        .nestedScroll(state.preUpPostDownNestedScrollConnection)
                ) {
                
                if (selection) {
                    item(key = "queue_selection_spacer") {
                        Spacer(
                            modifier = Modifier
                                .animateContentSize()
                                .height(48.dp),
                        )
                    }
                }

                itemsIndexed(
                    items = mutableQueueWindows,
                    key = { _, item -> item.uid.hashCode() },
                ) { index, window ->
                    ReorderableItem(
                        state = reorderableState,
                        key = window.uid.hashCode(),
                    ) {
                        val currentItem by rememberUpdatedState(window)
                        val isActive = window.uid == currentPlayingUid
                        val dismissBoxState =
                            key(window.uid) {
                                rememberSwipeToDismissBoxState(
                                    positionalThreshold = { totalDistance -> totalDistance },
                                    confirmValueChange = { targetValue ->
                                        (targetValue == SwipeToDismissBoxValue.Settled || !lazyListState.isScrollInProgress) &&
                                            !reorderableState.isAnyItemDragging
                                    }
                                )
                            }

                        var processedDismiss by remember { mutableStateOf(false) }
                        LaunchedEffect(dismissBoxState.currentValue) {
                            val dv = dismissBoxState.currentValue
                            if (!processedDismiss && (
                                    dv == SwipeToDismissBoxValue.StartToEnd ||
                                    dv == SwipeToDismissBoxValue.EndToStart
                                )
                            ) {
                                processedDismiss = true
                                playerConnection.player.removeMediaItem(currentItem.firstPeriodIndex)
                                dismissJob?.cancel()
                                dismissJob = coroutineScope.launch {
                                    val snackbarResult = snackbarHostState.showSnackbar(
                                        message = context.getString(
                                            R.string.removed_song_from_playlist,
                                            currentItem.mediaItem.metadata?.title,
                                        ),
                                        actionLabel = context.getString(R.string.undo),
                                        duration = SnackbarDuration.Short,
                                    )
                                    if (snackbarResult == SnackbarResult.ActionPerformed) {
                                        playerConnection.player.addMediaItem(currentItem.mediaItem)
                                        playerConnection.player.moveMediaItem(
                                            mutableQueueWindows.size,
                                            currentItem.firstPeriodIndex,
                                        )
                                    }
                                }
                            }
                            if (dv == SwipeToDismissBoxValue.Settled) {
                                processedDismiss = false
                            }
                        }

                        val content: @Composable () -> Unit = {
                            val trackMetadata = window.mediaItem.metadata
                            if (trackMetadata != null) {
                                val isDarkTheme = if (backdrop != null || backgroundColor == Color.Transparent) true else MaterialTheme.colorScheme.background.luminance() < 0.5f
                                val textPrimary = onBackgroundColor
                                val textSecondary = onBackgroundColor.copy(alpha = 0.55f)
                                val primaryColor = MaterialTheme.colorScheme.primary
                                val activeRowBg = if (isDarkTheme) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f)

                                val currentArtist = mediaMetadata?.artists?.joinToString { it.name }
                                val artistString = trackMetadata.artists.joinToString { it.name }
                                val showSubtitle = artistString.isNotBlank() && isMultiArtistQueue && artistString != currentArtist

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 1.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .then(
                                            if (isActive) {
                                                Modifier.background(activeRowBg)
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                if (selection) {
                                                    if (trackMetadata in selectedSongs) {
                                                        selectedSongs.remove(trackMetadata)
                                                        selectedItems.remove(currentItem)
                                                    } else {
                                                        selectedSongs.add(trackMetadata)
                                                        selectedItems.add(currentItem)
                                                    }
                                                } else {
                                                    if (index == currentWindowIndex) {
                                                        playerConnection.player.togglePlayPause()
                                                    } else {
                                                        val joined =
                                                            togetherSessionState as? com.tamed.music.together.TogetherSessionState.Joined
                                                        val isGuest = joined?.role is com.tamed.music.together.TogetherRole.Guest
                                                        if (isGuest) {
                                                            if (joined?.roomState?.settings?.allowGuestsToControlPlayback != true) {
                                                                Toast.makeText(context, R.string.not_allowed, Toast.LENGTH_SHORT).show()
                                                                return@combinedClickable
                                                            }
                                                            val trackId =
                                                                window.mediaItem.metadata?.id?.trim().orEmpty().ifBlank {
                                                                    window.mediaItem.mediaId.trim()
                                                                }
                                                            if (trackId.isBlank()) return@combinedClickable
                                                            Toast.makeText(context, R.string.together_requesting_song_change, Toast.LENGTH_SHORT).show()
                                                            playerConnection.service.requestTogetherControl(
                                                                com.tamed.music.together.ControlAction.SeekToTrack(
                                                                    trackId = trackId,
                                                                    positionMs = 0L,
                                                                ),
                                                            )
                                                        } else {
                                                            playerConnection.player.seekToDefaultPosition(
                                                                window.firstPeriodIndex,
                                                            )
                                                            playerConnection.player.playWhenReady = true
                                                        }
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                if (!selection) {
                                                    selection = true
                                                }
                                                selectedSongs.clear()
                                                selectedSongs.add(trackMetadata)
                                            },
                                        )
                                        .padding(horizontal = 8.dp, vertical = if (showSubtitle) 6.dp else 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (selection) {
                                        Checkbox(
                                            checked = trackMetadata in selectedSongs,
                                            onCheckedChange = { isChecked ->
                                                if (isChecked) {
                                                    selectedSongs.add(trackMetadata)
                                                    selectedItems.add(currentItem)
                                                } else {
                                                    selectedSongs.remove(trackMetadata)
                                                    selectedItems.remove(currentItem)
                                                }
                                            },
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.width(28.dp),
                                            contentAlignment = Alignment.CenterStart,
                                        ) {
                                            if (isActive && isPlaying) {
                                                Icon(
                                                    painter = painterResource(R.drawable.volume_up),
                                                    contentDescription = "Playing",
                                                    tint = primaryColor,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            } else {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontSize = 14.sp,
                                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isActive) textPrimary else textSecondary,
                                                    ),
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = trackMetadata.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontSize = 14.5.sp,
                                                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = textPrimary,
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false),
                                            )
                                            if (trackMetadata.explicit) {
                                                ExplicitTag(
                                                    color = textSecondary,
                                                    size = 12.dp,
                                                )
                                            }
                                        }

                                        if (showSubtitle) {
                                            Text(
                                                text = artistString,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 12.sp,
                                                    color = textSecondary,
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(bounded = true, radius = 16.dp),
                                                onClick = {
                                                    menuState.show {
                                                        PlayerMenu(
                                                            mediaMetadata = trackMetadata,
                                                            navController = navController,
                                                            playerBottomSheetState = playerBottomSheetState,
                                                            isQueueTrigger = true,
                                                            onShowDetailsDialog = {
                                                                window.mediaItem.mediaId.let {
                                                                    bottomSheetPageState.show {
                                                                        ShowMediaInfo(it)
                                                                    }
                                                                }
                                                            },
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                }
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_horiz),
                                            contentDescription = null,
                                            tint = if (isActive) textPrimary.copy(alpha = 0.85f) else textSecondary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }

                                    if (!effectiveLocked) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .draggableHandle()
                                                .graphicsLayer {
                                                    alpha = 0.99f
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.drag_handle),
                                                contentDescription = null,
                                                tint = if (isActive) textPrimary.copy(alpha = 0.85f) else textSecondary,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (effectiveLocked || selection) {
                            content()
                        } else {
                            SwipeToDismissBox(
                                state = dismissBoxState,
                                backgroundContent = {},
                                gesturesEnabled = !reorderableState.isAnyItemDragging,
                            ) {
                                content()
                            }
                        }
                    }
                }


            }
        }

        // Old header hidden - now using sticky header at top of queue
        Column(
            modifier =
            Modifier
                .height(0.dp)
        ) {

            AnimatedVisibility(
                visible = selection,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Row(
                    modifier =
                    Modifier
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val count = selectedSongs.size
                    IconButton(
                        onClick = {
                            selection = false
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                        )
                    }
                    Text(
                        text = stringResource(R.string.elements_selected, count),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (count == mutableQueueWindows.size) {
                                selectedSongs.clear()
                                selectedItems.clear()
                            } else {
                                queueWindows
                                    .filter { it.mediaItem.metadata!! !in selectedSongs }
                                    .forEach {
                                        selectedSongs.add(it.mediaItem.metadata!!)
                                        selectedItems.add(it)
                                    }
                            }
                        },
                    ) {
                        Icon(
                            painter =
                            painterResource(
                                if (count == mutableQueueWindows.size) {
                                    R.drawable.deselect
                                } else {
                                    R.drawable.select_all
                                },
                            ),
                            contentDescription = null,
                        )
                    }

                    IconButton(
                        onClick = {
                            menuState.show {
                                SelectionMediaMetadataMenu(
                                    songSelection = selectedSongs,
                                    onDismiss = menuState::dismiss,
                                    clearAction = {
                                        selectedSongs.clear()
                                        selectedItems.clear()
                                    },
                                    currentItems = selectedItems,
                                )
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null,
                            tint = LocalContentColor.current,
                        )
                    }
                }
            }
            if (pureBlack) {
                    HorizontalDivider()
            }
        }

        val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()

        // Bottom bar hidden - controls now in sticky header
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
            modifier =
            Modifier
                .height(0.dp)
                .align(Alignment.BottomCenter)
        ) {
            IconButton(
                modifier = Modifier.align(Alignment.CenterStart),
                onClick = {
                    coroutineScope
                        .launch {
                            lazyListState.animateScrollToItem(
                                if (playerConnection.player.shuffleModeEnabled) playerConnection.player.currentMediaItemIndex else 0,
                            )
                        }.invokeOnCompletion {
                            playerConnection.player.shuffleModeEnabled =
                                !playerConnection.player.shuffleModeEnabled
                        }
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = null,
                    modifier = Modifier.alpha(if (shuffleModeEnabled) 1f else 0.5f),
                )
            }

            Icon(
                painter = painterResource(R.drawable.expand_more),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center),
            )

            IconButton(
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = playerConnection.player::toggleRepeatMode,
            ) {
                Icon(
                    painter =
                    painterResource(
                        when (repeatMode) {
                            Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                            else -> throw IllegalStateException()
                        },
                    ),
                    contentDescription = null,
                    modifier = Modifier.alpha(if (repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f),
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
            Modifier
                .padding(
                    bottom =
                    ListItemHeight +
                            WindowInsets.systemBars
                                .asPaddingValues()
                                .calculateBottomPadding(),
                )
                .align(Alignment.BottomCenter),
        )
    }
}
}
}

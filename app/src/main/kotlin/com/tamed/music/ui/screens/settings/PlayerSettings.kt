/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.R
import com.tamed.music.constants.ArtistSeparatorsKey
import com.tamed.music.constants.ExternalDownloaderEnabledKey
import com.tamed.music.constants.ExternalDownloaderPackageKey
import com.tamed.music.constants.AudioNormalizationKey
import com.tamed.music.constants.AudioOffload
import com.tamed.music.sources.AudioQuality
import com.tamed.music.sources.SourceSettings
import com.tamed.music.playback.smart.AutomixSettings
import com.tamed.music.constants.AudioQualityKey
import com.tamed.music.constants.NetworkMeteredKey
import com.tamed.music.constants.AutoDownloadOnLikeKey
import com.tamed.music.constants.AutoStartOnBluetoothKey
import com.tamed.music.constants.AutoSkipNextOnErrorKey
import com.tamed.music.constants.PauseOnDeviceMuteKey
import com.tamed.music.constants.PermanentShuffleKey
import com.tamed.music.constants.PersistentQueueKey

import com.tamed.music.constants.SkipSilenceKey
import com.tamed.music.constants.StopMusicOnTaskClearKey
import com.tamed.music.constants.WakelockKey
import com.tamed.music.ui.component.SwitchPreference
import com.tamed.music.constants.HistoryDuration
import com.tamed.music.constants.AudioCrossfadeDurationKey
import com.tamed.music.constants.AutomixEnabledKey
import com.tamed.music.constants.PlayerStreamClient
import com.tamed.music.constants.PlayerStreamClientKey
import com.tamed.music.constants.SeekExtraSeconds
import com.tamed.music.ui.component.ArtistSeparatorsDialog
import com.tamed.music.ui.component.TagsManagementDialog
import com.tamed.music.ui.component.TextFieldDialog
import com.tamed.music.ui.component.EnumListPreference
import com.tamed.music.ui.component.ListPreference
import com.tamed.music.ui.component.IconButton
import com.tamed.music.ui.component.ListDialog
import com.tamed.music.ui.component.PreferenceEntry
import com.tamed.music.ui.component.PreferenceGroupTitle
import com.tamed.music.ui.component.SliderPreference
import com.tamed.music.ui.component.CrossfadeSliderPreference
import com.tamed.music.ui.component.SwitchPreference
import com.tamed.music.ui.utils.backToMain
import com.tamed.music.utils.rememberEnumPreference
import com.tamed.music.utils.rememberPreference
import com.tamed.music.LocalDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(
        AudioQualityKey,
        defaultValue = AudioQuality.LOSSLESS
    )
    val (playerStreamClient, onPlayerStreamClientChange) = rememberEnumPreference(
        PlayerStreamClientKey,
        defaultValue = PlayerStreamClient.ANDROID_VR
    )
    val (networkMetered, onNetworkMeteredChange) = rememberPreference(
        NetworkMeteredKey,
        defaultValue = true
    )
    val (persistentQueue, onPersistentQueueChange) = rememberPreference(
        PersistentQueueKey,
        defaultValue = true
    )
    val (permanentShuffle, onPermanentShuffleChange) = rememberPreference(
        PermanentShuffleKey,
        defaultValue = false
    )
    val (skipSilence, onSkipSilenceChange) = rememberPreference(
        SkipSilenceKey,
        defaultValue = false
    )
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(
        AudioNormalizationKey,
        defaultValue = true
    )
    val (audioOffload, onAudioOffloadChange) = rememberPreference(
        AudioOffload,
        defaultValue = false
    )
    val (automixEnabled, onAutomixEnabledChange) = rememberPreference(
        AutomixEnabledKey,
        defaultValue = false
    )

    val (seekExtraSeconds, onSeekExtraSeconds) = rememberPreference(
        SeekExtraSeconds,
        defaultValue = false
    )

    val (autoDownloadOnLike, onAutoDownloadOnLikeChange) = rememberPreference(
        AutoDownloadOnLikeKey,
        defaultValue = false
    )
    val (autoSkipNextOnError, onAutoSkipNextOnErrorChange) = rememberPreference(
        AutoSkipNextOnErrorKey,
        defaultValue = false
    )
    val (pauseOnDeviceMute, onPauseOnDeviceMuteChange) = rememberPreference(
        PauseOnDeviceMuteKey,
        defaultValue = false
    )
    val (autoStartOnBluetooth, onAutoStartOnBluetoothChange) = rememberPreference(
        AutoStartOnBluetoothKey,
        defaultValue = false
    )
    val (stopMusicOnTaskClear, onStopMusicOnTaskClearChange) = rememberPreference(
        StopMusicOnTaskClearKey,
        defaultValue = true
    )
    val (historyDuration, onHistoryDurationChange) = rememberPreference(
        HistoryDuration,
        defaultValue = 30f
    )

    val (audioCrossfadeSeconds, onAudioCrossfadeSecondsChange) = rememberPreference(
        AudioCrossfadeDurationKey,
        defaultValue = 0
    )

    val (artistSeparators, onArtistSeparatorsChange) = rememberPreference(
        ArtistSeparatorsKey,
        defaultValue = ",;/&"
    )
    val (externalDownloaderEnabled, onExternalDownloaderEnabledChange) = rememberPreference(
        ExternalDownloaderEnabledKey,
        defaultValue = false
    )
    val (externalDownloaderPackage, onExternalDownloaderPackageChange) = rememberPreference(
        ExternalDownloaderPackageKey,
        defaultValue = ""
    )

    val (wakelockEnabled, onWakelockChange) = rememberPreference(
        WakelockKey,
        defaultValue = true
    )

    val (skipSilenceInstant, onSkipSilenceInstantChange) = rememberPreference(
        com.tamed.music.constants.SkipSilenceInstantKey,
        defaultValue = false
    )

    var showArtistSeparatorsDialog by remember { mutableStateOf(false) }
    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showTagsManagementDialog by remember { mutableStateOf(false) }
    var showPlayerStreamClientDialog by remember { mutableStateOf(false) }
    var showExternalDownloaderPackageDialog by remember { mutableStateOf(false) }
    val database = LocalDatabase.current

    if (showAudioQualityDialog) {
        ListDialog(
            onDismiss = { showAudioQualityDialog = false },
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            items(AudioQuality.entries) { quality ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onAudioQualityChange(quality)
                            SourceSettings.audioQuality.value = quality
                            showAudioQualityDialog = false
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = quality == audioQuality,
                        onClick = null,
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = quality.label,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = quality.detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        // Unified Audio Quality
        PreferenceEntry(
            title = { Text(stringResource(R.string.audio_quality)) },
            description = "${audioQuality.label} · ${audioQuality.detail}",
            icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
            onClick = { showAudioQualityDialog = true }
        )

        // Audio Sources & Priority
        PreferenceEntry(
            title = { Text("Audio Sources & Priority") },
            description = "Manage module sources, JioSaavn, and YouTube fallback order",
            icon = { Icon(painterResource(R.drawable.music_note), null) },
            onClick = { navController.navigate("settings/sources") }
        )

        // Automix
        SwitchPreference(
            title = { Text("Automix") },
            icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
            checked = automixEnabled,
            onCheckedChange = {
                AutomixSettings.enabled.value = it
                onAutomixEnabledChange(it)
            },
        )

        // Crossfade
        CrossfadeSliderPreference(
            value = audioCrossfadeSeconds,
            onValueChange = { seconds ->
                AutomixSettings.crossfadeDurationSeconds.value = seconds
                onAudioCrossfadeSecondsChange(seconds)
            },
            isEnabled = true,
        )

        // History Duration
        SliderPreference(
            title = { Text(stringResource(R.string.history_duration)) },
            icon = { Icon(painterResource(R.drawable.history), null) },
            value = historyDuration,
            onValueChange = onHistoryDurationChange,
        )

        // Skip silence
        SwitchPreference(
            title = { Text(stringResource(R.string.skip_silence)) },
            icon = { Icon(painterResource(R.drawable.fast_forward), null) },
            checked = skipSilence,
            onCheckedChange = onSkipSilenceChange,
            isEnabled = !audioOffload,
        )

        // Instantly Skip Silence
        SwitchPreference(
            title = { Text("Instantly Skip Silence") },
            icon = { Icon(painterResource(R.drawable.fast_forward), null) },
            checked = skipSilenceInstant,
            onCheckedChange = onSkipSilenceInstantChange,
            isEnabled = !audioOffload && skipSilence,
        )

        // Audio Normalization
        SwitchPreference(
            title = { Text(stringResource(R.string.audio_normalization)) },
            icon = { Icon(painterResource(R.drawable.volume_up), null) },
            checked = audioNormalization,
            onCheckedChange = onAudioNormalizationChange
        )

        Spacer(modifier = Modifier.height(140.dp))
    }

    TopAppBar(
        title = { Text("Player") },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        }
    )
}

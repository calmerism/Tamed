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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
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
import com.tamed.music.constants.AudioQuality
import com.tamed.music.constants.AudioQualityKey
import com.tamed.music.constants.LosslessStreamingProvider
import com.tamed.music.constants.LosslessStreamingProviderKey
import com.tamed.music.constants.LosslessStreamingQuality
import com.tamed.music.constants.LosslessStreamingQualityKey
import com.tamed.music.constants.QobuzBackend
import com.tamed.music.constants.QobuzBackendKey
import com.tamed.music.constants.QobuzCountryKey
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
import com.tamed.music.constants.HistoryDuration
import com.tamed.music.constants.AudioCrossfadeDurationKey
import com.tamed.music.constants.PlayerStreamClient
import com.tamed.music.constants.PlayerStreamClientKey
import com.tamed.music.constants.SeekExtraSeconds
import com.tamed.music.ui.component.ArtistSeparatorsDialog
import com.tamed.music.ui.component.TagsManagementDialog
import com.tamed.music.ui.component.TextFieldDialog
import com.tamed.music.ui.component.EnumListPreference
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(
        AudioQualityKey,
        defaultValue = AudioQuality.AUTO
    )
    val (losslessStreamingProvider, onLosslessStreamingProviderChange) = rememberEnumPreference(
        LosslessStreamingProviderKey,
        defaultValue = LosslessStreamingProvider.QOBUZ
    )
    val (losslessStreamingQuality, onLosslessStreamingQualityChange) = rememberEnumPreference(
        LosslessStreamingQualityKey,
        defaultValue = LosslessStreamingQuality.HI_RES_MAX
    )
    val (qobuzBackend, onQobuzBackendChange) = rememberEnumPreference(
        QobuzBackendKey,
        defaultValue = QobuzBackend.JUMO
    )
    val (qobuzCountry, onQobuzCountryChange) = rememberPreference(
        QobuzCountryKey,
        defaultValue = "US"
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
        defaultValue = false
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

    var showArtistSeparatorsDialog by remember { mutableStateOf(false) }
    var showTagsManagementDialog by remember { mutableStateOf(false) }
    var showPlayerStreamClientDialog by remember { mutableStateOf(false) }
    var showExternalDownloaderPackageDialog by remember { mutableStateOf(false) }
    var showQobuzCountryDialog by remember { mutableStateOf(false) }
    val database = LocalDatabase.current
    val normalizedQobuzCountry =
        qobuzCountry.trim().uppercase(Locale.US).takeIf { it.matches(Regex("[A-Z]{2}")) } ?: "US"

    if (showArtistSeparatorsDialog) {
        ArtistSeparatorsDialog(
            currentSeparators = artistSeparators,
            onDismiss = { showArtistSeparatorsDialog = false },
            onSave = { newSeparators ->
                onArtistSeparatorsChange(newSeparators)
                showArtistSeparatorsDialog = false
            }
        )
    }

    if (showTagsManagementDialog) {
        TagsManagementDialog(
            database = database,
            onDismiss = { showTagsManagementDialog = false }
        )
    }

    if (showExternalDownloaderPackageDialog) {
        TextFieldDialog(
            initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(externalDownloaderPackage),
            onDone = { pkg ->
                onExternalDownloaderPackageChange(pkg)
                showExternalDownloaderPackageDialog = false
            },
            onDismiss = { showExternalDownloaderPackageDialog = false },
            singleLine = true,
            maxLines = 1,
        )
    }

    if (showQobuzCountryDialog) {
        TextFieldDialog(
            initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(normalizedQobuzCountry),
            onDone = { country ->
                val normalized = country.trim().uppercase(Locale.US)
                onQobuzCountryChange(normalized.ifBlank { "US" })
                showQobuzCountryDialog = false
            },
            onDismiss = { showQobuzCountryDialog = false },
            singleLine = true,
            maxLines = 1,
        )
    }

    if (showPlayerStreamClientDialog) {
        ListDialog(
            onDismiss = { showPlayerStreamClientDialog = false },
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            items(listOf(PlayerStreamClient.ANDROID_VR, PlayerStreamClient.WEB_REMIX)) { value ->
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onPlayerStreamClientChange(value)
                            showPlayerStreamClientDialog = false
                        }.padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    RadioButton(
                        selected = value == playerStreamClient,
                        onClick = null,
                    )

                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            text =
                            when (value) {
                                PlayerStreamClient.ANDROID_VR -> stringResource(R.string.player_stream_client_android_vr)
                                else -> stringResource(R.string.player_stream_client_web_remix)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text =
                            when (value) {
                                PlayerStreamClient.ANDROID_VR -> stringResource(R.string.player_stream_client_android_vr_desc)
                                else -> stringResource(R.string.player_stream_client_web_remix_desc)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
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

        PreferenceGroupTitle(
            title = stringResource(R.string.player)
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.audio_quality)) },
            icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
            selectedValue = audioQuality,
            onValueSelected = onAudioQualityChange,
            valueText = {
                when (it) {
                    AudioQuality.HIGHEST -> stringResource(R.string.audio_quality_max)
                    AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                    AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                    AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                }
            }
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.lossless_streaming)) },
            icon = { Icon(painterResource(R.drawable.music_note), null) },
            selectedValue = losslessStreamingProvider,
            onValueSelected = onLosslessStreamingProviderChange,
            valueText = {
                when (it) {
                    LosslessStreamingProvider.OFF -> stringResource(R.string.lossless_streaming_off)
                    LosslessStreamingProvider.QOBUZ -> stringResource(R.string.lossless_streaming_qobuz)
                }
            }
        )

        if (losslessStreamingProvider != LosslessStreamingProvider.OFF) {
            EnumListPreference(
                title = { Text(stringResource(R.string.lossless_streaming_quality)) },
                icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
                selectedValue = losslessStreamingQuality,
                onValueSelected = onLosslessStreamingQualityChange,
                valueText = {
                    when (it) {
                        LosslessStreamingQuality.LOSSLESS -> stringResource(R.string.flac_lossless)
                        LosslessStreamingQuality.HI_RES -> stringResource(R.string.hi_res_flac)
                        LosslessStreamingQuality.HI_RES_MAX -> stringResource(R.string.hi_res_flac_max)
                    }
                }
            )

            if (losslessStreamingProvider == LosslessStreamingProvider.QOBUZ) {
                EnumListPreference(
                    title = { Text(stringResource(R.string.qobuz_backend)) },
                    icon = { Icon(painterResource(R.drawable.integration), null) },
                    selectedValue = qobuzBackend,
                    onValueSelected = onQobuzBackendChange,
                    valueText = {
                        when (it) {
                            QobuzBackend.JUMO -> stringResource(R.string.qobuz_backend_jumo)
                            QobuzBackend.KENNY -> stringResource(R.string.qobuz_backend_kenny)
                            QobuzBackend.SQUID -> stringResource(R.string.qobuz_backend_squid)
                        }
                    }
                )

                PreferenceEntry(
                    title = { Text(stringResource(R.string.qobuz_country)) },
                    description = stringResource(R.string.qobuz_country_desc, normalizedQobuzCountry),
                    icon = { Icon(painterResource(R.drawable.language), null) },
                    onClick = { showQobuzCountryDialog = true }
                )
            }

            PreferenceEntry(
                title = { Text(stringResource(R.string.experimental_feature)) },
                description = stringResource(R.string.lossless_streaming_qobuz_desc),
                icon = { Icon(painterResource(R.drawable.info), null) },
                onClick = null,
            )
        }

        PreferenceEntry(
            title = { Text(stringResource(R.string.player_stream_client)) },
            description =
            when (playerStreamClient) {
                PlayerStreamClient.ANDROID_VR -> stringResource(R.string.player_stream_client_android_vr)
                else -> stringResource(R.string.player_stream_client_web_remix)
            },
            icon = { Icon(painterResource(R.drawable.integration), null) },
            onClick = { showPlayerStreamClientDialog = true }
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.network_metered_title)) },
            description = stringResource(R.string.network_metered_description),
            icon = { Icon(painterResource(R.drawable.android_cell), null) },
            checked = networkMetered,
            onCheckedChange = onNetworkMeteredChange
        )

        SliderPreference(
            title = { Text(stringResource(R.string.history_duration)) },
            icon = { Icon(painterResource(R.drawable.history), null) },
            value = historyDuration,
            onValueChange = onHistoryDurationChange,
        )

        CrossfadeSliderPreference(
            value = audioCrossfadeSeconds,
            onValueChange = onAudioCrossfadeSecondsChange,
            isEnabled = !audioOffload,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.skip_silence)) },
            icon = { Icon(painterResource(R.drawable.fast_forward), null) },
            checked = skipSilence,
            onCheckedChange = onSkipSilenceChange,
            isEnabled = !audioOffload,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.audio_normalization)) },
            icon = { Icon(painterResource(R.drawable.volume_up), null) },
            checked = audioNormalization,
            onCheckedChange = onAudioNormalizationChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.audio_offload)) },
            description = stringResource(R.string.audio_offload_desc),
            icon = { Icon(painterResource(R.drawable.speed), null) },
            checked = audioOffload,
            onCheckedChange = { enabled ->
                onAudioOffloadChange(enabled)
                if (enabled) {
                    onSkipSilenceChange(false)
                }
            }
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.seek_seconds_addup)) },
            description = stringResource(R.string.seek_seconds_addup_description),
            icon = { Icon(painterResource(R.drawable.arrow_forward), null) },
            checked = seekExtraSeconds,
            onCheckedChange = onSeekExtraSeconds
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.pause_on_device_mute)) },
            description = stringResource(R.string.pause_on_device_mute_desc),
            icon = { Icon(painterResource(R.drawable.volume_off), null) },
            checked = pauseOnDeviceMute,
            onCheckedChange = onPauseOnDeviceMuteChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.auto_start_on_bluetooth)) },
            description = stringResource(R.string.auto_start_on_bluetooth_desc),
            icon = { Icon(painterResource(R.drawable.bluetooth), null) },
            checked = autoStartOnBluetooth,
            onCheckedChange = onAutoStartOnBluetoothChange
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.queue)
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.persistent_queue)) },
            description = stringResource(R.string.persistent_queue_desc),
            icon = { Icon(painterResource(R.drawable.queue_music), null) },
            checked = persistentQueue,
            onCheckedChange = onPersistentQueueChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.permanent_shuffle)) },
            description = stringResource(R.string.permanent_shuffle_desc),
            icon = { Icon(painterResource(R.drawable.shuffle), null) },
            checked = permanentShuffle,
            onCheckedChange = onPermanentShuffleChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.auto_download_on_like)) },
            description = stringResource(R.string.auto_download_on_like_desc),
            icon = { Icon(painterResource(R.drawable.download), null) },
            checked = autoDownloadOnLike,
            onCheckedChange = onAutoDownloadOnLikeChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.auto_skip_next_on_error)) },
            description = stringResource(R.string.auto_skip_next_on_error_desc),
            icon = { Icon(painterResource(R.drawable.skip_next), null) },
            checked = autoSkipNextOnError,
            onCheckedChange = onAutoSkipNextOnErrorChange
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.misc)
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.stop_music_on_task_clear)) },
            icon = { Icon(painterResource(R.drawable.clear_all), null) },
            checked = stopMusicOnTaskClear,
            onCheckedChange = onStopMusicOnTaskClearChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.wakelock)) },
            description = stringResource(R.string.wakelock_desc),
            icon = { Icon(painterResource(R.drawable.bolt), null) },
            checked = wakelockEnabled,
            onCheckedChange = onWakelockChange
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.artist_separators)) },
            description = artistSeparators.map { "\"$it\"" }.joinToString("  "),
            icon = { Icon(painterResource(R.drawable.artist), null) },
            onClick = { showArtistSeparatorsDialog = true }
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.manage_playlist_tags)) },
            description = stringResource(R.string.manage_playlist_tags_desc),
            icon = { Icon(painterResource(R.drawable.style), null) },
            onClick = { showTagsManagementDialog = true }
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.external_downloader)) },
            description = stringResource(R.string.external_downloader_desc),
            icon = { Icon(painterResource(R.drawable.download), null) },
            checked = externalDownloaderEnabled,
            onCheckedChange = onExternalDownloaderEnabledChange
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.external_downloader_package)) },
            description = externalDownloaderPackage.ifEmpty { stringResource(R.string.external_downloader_package_desc) },
            icon = { Icon(painterResource(R.drawable.integration), null) },
            onClick = { showExternalDownloaderPackageDialog = true },
            isEnabled = externalDownloaderEnabled
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.player_and_audio)) },
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

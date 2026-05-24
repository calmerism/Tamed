/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.tamed.music.R
import com.tamed.music.ui.screens.musicrecognition.MusicRecognitionRoute

@Composable
fun buildQuickActions(
    navController: NavController,
    resetSearch: () -> Unit,
): List<SettingsQuickAction> =
    listOf(
        SettingsQuickAction(
            icon = painterResource(R.drawable.palette),
            label = stringResource(R.string.appearance),
            onClick = { resetSearch(); navController.navigate("settings/appearance") },
            accentColor = MaterialTheme.colorScheme.primary,
        ),
        SettingsQuickAction(
            icon = painterResource(R.drawable.play),
            label = stringResource(R.string.player_and_audio),
            onClick = { resetSearch(); navController.navigate("settings/player") },
            accentColor = MaterialTheme.colorScheme.tertiary,
        ),
        SettingsQuickAction(
            icon = painterResource(R.drawable.storage),
            label = stringResource(R.string.storage),
            onClick = { resetSearch(); navController.navigate("settings/storage") },
            accentColor = MaterialTheme.colorScheme.secondary,
        ),
        SettingsQuickAction(
            icon = painterResource(R.drawable.security),
            label = stringResource(R.string.privacy),
            onClick = { resetSearch(); navController.navigate("settings/privacy") },
            accentColor = MaterialTheme.colorScheme.error,
        ),
    )

@Composable
fun buildIntegrationActions(
    navController: NavController,
    resetSearch: () -> Unit,
): List<SettingsIntegrationAction> = emptyList()

@Composable
fun buildSettingsGroups(
    navController: NavController,
    isAndroid12OrLater: Boolean,
    context: Context,
    resetSearch: () -> Unit,
): List<SettingsGroup> =
    listOf(
        SettingsGroup(
            title = "",
            items = buildList {
                add(
                    SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = stringResource(R.string.appearance),
                        subtitle = "Theme, colors and UI styling",
                        accentColor = MaterialTheme.colorScheme.primary,
                        keywords = listOf("theme", "palette", "material you", "dynamic color", "font", "ui"),
                        onClick = { resetSearch(); navController.navigate("settings/appearance") },
                    )
                )
                add(
                    SettingsItem(
                        icon = painterResource(R.drawable.play),
                        title = stringResource(R.string.player_and_audio),
                        subtitle = stringResource(R.string.audio_quality),
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        keywords = listOf("audio", "playback", "volume", "quality", "equalizer", "crossfade"),
                        onClick = { resetSearch(); navController.navigate("settings/player") },
                    )
                )
                add(
                    SettingsItem(
                        icon = painterResource(R.drawable.language),
                        title = stringResource(R.string.content),
                        subtitle = stringResource(R.string.content_language),
                        accentColor = MaterialTheme.colorScheme.secondary,
                        keywords = listOf("language", "content", "lyrics", "translation", "region"),
                        onClick = { resetSearch(); navController.navigate("settings/content") },
                    )
                )
                add(
                    SettingsItem(
                        icon = painterResource(R.drawable.stats),
                        title = stringResource(R.string.stats),
                        subtitle = stringResource(R.string.most_played_albums),
                        accentColor = MaterialTheme.colorScheme.primary,
                        keywords = listOf("stats", "most played", "albums", "artists", "songs", "listening"),
                        onClick = { resetSearch(); navController.navigate("stats") },
                    )
                )
                add(
                    SettingsItem(
                        icon = painterResource(R.drawable.mic),
                        title = stringResource(R.string.music_recognition),
                        subtitle = "Identify songs playing around you",
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        keywords = listOf("music recognition", "recognize", "shazam", "identify song", "microphone"),
                        onClick = { resetSearch(); navController.navigate(MusicRecognitionRoute) },
                    )
                )
                add(
                    SettingsItem(
                        icon = painterResource(R.drawable.shuffle),
                        title = stringResource(R.string.shuffle),
                        subtitle = stringResource(R.string.permanent_shuffle_desc),
                        accentColor = MaterialTheme.colorScheme.primary,
                        keywords = listOf("shuffle", "permanent shuffle", "random play", "queue"),
                        onClick = { resetSearch(); navController.navigate("shuffle_settings") },
                    )
                )
                add(
                    SettingsItem(
                        icon = painterResource(R.drawable.security),
                        title = stringResource(R.string.privacy),
                        subtitle = stringResource(R.string.pause_listen_history),
                        accentColor = MaterialTheme.colorScheme.error,
                        keywords = listOf("privacy", "history", "tracking", "security", "permissions"),
                        onClick = { resetSearch(); navController.navigate("settings/privacy") },
                    )
                )
                add(
                    SettingsItem(
                        icon = painterResource(R.drawable.storage),
                        title = stringResource(R.string.storage),
                        subtitle = stringResource(R.string.cache),
                        accentColor = MaterialTheme.colorScheme.secondary,
                        keywords = listOf("storage", "cache", "offline", "downloads", "cleanup"),
                        onClick = { resetSearch(); navController.navigate("settings/storage") },
                    )
                )
                if (isAndroid12OrLater) {
                    add(
                        SettingsItem(
                            icon = painterResource(R.drawable.link),
                            title = stringResource(R.string.default_links),
                            subtitle = stringResource(R.string.open_supported_links),
                            accentColor = MaterialTheme.colorScheme.primary,
                            keywords = listOf("links", "deeplink", "default", "supported links"),
                            onClick = {
                                resetSearch()
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    when (e) {
                                        is ActivityNotFoundException,
                                        is SecurityException,
                                        -> {
                                            Toast.makeText(
                                                context,
                                                R.string.open_app_settings_error,
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                        else -> {
                                            Toast.makeText(
                                                context,
                                                R.string.open_app_settings_error,
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                    }
                                }
                            },
                        ),
                    )
                }
                add(
                    SettingsItem(
                        icon = painterResource(R.drawable.experiment),
                        title = stringResource(R.string.experiment_settings),
                        subtitle = stringResource(R.string.misc),
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        keywords = listOf("experimental", "debug", "developer", "labs", "internal"),
                        onClick = { resetSearch(); navController.navigate("settings/misc") },
                    ),
                )
            }
        )
    )

@Composable
fun buildInternalItems(
    navController: NavController,
    resetSearch: () -> Unit,
): List<SettingsItem> =
    listOf(
        SettingsItem(
            icon = painterResource(R.drawable.palette),
            title = stringResource(R.string.theme_creator_title),
            subtitle = stringResource(R.string.theme_creator_subtitle),
            accentColor = MaterialTheme.colorScheme.primary,
            keywords = listOf("theme", "creator", "seed", "material", "palette", "import", "export"),
            onClick = { resetSearch(); navController.navigate("settings/appearance/theme_creator") },
        ),
        SettingsItem(
            icon = painterResource(R.drawable.palette),
            title = stringResource(R.string.customize_colors),
            subtitle = stringResource(R.string.appearance),
            accentColor = MaterialTheme.colorScheme.primary,
            keywords = listOf("palette", "color", "accent", "tone", "dynamic color"),
            onClick = { resetSearch(); navController.navigate("settings/appearance/palette_picker") },
        ),
        SettingsItem(
            icon = painterResource(R.drawable.image),
            title = stringResource(R.string.customize_background_title),
            subtitle = stringResource(R.string.appearance),
            accentColor = MaterialTheme.colorScheme.secondary,
            keywords = listOf("background", "wallpaper", "image", "blur", "gradient"),
            onClick = { resetSearch(); navController.navigate("customize_background") },
        ),
    )

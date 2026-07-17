/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.tamed.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.R
import com.tamed.music.constants.LyricsV2BounceFactorKey
import com.tamed.music.constants.LyricsV2FillTransitionWidthKey
import com.tamed.music.constants.LyricsV2GlowFactorKey
import com.tamed.music.constants.LyricsV2LrcBounceEnabledKey
import com.tamed.music.ui.component.IconButton
import com.tamed.music.ui.component.PreferenceEntry
import com.tamed.music.ui.component.PreferenceGroup
import com.tamed.music.ui.utils.backToMain
import com.tamed.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsAnimationSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (bounceFactor, onBounceFactorChange) = rememberPreference(LyricsV2BounceFactorKey, defaultValue = 1f)
    val (glowFactor, onGlowFactorChange) = rememberPreference(LyricsV2GlowFactorKey, defaultValue = 1f)
    val (fillTransitionWidth, onFillTransitionWidthChange) = rememberPreference(LyricsV2FillTransitionWidthKey, defaultValue = 8f)
    val (lrcBounceEnabled, onLrcBounceEnabledChange) = rememberPreference(LyricsV2LrcBounceEnabledKey, defaultValue = true)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        TopAppBar(
            title = { Text(text = stringResource(R.string.lyrics_animation_style)) },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
            scrollBehavior = scrollBehavior,
        )

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                        ),
                    ).padding(bottom = 80.dp),
        ) {
            PreferenceGroup(title = "Animation Tuning") {
                    PreferenceEntry(
                        title = { Text("Line Bounce Effect") },
                        description = "Enable bounce animation for line-synced (LRC) lyrics",
                        icon = { Icon(painterResource(R.drawable.animation), null) },
                        trailingContent = {
                            Switch(
                                checked = lrcBounceEnabled,
                                onCheckedChange = onLrcBounceEnabledChange,
                            )
                        },
                    )

                    PreferenceEntry(
                        title = { Text("Bounce Amplitude") },
                        description = "Adjust the bounce effect when a word is sung (${(bounceFactor * 100).toInt()}%)",
                        icon = { Icon(painterResource(R.drawable.animation), null) },
                        content = {
                            Slider(
                                value = bounceFactor,
                                onValueChange = onBounceFactorChange,
                                valueRange = 0f..2f,
                            )
                        },
                    )

                    PreferenceEntry(
                        title = { Text("Glow Intensity") },
                        description = "Adjust the glow brightness of the sung word (${(glowFactor * 100).toInt()}%)",
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        content = {
                            Slider(
                                value = glowFactor,
                                onValueChange = onGlowFactorChange,
                                valueRange = 0f..2f,
                            )
                        },
                    )

                    PreferenceEntry(
                        title = { Text("Fill Transition Smoothness") },
                        description = "Adjust the gradient edge width of the liquid fill effect (${fillTransitionWidth.toInt()} dp)",
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        content = {
                            Slider(
                                value = fillTransitionWidth,
                                onValueChange = onFillTransitionWidthChange,
                                valueRange = 2f..24f,
                            )
                        },
                    )
            }
        }
    }
}

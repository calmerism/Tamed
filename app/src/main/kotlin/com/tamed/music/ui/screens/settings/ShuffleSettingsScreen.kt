/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.R
import com.tamed.music.constants.PermanentShuffleKey
import com.tamed.music.ui.component.GlassIconCircleButton
import com.tamed.music.ui.component.GlassPanel
import com.tamed.music.ui.theme.TamedAppleTypography
import com.tamed.music.ui.theme.appleBackgroundColor
import com.tamed.music.ui.theme.appleDividerColor
import com.tamed.music.ui.theme.applePrimaryTextColor
import com.tamed.music.ui.theme.appleSecondaryTextColor
import com.tamed.music.utils.rememberPreference
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun ShuffleSettingsScreen(
    navController: NavController,
) {
    val (permanentShuffle, onPermanentShuffleChange) = rememberPreference(
        PermanentShuffleKey,
        defaultValue = false,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appleBackgroundColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top + WindowInsetsSides.Bottom
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.shuffle),
                    style = TamedAppleTypography.largeTitle(),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.permanent_shuffle_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    color = appleSecondaryTextColor(),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            GlassIconCircleButton(
                iconRes = R.drawable.arrow_back,
                contentDescription = stringResource(R.string.back_button_desc),
                onClick = navController::navigateUp,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.permanent_shuffle),
                        style = MaterialTheme.typography.titleMedium,
                        color = applePrimaryTextColor(),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.permanent_shuffle_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = appleSecondaryTextColor(),
                    )
                }
                Switch(
                    checked = permanentShuffle,
                    onCheckedChange = onPermanentShuffleChange,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = appleDividerColor())
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "When enabled, starting a new song or queue keeps playback in shuffle mode automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = appleSecondaryTextColor(),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "More playback controls",
                style = MaterialTheme.typography.titleMedium,
                color = applePrimaryTextColor(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Open Player and audio settings for queue, crossfade, normalization, and stream options.",
                style = MaterialTheme.typography.bodyMedium,
                color = appleSecondaryTextColor(),
            )
            Spacer(modifier = Modifier.height(14.dp))
            GlassIconCircleButton(
                iconRes = R.drawable.play,
                contentDescription = stringResource(R.string.player_and_audio),
                onClick = { navController.navigate("settings/player") },
                modifier = Modifier,
            )
        }
    }
}
}

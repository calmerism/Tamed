package com.tamed.music.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.R
import com.tamed.music.constants.CanvasSource
import com.tamed.music.constants.CanvasSourceKey
import com.tamed.music.constants.TamedCanvasKey
import com.tamed.music.ui.component.AnimatedRadioButton
import com.tamed.music.ui.component.IconButton
import com.tamed.music.ui.component.PreferenceEntry
import com.tamed.music.ui.component.PreferenceGroup
import com.tamed.music.ui.utils.backToMain
import com.tamed.music.utils.rememberEnumPreference
import com.tamed.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasSelection(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val (tamedCanvasEnabled, onTamedCanvasEnabledChange) = rememberPreference(
        TamedCanvasKey,
        defaultValue = false
    )
    val (canvasSource, onCanvasSourceChange) = rememberEnumPreference(
        CanvasSourceKey,
        defaultValue = CanvasSource.AUTO
    )

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
    ) {
        // Description text
        Text(
            text = stringResource(R.string.tamed_canvas_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 16.dp)
        )

        // Large capsule banner for main toggle
        val containerColor by animateColorAsState(
            targetValue = if (tamedCanvasEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
            label = "containerColor"
        )

        val contentColor = if (tamedCanvasEnabled) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Card(
            onClick = { onTamedCanvasEnabledChange(!tamedCanvasEnabled) },
            shape = RoundedCornerShape(50),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.use_canvas),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                Switch(
                    checked = tamedCanvasEnabled,
                    onCheckedChange = onTamedCanvasEnabledChange
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Options settings group
        PreferenceGroup(
            title = stringResource(R.string.canvas_source),
        ) {
            PreferenceEntry(
                icon = {
                    AnimatedRadioButton(
                        selected = canvasSource == CanvasSource.AUTO,
                        onClick = null,
                        enabled = tamedCanvasEnabled
                    )
                },
                title = { Text(stringResource(R.string.canvas_source_auto)) },
                description = stringResource(R.string.canvas_source_auto_desc),
                isEnabled = tamedCanvasEnabled,
                onClick = { onCanvasSourceChange(CanvasSource.AUTO) },
            )
            PreferenceEntry(
                icon = {
                    AnimatedRadioButton(
                        selected = canvasSource == CanvasSource.APPLE_MUSIC,
                        onClick = null,
                        enabled = tamedCanvasEnabled
                    )
                },
                title = { Text(stringResource(R.string.canvas_source_apple_music)) },
                description = stringResource(R.string.canvas_source_apple_music_desc),
                isEnabled = tamedCanvasEnabled,
                onClick = { onCanvasSourceChange(CanvasSource.APPLE_MUSIC) },
            )
            PreferenceEntry(
                icon = {
                    AnimatedRadioButton(
                        selected = canvasSource == CanvasSource.VIVIMUSIC,
                        onClick = null,
                        enabled = tamedCanvasEnabled
                    )
                },
                title = { Text(stringResource(R.string.canvas_source_vivimusic)) },
                description = stringResource(R.string.canvas_source_vivimusic_desc),
                isEnabled = tamedCanvasEnabled,
                onClick = { onCanvasSourceChange(CanvasSource.VIVIMUSIC) },
            )
            PreferenceEntry(
                icon = {
                    AnimatedRadioButton(
                        selected = canvasSource == CanvasSource.TIDAL,
                        onClick = null,
                        enabled = tamedCanvasEnabled
                    )
                },
                title = { Text(stringResource(R.string.canvas_source_tidal)) },
                description = stringResource(R.string.canvas_source_tidal_desc),
                isEnabled = tamedCanvasEnabled,
                onClick = { onCanvasSourceChange(CanvasSource.TIDAL) },
            )
        }
        Spacer(modifier = Modifier.height(36.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.tamed_canvas)) },
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
        }
    )
}

/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tamed.music.R
import com.tamed.music.spotiflac.SpotiFlacProvider
import com.tamed.music.spotiflac.SpotiFlacQuality
import com.tamed.music.ui.theme.appleSecondaryTextColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LosslessDownloadSheet(
    title: String,
    subtitle: String,
    trackCount: Int,
    onDismiss: () -> Unit,
    enableYouTubeDownload: Boolean = false,
    onStartYouTube: (() -> Unit)? = null,
    onStart: (SpotiFlacProvider, SpotiFlacQuality) -> Unit,
) {
    val qualities = remember { SpotiFlacQuality.all }
    var selectedProvider by remember { mutableStateOf(SpotiFlacProvider.TIDAL) }
    var selectedQuality by remember { mutableStateOf(qualities.first()) }
    var useYouTubeDownload by remember(enableYouTubeDownload) { mutableStateOf(enableYouTubeDownload) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = null,
                        modifier = Modifier.padding(15.dp),
                    )
                }
                Column {
                    Text(
                        text = pluralStringResource(R.plurals.n_song, trackCount, trackCount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = title.ifBlank { subtitle },
                        style = MaterialTheme.typography.bodyLarge,
                        color = appleSecondaryTextColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.download_from),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (enableYouTubeDownload) {
                        FilterChip(
                            selected = useYouTubeDownload,
                            onClick = { useYouTubeDownload = true },
                            label = { Text("YouTube") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                    SpotiFlacProvider.entries.forEach { provider ->
                        FilterChip(
                            selected = !useYouTubeDownload && selectedProvider == provider,
                            onClick = {
                                useYouTubeDownload = false
                                selectedProvider = provider
                            },
                            label = {
                                Text(
                                    text =
                                        if (provider == SpotiFlacProvider.TIDAL) {
                                            stringResource(R.string.tidal_recommended)
                                        } else {
                                            provider.label
                                        },
                                )
                            },
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                        )
                    }
                }
            }

            if (!useYouTubeDownload) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.select_quality),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.lossless_quality_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = appleSecondaryTextColor(),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    qualities.forEach { quality ->
                        val selected = quality == selectedQuality
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = stringResource(quality.titleRes),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = stringResource(quality.subtitleRes),
                                    color = appleSecondaryTextColor(),
                                )
                            },
                            leadingContent = {
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 1f else 0.18f),
                                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                ) {
                                    Icon(
                                        painter = painterResource(quality.iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.padding(12.dp),
                                    )
                                }
                            },
                            trailingContent = {
                                if (selected) {
                                    Icon(
                                        painter = painterResource(R.drawable.done),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { selectedQuality = quality },
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (useYouTubeDownload) {
                        onStartYouTube?.invoke()
                    } else {
                        onStart(selectedProvider, selectedQuality)
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.download),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (useYouTubeDownload) {
                        stringResource(R.string.action_download)
                    } else {
                        stringResource(R.string.start_lossless_download)
                    }
                )
            }
        }
    }
}

/*
 * Tamed Project (2026)
 * Licensed Under GPL-3.0
 */

package com.tamed.music.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.R
import com.tamed.music.sources.AudioQuality
import com.tamed.music.sources.SourceConfig
import com.tamed.music.sources.SourceKind
import com.tamed.music.sources.SourceRegistry
import com.tamed.music.sources.SourceSettings
import com.tamed.music.ui.component.ListDialog
import com.tamed.music.ui.component.PreferenceEntry
import com.tamed.music.ui.component.TextFieldDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val configs by SourceRegistry.configs.collectAsState()

    var showCustomModuleDialog by remember { mutableStateOf(false) }
    var editingCustomModuleUrl by remember { mutableStateOf("") }

    if (showCustomModuleDialog) {
        TextFieldDialog(
            icon = { Icon(painterResource(R.drawable.music_note), null) },
            title = { Text("Custom Module Source") },
            initialTextFieldValue = TextFieldValue(editingCustomModuleUrl),
            placeholder = { Text("https://example.com/module-index.json") },
            singleLine = true,
            onDone = { url ->
                SourceRegistry.setCustomModule(url)
                showCustomModuleDialog = false
            },
            onDismiss = { showCustomModuleDialog = false },
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )
        Spacer(Modifier.height(12.dp))

        val ordered = configs.sortedBy { it.kind.ordinal }
        ordered.forEach { config ->
            SourceConfigItem(
                config = config,
                onToggle = if (config.kind == SourceKind.YOUTUBE) null else { enabled ->
                    SourceRegistry.setEnabled(config.id, enabled)
                },
                onEdit = if (config.kind == SourceKind.CUSTOM_MODULE) {
                    {
                        editingCustomModuleUrl = config.baseUrl
                        showCustomModuleDialog = true
                    }
                } else null,
            )
        }

        val hasCustomModule = configs.any { it.kind == SourceKind.CUSTOM_MODULE }
        if (!hasCustomModule) {
            Spacer(Modifier.height(8.dp))
            PreferenceEntry(
                title = { Text("Add Custom Module") },
                description = "External QuickJS plugin source",
                icon = { Icon(painterResource(R.drawable.add), null) },
                onClick = {
                    editingCustomModuleUrl = ""
                    showCustomModuleDialog = true
                },
            )
        }

        Spacer(Modifier.height(140.dp))
    }

    TopAppBar(
        title = { Text("Audio Sources & Priority") },
        navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun SourceConfigItem(
    config: SourceConfig,
    onToggle: ((Boolean) -> Unit)?,
    onEdit: (() -> Unit)?,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onEdit != null) { onEdit?.invoke() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = if (config.baseUrl.isNotBlank() && config.kind.needsServer) config.baseUrl else config.kind.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (onToggle != null) {
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = config.enabled,
                    onCheckedChange = onToggle,
                )
            }
        }
    }
}

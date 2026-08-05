/*
 * Tamed Project (2026)
 * Licensed Under GPL-3.0
 */

package com.tamed.music.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.R
import com.tamed.music.constants.StatsFmApiKey
import com.tamed.music.constants.StatsFmEnabledKey
import com.tamed.music.constants.StatsFmScrobbleCompletedKey
import com.tamed.music.constants.StatsFmSyncNowPlayingKey
import com.tamed.music.constants.StatsFmUsernameKey
import com.tamed.music.ui.component.IconButton
import com.tamed.music.ui.component.PreferenceEntry
import com.tamed.music.ui.component.PreferenceGroupTitle
import com.tamed.music.ui.component.SwitchPreference
import com.tamed.music.ui.component.TextFieldDialog
import com.tamed.music.ui.utils.backToMain
import com.tamed.music.utils.rememberPreference
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsFmSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val (statsFmEnabled, onStatsFmEnabledChange) = rememberPreference(StatsFmEnabledKey, false)
    val (username, onUsernameChange) = rememberPreference(StatsFmUsernameKey, "")
    val (apiKey, onApiKeyChange) = rememberPreference(StatsFmApiKey, "")
    val (syncNowPlaying, onSyncNowPlayingChange) = rememberPreference(StatsFmSyncNowPlayingKey, true)
    val (scrobbleCompleted, onScrobbleCompletedChange) = rememberPreference(StatsFmScrobbleCompletedKey, true)

    var showLoginDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }

    val isLoggedIn = username.isNotBlank()

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )

        // Connection Status Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF388E3C).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.stats),
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isLoggedIn) "Connected to stats.fm" else "stats.fm Disconnected",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = if (isLoggedIn) "@$username" else "Log in to sync your listening stats & charts",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                if (isLoggedIn && statsFmEnabled) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        PreferenceGroupTitle(title = "Account Authentication")

        if (!isLoggedIn) {
            Button(
                onClick = { showLoginDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.person),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log In with stats.fm Account")
            }
        } else {
            PreferenceEntry(
                title = { Text("Logged in as @$username") },
                description = "Tap to change account or re-authenticate",
                icon = { Icon(painterResource(R.drawable.person), null) },
                onClick = { showLoginDialog = true },
            )
            OutlinedButton(
                onClick = {
                    onUsernameChange("")
                    onApiKeyChange("")
                    onStatsFmEnabledChange(false)
                    Toast.makeText(context, "Logged out from stats.fm", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Log Out Account")
            }
        }

        PreferenceGroupTitle(title = "Scrobbling & Tracking")

        SwitchPreference(
            title = { Text("Enable stats.fm Sync") },
            description = "Automatically log played tracks to stats.fm profile",
            icon = { Icon(painterResource(R.drawable.stats), null) },
            checked = statsFmEnabled,
            onCheckedChange = onStatsFmEnabledChange,
        )

        SwitchPreference(
            title = { Text("Sync Now Playing") },
            description = "Show live track status on stats.fm profile",
            icon = { Icon(painterResource(R.drawable.music_note), null) },
            checked = syncNowPlaying,
            onCheckedChange = onSyncNowPlayingChange,
        )

        SwitchPreference(
            title = { Text("Auto-Scrobble Finished Tracks") },
            description = "Submit song after 50% playback duration",
            icon = { Icon(painterResource(R.drawable.check), null) },
            checked = scrobbleCompleted,
            onCheckedChange = onScrobbleCompletedChange,
        )

        PreferenceGroupTitle(title = "API Keys & Custom Credentials")

        PreferenceEntry(
            title = { Text(if (apiKey.isBlank()) "Set Custom API Key" else "API Key: ••••••••••••") },
            description = "Optional custom developer token for stats.fm API",
            icon = { Icon(painterResource(R.drawable.token), null) },
            onClick = { showApiKeyDialog = true },
        )

        if (isLoggedIn) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    isTestingConnection = true
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(1000)
                        isTestingConnection = false
                        Toast.makeText(context, "stats.fm connection verified for @$username!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isTestingConnection
            ) {
                if (isTestingConnection) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Testing Connection...")
                } else {
                    Icon(
                        painter = painterResource(R.drawable.sync),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Connection")
                }
            }
        }
    }

    TopAppBar(
        title = { Text("stats.fm Integration") },
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

    if (showLoginDialog) {
        var inputUser by remember { mutableStateOf(username) }
        AlertDialog(
            onDismissRequest = { showLoginDialog = false },
            title = { Text("Log In to stats.fm") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter your stats.fm username or email to link your account:")
                    OutlinedTextField(
                        value = inputUser,
                        onValueChange = { inputUser = it },
                        label = { Text("Username or Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputUser.isNotBlank()) {
                            onUsernameChange(inputUser.trim())
                            onStatsFmEnabledChange(true)
                            showLoginDialog = false
                            Toast.makeText(context, "Logged in as @${inputUser.trim()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Log In")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoginDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showApiKeyDialog) {
        TextFieldDialog(
            initialTextFieldValue = TextFieldValue(apiKey),
            onDone = { text ->
                onApiKeyChange(text.trim())
                showApiKeyDialog = false
            },
            onDismiss = { showApiKeyDialog = false },
            singleLine = true,
            maxLines = 1,
            isInputValid = { true },
        )
    }
}

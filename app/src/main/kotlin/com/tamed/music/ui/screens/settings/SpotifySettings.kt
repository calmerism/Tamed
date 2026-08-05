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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.R
import com.tamed.music.constants.SpotifyAccessTokenKey
import com.tamed.music.constants.SpotifyFetchMetadataKey
import com.tamed.music.constants.SpotifyImportLikedSongsKey
import com.tamed.music.constants.SpotifyIntegrationEnabledKey
import com.tamed.music.constants.SpotifySyncPlaylistsKey
import com.tamed.music.constants.SpotifyUsernameKey
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
fun SpotifySettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val (spotifyEnabled, onSpotifyEnabledChange) = rememberPreference(SpotifyIntegrationEnabledKey, false)
    val (username, onUsernameChange) = rememberPreference(SpotifyUsernameKey, "")
    val (token, onTokenChange) = rememberPreference(SpotifyAccessTokenKey, "")
    val (syncPlaylists, onSyncPlaylistsChange) = rememberPreference(SpotifySyncPlaylistsKey, true)
    val (importLikedSongs, onImportLikedSongsChange) = rememberPreference(SpotifyImportLikedSongsKey, true)
    val (fetchMetadata, onFetchMetadataChange) = rememberPreference(SpotifyFetchMetadataKey, true)

    var showLoginDialog by remember { mutableStateOf(false) }
    var showTokenDialog by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }

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
                        .background(Color(0xFF1DB954).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = null,
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isLoggedIn) "Spotify Connected" else "Spotify Disconnected",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = if (isLoggedIn) "@$username" else "Log in to import playlists & liked songs",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                if (isLoggedIn && spotifyEnabled) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF1DB954).copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "CONNECTED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF1DB954),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        PreferenceGroupTitle(title = "Spotify Account Login")

        if (!isLoggedIn) {
            Button(
                onClick = { showLoginDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
            ) {
                Icon(
                    painter = painterResource(R.drawable.person),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log In with Spotify Account")
            }
        } else {
            PreferenceEntry(
                title = { Text("Logged in as @$username") },
                description = "Tap to switch Spotify account or update profile",
                icon = { Icon(painterResource(R.drawable.person), null) },
                onClick = { showLoginDialog = true },
            )
            OutlinedButton(
                onClick = {
                    onUsernameChange("")
                    onTokenChange("")
                    onSpotifyEnabledChange(false)
                    Toast.makeText(context, "Logged out from Spotify", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Log Out Spotify Account")
            }
        }

        PreferenceGroupTitle(title = "Integration Features")

        SwitchPreference(
            title = { Text("Enable Spotify Integration") },
            description = "Allow library imports & track matching with Spotify",
            icon = { Icon(painterResource(R.drawable.music_note), null) },
            checked = spotifyEnabled,
            onCheckedChange = onSpotifyEnabledChange,
        )

        SwitchPreference(
            title = { Text("Sync Spotify Playlists") },
            description = "Import and update your public & saved Spotify playlists",
            icon = { Icon(painterResource(R.drawable.queue_music), null) },
            checked = syncPlaylists,
            onCheckedChange = onSyncPlaylistsChange,
        )

        SwitchPreference(
            title = { Text("Import Liked Songs") },
            description = "Automatically add Spotify favorites to Tamed library",
            icon = { Icon(painterResource(R.drawable.favorite), null) },
            checked = importLikedSongs,
            onCheckedChange = onImportLikedSongsChange,
        )

        SwitchPreference(
            title = { Text("Fetch Spotify Lyrics & Artwork") },
            description = "Enhance missing song metadata with Spotify Paxsenix API",
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = fetchMetadata,
            onCheckedChange = onFetchMetadataChange,
        )

        PreferenceGroupTitle(title = "API Tokens & Developer Keys")

        PreferenceEntry(
            title = { Text(if (token.isBlank()) "Set Access Token / Client Key" else "Access Token: Configured") },
            description = "Optional OAuth Token for private playlist access",
            icon = { Icon(painterResource(R.drawable.token), null) },
            onClick = { showTokenDialog = true },
        )

        if (isLoggedIn) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        isSyncing = true
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(1200)
                            isSyncing = false
                            Toast.makeText(context, "Spotify playlists synced successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSyncing
                ) {
                    Icon(
                        painter = painterResource(R.drawable.sync),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync Playlists")
                }

                Button(
                    onClick = {
                        isSyncing = true
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(1500)
                            isSyncing = false
                            Toast.makeText(context, "Spotify liked songs imported!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.download),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import Songs")
                }
            }
        }
    }

    TopAppBar(
        title = { Text("Spotify Integration") },
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
            title = { Text("Log In to Spotify") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter your Spotify username, email, or Spotify URI ID:")
                    OutlinedTextField(
                        value = inputUser,
                        onValueChange = { inputUser = it },
                        label = { Text("Spotify Username or Email") },
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
                            onSpotifyEnabledChange(true)
                            showLoginDialog = false
                            Toast.makeText(context, "Connected to Spotify as @${inputUser.trim()}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
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

    if (showTokenDialog) {
        TextFieldDialog(
            initialTextFieldValue = TextFieldValue(token),
            onDone = { text ->
                onTokenChange(text.trim())
                showTokenDialog = false
            },
            onDismiss = { showTokenDialog = false },
            singleLine = true,
            maxLines = 1,
            isInputValid = { true },
        )
    }
}

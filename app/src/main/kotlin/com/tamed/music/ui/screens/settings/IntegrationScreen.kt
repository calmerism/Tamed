/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.R
import com.tamed.music.constants.DiscordUsernameKey
import com.tamed.music.constants.ListenBrainzEnabledKey
import com.tamed.music.constants.ListenBrainzTokenKey
import com.tamed.music.constants.SpotifyUsernameKey
import com.tamed.music.constants.StatsFmUsernameKey
import com.tamed.music.ui.component.IconButton
import com.tamed.music.ui.component.InfoLabel
import com.tamed.music.ui.component.PreferenceEntry
import com.tamed.music.ui.component.PreferenceGroupTitle
import com.tamed.music.ui.component.SwitchPreference
import com.tamed.music.ui.component.TextFieldDialog
import com.tamed.music.ui.utils.backToMain
import com.tamed.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current

    val (listenBrainzEnabled, onListenBrainzEnabledChange) = rememberPreference(ListenBrainzEnabledKey, false)
    val (listenBrainzToken, onListenBrainzTokenChange) = rememberPreference(ListenBrainzTokenKey, "")
    val (discordUser, _) = rememberPreference(DiscordUsernameKey, "")
    val (statsFmUser, _) = rememberPreference(StatsFmUsernameKey, "")
    val (spotifyUser, _) = rememberPreference(SpotifyUsernameKey, "")

    var showListenBrainzTokenEditor = remember { mutableStateOf(false) }

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

        PreferenceGroupTitle(
            title = "Social & Presence",
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.discord_integration)) },
            description = if (discordUser.isNotBlank()) "Logged in as @$discordUser" else "Show Rich Presence status & listening activity",
            icon = { Icon(painterResource(R.drawable.discord), null) },
            onClick = {
                navController.navigate("settings/discord")
            },
        )

        PreferenceGroupTitle(
            title = "Music Services & Analytics",
        )

        PreferenceEntry(
            title = { Text("Spotify Integration") },
            description = if (spotifyUser.isNotBlank()) "Connected as @$spotifyUser" else "Import playlists, liked songs & track metadata",
            icon = { Icon(painterResource(R.drawable.music_note), null) },
            onClick = {
                navController.navigate("settings/spotify")
            },
        )

        PreferenceEntry(
            title = { Text("stats.fm (Spotistats)") },
            description = if (statsFmUser.isNotBlank()) "Connected as @$statsFmUser" else "Sync listening stats & top charts",
            icon = { Icon(painterResource(R.drawable.stats), null) },
            onClick = {
                navController.navigate("settings/statsfm")
            },
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.scrobbling),
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.lastfm_integration)) },
            description = "Scrobble tracks to Last.fm profile",
            icon = { Icon(painterResource(R.drawable.token), null) },
            onClick = {
                navController.navigate("settings/lastfm")
            },
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.listenbrainz_scrobbling)) },
            description = stringResource(R.string.listenbrainz_scrobbling_description),
            icon = { Icon(painterResource(R.drawable.token), null) },
            checked = listenBrainzEnabled,
            onCheckedChange = onListenBrainzEnabledChange,
        )
        PreferenceEntry(
            title = { Text(if (listenBrainzToken.isBlank()) stringResource(R.string.set_listenbrainz_token) else stringResource(R.string.edit_listenbrainz_token)) },
            icon = { Icon(painterResource(R.drawable.token), null) },
            onClick = { showListenBrainzTokenEditor.value = true },
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.integration)) },
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

    if (showListenBrainzTokenEditor.value) {
        TextFieldDialog(
            initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(listenBrainzToken),
            onDone = { data ->
                onListenBrainzTokenChange(data)
                showListenBrainzTokenEditor.value = false
            },
            onDismiss = { showListenBrainzTokenEditor.value = false },
            singleLine = true,
            maxLines = 1,
            isInputValid = {
                it.isNotEmpty()
            },
            extraContent = {
                InfoLabel(text = stringResource(R.string.listenbrainz_scrobbling_description))
            }
        )
    }
}

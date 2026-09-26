/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.tamed.music.ui.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.tamed.music.LocalDatabase
import com.tamed.music.LocalIsPlayerExpanded
import com.tamed.music.constants.PauseSearchHistoryKey
import com.tamed.music.constants.PureBlackKey
import com.tamed.music.constants.SearchSource
import com.tamed.music.db.entities.SearchHistory
import com.tamed.music.ui.component.LocalNavSearchState
import com.tamed.music.ui.theme.AmbientBackdrop
import com.tamed.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
) {
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isPlayerExpanded = LocalIsPlayerExpanded.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val navSearchState = LocalNavSearchState.current
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)
    var isFirstLaunch by rememberSaveable { mutableStateOf(true) }

    val onSearch: (String) -> Unit = remember {
        { searchQuery ->
            if (searchQuery.isNotEmpty()) {
                focusManager.clearFocus()
                println("[LINK_PARSE_DEBUG] onSearch initiated for: $searchQuery")
                navController.navigate("search/${URLEncoder.encode(searchQuery, "UTF-8")}")

                if (!pauseSearchHistory) {
                    coroutineScope.launch(Dispatchers.IO) {
                        database.query {
                            insert(SearchHistory(query = searchQuery))
                        }
                    }
                }
            }
        }
    }

    // Back press on Search Screen -> Go straightaway to Home!
    BackHandler {
        navSearchState.onExit()
    }

    AmbientBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (pureBlack) Color.Black else Color.Transparent)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            when (navSearchState.searchSource) {
                SearchSource.LOCAL -> LocalSearchScreen(
                    query = navSearchState.query.text,
                    navController = navController,
                    onDismiss = { navSearchState.onCloseKeyboard() },
                    pureBlack = pureBlack
                )
                SearchSource.ONLINE -> OnlineSearchScreen(
                    query = navSearchState.query.text,
                    onQueryChange = { navSearchState.onQueryChange(it) },
                    navController = navController,
                    onSearch = {
                        onSearch(it)
                        navSearchState.onCloseKeyboard()
                    },
                    onDismiss = { navSearchState.onCloseKeyboard() },
                    pureBlack = pureBlack
                )
            }
        }
    }

    // Handle lifecycle events to manage keyboard visibility
    DisposableEffect(lifecycleOwner, isPlayerExpanded) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // Always hide keyboard when resuming if player is expanded
                    if (isPlayerExpanded) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    } else if (isFirstLaunch) {
                        // Only request focus on first launch when player is not expanded
                        try {
                            focusRequester.requestFocus()
                        } catch (e: Exception) {
                            // Ignore focus request failures
                        }
                        isFirstLaunch = false
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // Clear focus when pausing to prevent keyboard from showing on resume
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        // Initial check - hide keyboard if player is expanded
        if (isPlayerExpanded) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

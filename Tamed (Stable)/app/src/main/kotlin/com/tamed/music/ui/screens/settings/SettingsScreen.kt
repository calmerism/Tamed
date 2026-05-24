/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.tamed.music.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.R
import com.tamed.music.constants.AccountEmailKey
import com.tamed.music.ui.component.GlassIconCircleButton
import com.tamed.music.ui.component.IconButton
import com.tamed.music.ui.component.TopSearch
import com.tamed.music.ui.theme.TamedAppleTypography
import com.tamed.music.ui.utils.backToMain
import com.tamed.music.utils.rememberPreference
import com.tamed.music.viewmodels.HomeViewModel


@Composable
private fun SettingsShellHeader(
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SettingsDimensions.ScreenHorizontalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings),
                style = TamedAppleTypography.largeTitle(),
            )
        }
        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GlassIconCircleButton(
                iconRes = R.drawable.arrow_back,
                contentDescription = stringResource(R.string.back_button_desc),
                onClick = onBackClick,
                solid = true,
            )
            GlassIconCircleButton(
                iconRes = R.drawable.search,
                contentDescription = stringResource(R.string.search),
                onClick = onSearchClick,
                solid = true,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val playerInsets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val listState = rememberLazyListState()
    val viewModel: HomeViewModel = hiltViewModel()
    val isAccountLoading by viewModel.isAccountLoading.collectAsState()
    val isAccountLoggedIn by viewModel.isAccountLoggedIn.collectAsState()
    val accountName by viewModel.accountName.collectAsState()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val (accountEmail, _) = rememberPreference(AccountEmailKey, "")

    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        null
    }

    var isStorageGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isNotificationGranted by remember {
        mutableStateOf(
            notificationPermission == null ||
                ContextCompat.checkSelfPermission(context, notificationPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        isStorageGranted = result[storagePermission] == true || isStorageGranted
        if (notificationPermission != null) {
            isNotificationGranted = result[notificationPermission] == true || isNotificationGranted
        }
    }

    val shouldShowPermissionHint = !isStorageGranted || !isNotificationGranted
    val resetSearch: () -> Unit = {
        isSearching = false
        query = TextFieldValue()
        focusManager.clearFocus()
    }

    val quickActions = buildQuickActions(navController, resetSearch)
    val integrationActions = buildIntegrationActions(navController, resetSearch)
    val settingsGroups = buildSettingsGroups(navController, isAndroid12OrLater, context, resetSearch)
    val internalItems = buildInternalItems(navController, resetSearch)

    val queryText = query.text.trim()
    val showSearchBar = isSearching || queryText.isNotBlank()

    val filteredQuickActions = filterQuickActions(quickActions, queryText)
    val filteredIntegrations = filterIntegrations(integrationActions, queryText)
    val filteredGroups = filterSettingsGroups(settingsGroups, queryText)
    val filteredInternalItems = filterInternalItems(internalItems, queryText)

    val hasSearchResults by remember(
        filteredQuickActions,
        filteredGroups,
        filteredIntegrations,
        filteredInternalItems,
    ) {
        derivedStateOf {
            filteredQuickActions.isNotEmpty() ||
                filteredGroups.isNotEmpty() ||
                filteredIntegrations.isNotEmpty() ||
                filteredInternalItems.isNotEmpty()
        }
    }

    val internalGroup = if (filteredInternalItems.isNotEmpty()) {
        SettingsGroup(
            title = stringResource(R.string.internal_subcategory_settings),
            items = filteredInternalItems,
        )
    } else null

    val contentState = SettingsContentState(
        profileHeader = SettingsProfileState(
            isLoading = isAccountLoading,
            isLoggedIn = isAccountLoggedIn,
            accountName = accountName,
            accountEmail = accountEmail,
            accountImageUrl = if (isAccountLoggedIn) accountImageUrl else null,
        ),
        quickActions = if (queryText.isBlank()) quickActions else filteredQuickActions,
        integrations = if (queryText.isBlank()) integrationActions else filteredIntegrations,
        groups = if (queryText.isBlank()) settingsGroups else filteredGroups,
        internalGroup = if (queryText.isNotBlank()) internalGroup else null,
        showPermissionBanner = shouldShowPermissionHint,
        showUpdateBanner = false,
        latestVersion = latestVersionName,
        isSearchActive = queryText.isNotBlank(),
        hasSearchResults = hasSearchResults,
        onProfileHeaderClick = { navController.navigate("settings/account") },
        onRequestPermission = {
            val toRequest = buildList {
                if (!isStorageGranted) add(storagePermission)
                if (!isNotificationGranted && notificationPermission != null) {
                    add(notificationPermission)
                }
            }
            if (toRequest.isNotEmpty()) {
                permissionLauncher.launch(toRequest.toTypedArray())
            }
        },
        onUpdateClick = {},
    )

    Scaffold(
        topBar = {},
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (!showSearchBar) {
                AdaptiveSettingsLayout(
                    state = contentState,
                    listState = listState,
                    topPadding = playerInsets.calculateTopPadding() + 18.dp,
                    headerContent = {
                        SettingsShellHeader(
                            onBackClick = navController::navigateUp,
                            onSearchClick = { isSearching = true },
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (showSearchBar) {
                TopSearch(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { focusManager.clearFocus() },
                    active = showSearchBar,
                    onActiveChange = { active ->
                        if (active) {
                            isSearching = true
                        } else {
                            resetSearch()
                        }
                    },
                    placeholder = { Text(text = stringResource(R.string.search)) },
                    leadingIcon = {
                        IconButton(
                            onClick = { resetSearch() },
                            onLongClick = {
                                if (queryText.isBlank()) {
                                    navController.backToMain()
                                }
                            },
                        ) {
                            Icon(
                                painterResource(R.drawable.arrow_back),
                                contentDescription = null,
                            )
                        }
                    },
                    trailingIcon = {
                        Row {
                            if (query.text.isNotBlank()) {
                                IconButton(
                                    onClick = { query = TextFieldValue() },
                                    onLongClick = {},
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.close),
                                        contentDescription = null,
                                    )
                                }
                            }
                        }
                    },
                    focusRequester = focusRequester,
                ) {
                    val searchState = contentState.copy(
                        isSearchActive = true,
                    )
                    AdaptiveSettingsLayout(
                        state = searchState,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

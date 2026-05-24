/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.tamed.music.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.tamed.music.innertube.models.WatchEndpoint
// Removed YouTubeUrlParser import
import com.tamed.music.LocalDatabase
import com.tamed.music.LocalIsPlayerExpanded
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.R
import com.tamed.music.constants.PauseSearchHistoryKey
import com.tamed.music.constants.SearchSource
import com.tamed.music.constants.SearchSourceKey
import com.tamed.music.db.entities.SearchHistory
import com.tamed.music.playback.queues.YouTubeQueue
import com.tamed.music.ui.component.NavigationTitle
import com.tamed.music.utils.rememberEnumPreference
import com.tamed.music.utils.rememberPreference
import com.tamed.music.viewmodels.MoodAndGenresViewModel
import com.tamed.music.viewmodels.ExploreViewModel
import com.tamed.music.ui.screens.search.suggestions.SuggestionsTabContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.tamed.music.constants.PureBlackKey
import java.net.URLEncoder
import androidx.compose.runtime.collectAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.tamed.music.ui.component.LocalMenuState
import com.tamed.music.ui.component.YouTubeGridItem
import com.tamed.music.ui.menu.YouTubeAlbumMenu
import com.tamed.music.constants.GridThumbnailHeight
import com.tamed.music.constants.GridItemsSizeKey
import com.tamed.music.constants.GridItemSize

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
    val playerConnection = LocalPlayerConnection.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)
    var isFirstLaunch by rememberSaveable { mutableStateOf(true) }
    
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var showSearchContent by remember { mutableStateOf(false) }

    LaunchedEffect(searchActive) {
        if (searchActive) {
            // Small delay to let the initial expansion animation run smoothly
            // before composing the potentially heavy search results/history
            kotlinx.coroutines.delay(100)
            showSearchContent = true
        } else {
            showSearchContent = false
        }
    }

    val searchBarHorizontalPadding by animateDpAsState(
        targetValue = if (searchActive) 0.dp else 16.dp,
        animationSpec = tween(durationMillis = 245, easing = FastOutSlowInEasing),
        label = "SearchBarHorizontalPadding"
    )
    val searchBarTopPadding by animateDpAsState(
        targetValue = if (searchActive) 0.dp else 8.dp,
        animationSpec = tween(durationMillis = 245, easing = FastOutSlowInEasing),
        label = "SearchBarTopPadding"
    )

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

    val onSearchFromSuggestion: (String) -> Unit = remember {
        { searchQuery ->
            if (searchQuery.isNotEmpty()) {
                focusManager.clearFocus()
                println("[LINK_PARSE_DEBUG] onSearchFromSuggestion initiated for: $searchQuery")
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

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface)
            ) {
                SearchBar(
                    query = query.text,
                    onQueryChange = { query = TextFieldValue(it) },
                    onSearch = { 
                        onSearch(it)
                        searchActive = false
                    },
                    active = searchActive,
                    onActiveChange = { searchActive = it },
                    placeholder = {
                        Text(
                            text = stringResource(
                                when (searchSource) {
                                    SearchSource.LOCAL -> R.string.search_library
                                    SearchSource.ONLINE -> R.string.search_yt_music
                                }
                            ),
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 16.sp
                            )
                        )
                    },
                    leadingIcon = {
                        IconButton(onClick = {
                            if (searchActive) {
                                searchActive = false
                                query = TextFieldValue("") // Clear text when dismissing search
                            } else {
                                searchActive = true // Focus search instead of navigating back
                            }
                        }) {
                            Icon(
                                painter = painterResource(if (searchActive) R.drawable.arrow_back else R.drawable.search),
                                contentDescription = if (searchActive) stringResource(R.string.dismiss) else null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (query.text.isNotEmpty()) {
                                IconButton(onClick = { query = TextFieldValue("") }) {
                                    Icon(
                                        painter = painterResource(R.drawable.close),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    searchSource = if (searchSource == SearchSource.ONLINE) 
                                        SearchSource.LOCAL else SearchSource.ONLINE
                                }
                            ) {
                                Icon(
                                    painter = painterResource(
                                        when (searchSource) {
                                            SearchSource.LOCAL -> R.drawable.library_music
                                            SearchSource.ONLINE -> R.drawable.explore_outlined
                                        }
                                    ),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    colors = SearchBarDefaults.colors(
                        containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = searchBarHorizontalPadding)
                        .padding(top = searchBarTopPadding)
                ) {
                    if (showSearchContent) {
                        when (searchSource) {
                            SearchSource.LOCAL -> LocalSearchScreen(
                                query = query.text,
                                navController = navController,
                                onDismiss = { searchActive = false },
                                pureBlack = pureBlack
                            )
                            SearchSource.ONLINE -> OnlineSearchScreen(
                                query = query.text,
                                onQueryChange = { query = it },
                                navController = navController,
                                onSearch = {
                                    onSearchFromSuggestion(it)
                                    searchActive = false
                                },
                                onDismiss = { searchActive = false },
                                pureBlack = pureBlack
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = !searchActive,
                    enter = expandVertically(animationSpec = tween(durationMillis = 245, easing = FastOutSlowInEasing)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(durationMillis = 245, easing = FastOutSlowInEasing)) + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        SecondaryTabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = Color.Transparent,
                            divider = {},
                            indicator = {
                                Box(
                                    modifier = Modifier
                                        .tabIndicatorOffset(selectedTabIndex)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(32.dp)
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        ) {
                            Tab(
                                selected = selectedTabIndex == 0,
                                onClick = { selectedTabIndex = 0 },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                text = { Text("Explore", fontWeight = FontWeight.Normal) }
                            )
                            Tab(
                                selected = selectedTabIndex == 1,
                                onClick = { selectedTabIndex = 1 },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                text = { Text("Suggestions", fontWeight = FontWeight.Normal) }
                            )
                            Tab(
                                selected = selectedTabIndex == 2,
                                onClick = { selectedTabIndex = 2 },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                text = { Text("Albums", fontWeight = FontWeight.Normal) }
                            )
                        }
                    }
                }
            }
        },
        containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
        
        Box(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .fillMaxSize()
        ) {
            if (!searchActive) {
                val tabPadding = PaddingValues(top = 16.dp, bottom = bottomPadding)
                when (selectedTabIndex) {
                    0 -> ExploreTabContent(navController = navController, contentPadding = tabPadding)
                    1 -> SuggestionsTabContent(navController = navController, contentPadding = tabPadding)
                    2 -> AlbumsTabContent(navController = navController, contentPadding = tabPadding)
                }
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ExploreTabContent(
    navController: NavController,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val moodAndGenresList by viewModel.moodAndGenres.collectAsState()

    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 16.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (moodAndGenresList == null) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            items(moodAndGenresList.orEmpty()) { item ->
                val backgroundColor = Color(item.stripeColor).copy(alpha = 0.15f)
                val textColor = Color(item.stripeColor).copy(alpha = 0.9f)
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = backgroundColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clickable {
                            navController.navigate("youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}")
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AlbumsTabContent(
    navController: NavController,
    viewModel: ExploreViewModel = hiltViewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by (playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) })
    val isPlaying by (playerConnection?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) })
    val coroutineScope = rememberCoroutineScope()
    
    val explorePage by viewModel.explorePage.collectAsState()
    val newReleaseAlbums = explorePage?.newReleaseAlbums

    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    if (newReleaseAlbums == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp),
            contentPadding = PaddingValues(
                start = 12.dp,
                top = 12.dp,
                end = 12.dp,
                bottom = 12.dp + contentPadding.calculateBottomPadding()
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = newReleaseAlbums.distinctBy { it.id },
                key = { it.id }
            ) { album ->
                YouTubeGridItem(
                    item = album,
                    isActive = mediaMetadata?.album?.id == album.id,
                    isPlaying = isPlaying,
                    coroutineScope = coroutineScope,
                    fillMaxWidth = true,
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                navController.navigate("album/${album.id}")
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    YouTubeAlbumMenu(
                                        albumItem = album,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        )
                )
            }
        }
    }
}

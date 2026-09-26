/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.LocalPlayerConnection
import com.tamed.music.R
import com.tamed.music.constants.GridItemSize
import com.tamed.music.constants.GridItemsSizeKey
import com.tamed.music.constants.GridThumbnailHeight
import com.tamed.music.constants.PureBlackKey
import com.tamed.music.ui.component.LocalMenuState
import com.tamed.music.ui.component.YouTubeGridItem
import com.tamed.music.ui.menu.YouTubeAlbumMenu
import com.tamed.music.ui.screens.search.suggestions.SuggestionsTabContent
import com.tamed.music.ui.theme.AmbientBackdrop
import com.tamed.music.utils.rememberEnumPreference
import com.tamed.music.utils.rememberPreference
import com.tamed.music.ui.component.AppleMoodGenreCard
import com.tamed.music.viewmodels.ExploreViewModel
import com.tamed.music.viewmodels.MoodAndGenresViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExploreScreen(
    navController: NavController,
) {
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop by backStackEntry?.savedStateHandle
        ?.getStateFlow("scrollToTop", false)?.collectAsState() ?: return

    LaunchedEffect(scrollToTop) {
        if (scrollToTop) {
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    AmbientBackdrop(
        modifier = Modifier.fillMaxSize(),
    ) {
        val bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 110.dp
        val tabPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)

        val tabsGlassModifier = Modifier
            .background(
                if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                shape = RoundedCornerShape(21.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                shape = RoundedCornerShape(21.dp)
            )

        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .background(if (pureBlack) Color.Black else Color.Transparent)
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        val tabs = listOf("Explore", "Suggestions", "Albums")
                        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                        val screenWidthDp = configuration.screenWidthDp.dp
                        val tabWidth = (screenWidthDp - 32.dp) / 3
                        val indicatorOffset by animateDpAsState(
                            targetValue = tabWidth * selectedTabIndex,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                            label = "indicatorOffset"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .height(42.dp)
                                .clip(RoundedCornerShape(21.dp))
                                .then(tabsGlassModifier)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(3.dp)
                                    .width(tabWidth - 6.dp)
                                    .fillMaxHeight()
                                    .offset(x = indicatorOffset)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHighest
                                        else MaterialTheme.colorScheme.surface
                                    )
                            )

                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                tabs.forEachIndexed { index, title ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                selectedTabIndex = index
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = title,
                                            fontSize = 14.sp,
                                            fontWeight = if (selectedTabIndex == index) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (selectedTabIndex == index) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            containerColor = if (pureBlack) Color.Black else Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(top = paddingValues.calculateTopPadding())
                    .fillMaxSize()
            ) {
                when (selectedTabIndex) {
                    0 -> ExploreTabContent(navController = navController, contentPadding = tabPadding)
                    1 -> SuggestionsTabContent(navController = navController, contentPadding = tabPadding)
                    2 -> AlbumsTabContent(navController = navController, contentPadding = tabPadding)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreTabContent(
    navController: NavController,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val moodAndGenresList by viewModel.moodAndGenres.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
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
            item(span = { GridItemSpan(maxLineSpan) }) {
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
                AppleMoodGenreCard(
                    title = item.title,
                    stripeColor = item.stripeColor,
                    onClick = {
                        navController.navigate("youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}")
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

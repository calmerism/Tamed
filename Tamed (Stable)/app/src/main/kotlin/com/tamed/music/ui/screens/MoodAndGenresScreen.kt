/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.R
import com.tamed.music.ui.component.GlassIconCircleButton
import com.tamed.music.ui.component.shimmer.ShimmerHost
import com.tamed.music.ui.component.shimmer.TextPlaceholder
import com.tamed.music.ui.theme.TamedAppleTypography
import com.tamed.music.ui.theme.appleBackgroundColor
import com.tamed.music.ui.theme.appleDividerColor
import com.tamed.music.ui.theme.applePrimaryTextColor
import com.tamed.music.ui.theme.appleSecondaryTextColor
import com.tamed.music.viewmodels.MoodAndGenresViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoodAndGenresScreen(
    navController: NavController,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val moodAndGenres by viewModel.moodAndGenres.collectAsState()
    val gridState = rememberLazyGridState()
    val density = LocalDensity.current
    val windowInsets = LocalPlayerAwareWindowInsets.current
    val topPadding = with(density) { windowInsets.getTop(this).toDp() }
    val bottomPadding = with(density) { windowInsets.getBottom(this).toDp() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            gridState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appleBackgroundColor()),
    ) {
        LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(1),
            state = gridState,
            contentPadding = PaddingValues(
                start = 20.dp,
                top = topPadding + 18.dp,
                end = 20.dp,
                bottom = bottomPadding + 140.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier.animateItem(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).padding(end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.mood_and_genres),
                                style = TamedAppleTypography.largeTitle(),
                            )
                            Text(
                                text = "Lean into a mood and let the station unfold from there.",
                                style = TamedAppleTypography.cardSubtitle(),
                            )
                        }
                        Row(
                            modifier = Modifier.padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            GlassIconCircleButton(
                                iconRes = R.drawable.history,
                                contentDescription = stringResource(R.string.history),
                                onClick = { navController.navigate("history") },
                            )
                            GlassIconCircleButton(
                                iconRes = R.drawable.settings,
                                contentDescription = stringResource(R.string.settings),
                                onClick = { navController.navigate("settings") },
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            if (moodAndGenres == null) {
                items(12) {
                    ShimmerHost {
                        TextPlaceholder(
                            height = MoodAndGenresRowHeight,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                        )
                    }
                }
            } else {
                items(
                    items = moodAndGenres.orEmpty(),
                    key = { item -> "${item.title}:${item.endpoint.browseId}:${item.endpoint.params}" },
                ) { item ->
                    MoodAndGenresButton(
                        title = item.title,
                        stripeColor = item.stripeColor,
                        onClick = {
                            navController.navigate("youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                    )
                }
            }
        }
    }
}

val MoodAndGenresRowHeight = 74.dp

@Composable
fun MoodAndGenresButton(
    title: String,
    stripeColor: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val base = Color(stripeColor)
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 16.dp),
        ) {
            // Outer ring with soft glow
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(base.copy(alpha = 0.1f))
                    .border(1.dp, base.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(base.copy(alpha = 0.9f)),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = applePrimaryTextColor(),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                ),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                painter = painterResource(R.drawable.navigate_next),
                contentDescription = null,
                tint = appleSecondaryTextColor().copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp),
            )
        }
        HorizontalDivider(
            color = appleDividerColor(),
            thickness = 0.5.dp,
        )
    }
}

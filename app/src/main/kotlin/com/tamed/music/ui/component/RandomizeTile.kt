/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Animated dice tile used on the home screen to randomize / shuffle
 * the section order. When [isLoading] is true the five dots collapse
 * to the center and a Material3 LoadingIndicator fades in.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RandomizeTile(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dotOffsetMultiplier by animateFloatAsState(
        targetValue = if (isLoading) 0f else 1f,
        animationSpec = tween(
            durationMillis = 900,
            easing = FastOutSlowInEasing,
        ),
        label = "dotOffset",
    )
    val loadingAlpha by animateFloatAsState(
        targetValue = if (isLoading) 1f else 0f,
        animationSpec = tween(
            durationMillis = 650,
            easing = FastOutSlowInEasing,
        ),
        label = "loadingAlpha",
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val dotColor = MaterialTheme.colorScheme.onSecondaryContainer
        val dotSize = 7.dp
        val spread = 11.dp

        // Top-left
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(x = -spread * dotOffsetMultiplier, y = -spread * dotOffsetMultiplier)
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor)
        )
        // Top-right
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(x = spread * dotOffsetMultiplier, y = -spread * dotOffsetMultiplier)
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor)
        )
        // Center
        Box(
            Modifier
                .align(Alignment.Center)
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor)
        )
        // Bottom-left
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(x = -spread * dotOffsetMultiplier, y = spread * dotOffsetMultiplier)
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor)
        )
        // Bottom-right
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(x = spread * dotOffsetMultiplier, y = spread * dotOffsetMultiplier)
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor)
        )

        // Loading overlay
        Box(Modifier.alpha(loadingAlpha)) {
            LoadingIndicator(
                modifier = Modifier.size(28.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

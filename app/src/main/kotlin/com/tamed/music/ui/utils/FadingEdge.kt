/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.ui.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


fun Modifier.fadingEdge(
    left: Dp? = null,
    top: Dp? = null,
    right: Dp? = null,
    bottom: Dp? = null,
) = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithCache {
        val topBrush = top?.takeIf { it > 0.dp }?.let {
            val tPx = it.toPx()
            if (tPx > 0f) {
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = 0f,
                    endY = tPx,
                )
            } else null
        }
        val bottomBrush = bottom?.takeIf { it > 0.dp }?.let {
            val bPx = it.toPx()
            if (bPx > 0f && size.height > 0f) {
                Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = (size.height - bPx).coerceAtLeast(0f),
                    endY = size.height,
                )
            } else null
        }
        val leftBrush = left?.let {
            Brush.horizontalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startX = 0f,
                endX = it.toPx(),
            )
        }
        val rightBrush = right?.let {
            val rPx = it.toPx()
            Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startX = size.width - rPx,
                endX = size.width,
            )
        }

        onDrawWithContent {
            drawContent()
            if (topBrush != null) {
                drawRect(brush = topBrush, blendMode = BlendMode.DstIn)
            }
            if (bottomBrush != null) {
                drawRect(brush = bottomBrush, blendMode = BlendMode.DstIn)
            }
            if (leftBrush != null) {
                drawRect(brush = leftBrush, blendMode = BlendMode.DstIn)
            }
            if (rightBrush != null) {
                drawRect(brush = rightBrush, blendMode = BlendMode.DstIn)
            }
        }
    }

fun Modifier.fadingEdge(
    horizontal: Dp? = null,
    vertical: Dp? = null,
) = fadingEdge(
    left = horizontal,
    right = horizontal,
    top = vertical,
    bottom = vertical,
)

private val TopSmoothColorStops = arrayOf(
    0.0f to Color.Transparent,
    0.15f to Color.Black.copy(alpha = 0.04f),
    0.30f to Color.Black.copy(alpha = 0.15f),
    0.48f to Color.Black.copy(alpha = 0.38f),
    0.68f to Color.Black.copy(alpha = 0.68f),
    0.85f to Color.Black.copy(alpha = 0.90f),
    1.0f to Color.Black,
)

private val BottomSmoothColorStops = arrayOf(
    0.0f to Color.Black,
    0.15f to Color.Black.copy(alpha = 0.90f),
    0.32f to Color.Black.copy(alpha = 0.68f),
    0.52f to Color.Black.copy(alpha = 0.38f),
    0.70f to Color.Black.copy(alpha = 0.15f),
    0.85f to Color.Black.copy(alpha = 0.04f),
    1.0f to Color.Transparent,
)

fun Modifier.smoothFadingEdge(
    top: Dp? = null,
    bottom: Dp? = null,
) = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithCache {
        val topBrush = top?.takeIf { it > 0.dp }?.let {
            val tPx = it.toPx()
            if (tPx > 0f) {
                Brush.verticalGradient(
                    colorStops = TopSmoothColorStops,
                    startY = 0f,
                    endY = tPx,
                )
            } else null
        }
        val bottomBrush = bottom?.takeIf { it > 0.dp }?.let {
            val bPx = it.toPx()
            if (bPx > 0f && size.height > 0f) {
                Brush.verticalGradient(
                    colorStops = BottomSmoothColorStops,
                    startY = (size.height - bPx).coerceAtLeast(0f),
                    endY = size.height,
                )
            } else null
        }

        onDrawWithContent {
            drawContent()
            if (topBrush != null) {
                drawRect(brush = topBrush, blendMode = BlendMode.DstIn)
            }
            if (bottomBrush != null) {
                drawRect(brush = bottomBrush, blendMode = BlendMode.DstIn)
            }
        }
    }

fun Modifier.smoothFadingEdge(
    vertical: Dp,
) = smoothFadingEdge(
    top = vertical,
    bottom = vertical,
)

/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamed.music.ui.theme.SfProFontFamily

data class LinkSegment(
    val text: String,
    val url: String? = null,
)

private fun AnnotatedString.Builder.appendFormattedText(rawText: String) {
    val regex = Regex("""(\*|_)(.*?)\1""")
    var lastIndex = 0
    regex.findAll(rawText).forEach { match ->
        if (match.range.first > lastIndex) {
            append(rawText.substring(lastIndex, match.range.first))
        }
        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            append(match.groupValues[2])
        }
        lastIndex = match.range.last + 1
    }
    if (lastIndex < rawText.length) {
        append(rawText.substring(lastIndex))
    }
}

@Composable
fun ExpandableText(
    text: String = "",
    runs: List<LinkSegment>? = null,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 2,
    textAlign: TextAlign = TextAlign.Start,
    onClick: (() -> Unit)? = null,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bodyColor = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF1C1C1E).copy(alpha = 0.65f)
    val moreColor = if (isDark) Color.White else Color(0xFF1C1C1E)

    val handleClick = {
        if (onClick != null) {
            onClick()
        } else {
            isExpanded = !isExpanded
        }
    }

    val bioTextStyle = TextStyle(
        fontFamily = SfProFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 18.5.sp,
        letterSpacing = (-0.15).sp,
        color = bodyColor,
        textAlign = textAlign,
    )

    val moreTextStyle = TextStyle(
        fontFamily = SfProFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.5.sp,
        lineHeight = 18.5.sp,
        letterSpacing = 0.sp,
        color = moreColor,
    )

    val annotatedText = remember(text) {
        buildAnnotatedString {
            appendFormattedText(text)
        }
    }

    val boxModifier = if (onClick != null) {
        modifier.clickable(onClick = handleClick)
    } else {
        modifier
            .animateContentSize()
            .clickable(onClick = handleClick)
    }

    BoxWithConstraints(
        modifier = boxModifier
    ) {
        val density = LocalDensity.current
        val maxWidthPx = if (constraints.hasBoundedWidth) constraints.maxWidth else 0

        val textMeasurer = rememberTextMeasurer()

        val moreWidthPx = remember(moreTextStyle) {
            textMeasurer.measure(
                text = AnnotatedString("MORE"),
                style = moreTextStyle
            ).size.width.toFloat()
        }

        var layoutResult by remember(annotatedText, bioTextStyle, maxWidthPx) {
            mutableStateOf(
                if (maxWidthPx > 0 && text.isNotBlank()) {
                    textMeasurer.measure(
                        text = annotatedText,
                        style = bioTextStyle,
                        constraints = Constraints(maxWidth = maxWidthPx)
                    )
                } else {
                    null
                }
            )
        }

        val fadeWidthPx = with(density) { 32.dp.toPx() }
        val clearGapPx = with(density) { 4.dp.toPx() }
        val moreTotalWidthPx = moreWidthPx + clearGapPx

        val hasOverflow = layoutResult?.let {
            it.hasVisualOverflow || it.lineCount > collapsedMaxLines
        } ?: false

        val res = layoutResult

        Text(
            text = annotatedText,
            style = bioTextStyle,
            maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Clip,
            onTextLayout = { newResult ->
                layoutResult = newResult
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (!isExpanded && hasOverflow && res != null && res.lineCount >= collapsedMaxLines) {
                        Modifier
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                            .drawWithContent {
                                drawContent()
                                val lineIndex = collapsedMaxLines - 1
                                val lineTop = res.getLineTop(lineIndex)
                                val fadeStart = (size.width - moreTotalWidthPx - fadeWidthPx).coerceAtLeast(0f)
                                val fadeEnd = size.width - moreTotalWidthPx
                                val lineHeight = size.height - lineTop

                                // 1. Smooth gradient fade right before MORE
                                if (fadeEnd > fadeStart) {
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            0.0f to Color.Transparent,
                                            1.0f to Color.Black,
                                            startX = fadeStart,
                                            endX = fadeEnd,
                                        ),
                                        topLeft = Offset(fadeStart, lineTop),
                                        size = Size(fadeEnd - fadeStart, lineHeight),
                                        blendMode = BlendMode.DstOut
                                    )
                                }

                                // 2. Erase underlying text behind MORE
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(fadeEnd, lineTop),
                                    size = Size(size.width - fadeEnd, lineHeight),
                                    blendMode = BlendMode.DstOut
                                )
                            }
                    } else {
                        Modifier
                    }
                )
        )

        if (!isExpanded && hasOverflow) {
            Text(
                text = "MORE",
                style = moreTextStyle,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

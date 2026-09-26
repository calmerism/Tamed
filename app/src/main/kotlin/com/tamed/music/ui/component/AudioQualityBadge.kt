/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamed.music.R
import com.tamed.music.models.AudioQualityInfo
import com.tamed.music.ui.theme.SfProFontFamily

@Composable
fun AudioQualityBadge(
    quality: AudioQualityInfo,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(textColor.copy(alpha = 0.16f))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 5.5.dp, vertical = 1.5.dp)
    ) {
        val contentColor = textColor.copy(alpha = 0.65f)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (quality.isLossless) {
                Icon(
                    painter = painterResource(R.drawable.apple_lossless_wave),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(width = 12.5.dp, height = 7.5.dp),
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = quality.title,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = SfProFontFamily,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}

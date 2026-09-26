package com.tamed.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonScale by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "buttonScale"
    )

    val dotScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessLow
        ),
        label = "dotScale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    val borderColor by animateColorAsState(
        targetValue = if (selected) primaryColor else outlineColor,
        animationSpec = tween(durationMillis = 200),
        label = "borderColor"
    )
    
    val dotColor by animateColorAsState(
        targetValue = if (selected) primaryColor else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "dotColor"
    )

    Box(
        modifier = modifier
            .size(24.dp)
            .scale(buttonScale)
            .semantics { role = Role.RadioButton }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = false,
                    radius = 20.dp
                ),
                enabled = enabled && onClick != null,
                onClick = onClick ?: {}
            )
            .alpha(if (enabled) 1f else 0.38f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val outerRadius = size.minDimension / 2
            val strokeWidth = 2.dp.toPx()
            
            drawCircle(
                color = borderColor,
                radius = outerRadius - strokeWidth / 2,
                style = Stroke(width = strokeWidth)
            )
            
            if (dotScale > 0f) {
                val dotRadius = (outerRadius / 2) * dotScale
                drawCircle(
                    color = dotColor,
                    radius = dotRadius
                )
            }
        }
    }
}

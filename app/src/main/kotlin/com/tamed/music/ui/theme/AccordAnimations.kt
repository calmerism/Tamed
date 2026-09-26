package com.tamed.music.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp

/**
 * Animation curves, durations, and transition specifications ported directly
 * from Accord 2.0 (uk.akane.cupertino and uk.akane.accord).
 */
object AccordAnimations {
    /** Standard Apple easing curve: PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f) */
    val StandardEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    /** Cupertino page transition easing curve: PathInterpolator(0.2833f, 0.99f, 0.31833f, 0.99f) */
    val CupertinoTransitionEasing = CubicBezierEasing(0.2833f, 0.99f, 0.31833f, 0.99f)

    /** Decelerate easing for touch interactions: DecelerateInterpolator(1.7f) */
    val DecelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    /** Accelerate easing for exit animations: AccelerateInterpolator(1.7f) */
    val AccelerateEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

    /** Button toggle overshoot spring spec (matching Accord's OvershootInterpolator) */
    val ButtonToggleSpring = spring<Float>(
        dampingRatio = 0.55f,
        stiffness = 650f
    )

    /** Press shrink scale spec (matching Accord's ShrinkableView) */
    val ButtonPressSpec = tween<Float>(
        durationMillis = 150,
        easing = DecelerateEasing
    )

    const val NavTransitionDuration = 500
    const val CoverScaleDuration = 500
    const val SheetTransitionDuration = 270
    const val ContentSwapDuration = 300

    /** Navigation push/pop animation spec */
    val NavTransitionSpec = tween<Int>(
        durationMillis = NavTransitionDuration,
        easing = CupertinoTransitionEasing
    )

    /** Artwork play/pause scale spec (1.0f playing, 0.84f paused) */
    val CoverScaleSpec = tween<Float>(
        durationMillis = CoverScaleDuration,
        easing = StandardEasing
    )

    /** Content swap cross-fade / scale spec (Lyrics / Queue / Controls) */
    val ContentSwapSpec = tween<Float>(
        durationMillis = ContentSwapDuration,
        easing = StandardEasing
    )
}

/**
 * Interactive touch shrink modifier ported from Accord's ShrinkableView.
 * Shrinks to 0.92f on press with decelerate easing, and springs back to 1.0f on release.
 */
fun Modifier.accordPressScale(
    pressedScale: Float = 0.92f,
    interactionSource: MutableInteractionSource? = null,
): Modifier = composed {
    val localInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressedState = localInteractionSource.collectIsPressedAsState()
    var isPointerPressed by remember { mutableStateOf(false) }
    val isPressed = isPressedState.value || isPointerPressed

    val scaleState = animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = if (isPressed) AccordAnimations.ButtonPressSpec else spring(stiffness = Spring.StiffnessMediumLow),
        label = "accordPressScale"
    )
    this
        .graphicsLayer {
            scaleX = scaleState.value
            scaleY = scaleState.value
        }
        .then(
            if (interactionSource == null) {
                Modifier.pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        isPointerPressed = true
                        waitForUpOrCancellation()
                        isPointerPressed = false
                    }
                }
            } else {
                Modifier
            }
        )
}

/**
 * Toggle activation bounce scale modifier ported from Accord's FullPlayer overlayButton animator.
 * Scales with overshoot spring when active state changes.
 */
@Composable
fun Modifier.accordToggleScale(
    isActive: Boolean
): Modifier {
    val scaleState = animateFloatAsState(
        targetValue = if (isActive) 1.08f else 1.0f,
        animationSpec = AccordAnimations.ButtonToggleSpring,
        label = "accordToggleScale"
    )
    return this.graphicsLayer {
        scaleX = scaleState.value
        scaleY = scaleState.value
    }
}

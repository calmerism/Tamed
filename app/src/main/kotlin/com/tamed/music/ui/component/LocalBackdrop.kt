package com.tamed.music.ui.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import androidx.compose.runtime.staticCompositionLocalOf

val LocalBackdrop = staticCompositionLocalOf<Backdrop> { emptyBackdrop() }

fun isRunningOnEmulator(): Boolean {
    return Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.MODEL.contains("Genymotion") ||
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
            "google_sdk" == Build.PRODUCT ||
            Build.PRODUCT.contains("sdk_gphone") ||
            Build.MODEL.contains("emu64") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu")
}

@Composable
fun rememberLayerBackdropSafe(): Backdrop {
    val isEmulator = remember { isRunningOnEmulator() }

    return if (isEmulator) {
        remember { emptyBackdrop() }
    } else {
        rememberLayerBackdrop()
    }
}

fun Modifier.layerBackdropSafe(backdrop: Backdrop): Modifier = this.composed {
    val isEmulator = remember { isRunningOnEmulator() }

    if (isEmulator || backdrop !is com.kyant.backdrop.backdrops.LayerBackdrop) {
        this
    } else {
        this.layerBackdrop(backdrop)
    }
}

fun Modifier.drawBackdropSafe(
    backdrop: Backdrop,
    shape: () -> com.kyant.shapes.Capsule,
    effects: com.kyant.backdrop.BackdropEffectScope.() -> Unit = {},
    highlight: () -> com.kyant.backdrop.highlight.Highlight = { com.kyant.backdrop.highlight.Highlight.Default },
    shadow: () -> com.kyant.backdrop.shadow.Shadow = { com.kyant.backdrop.shadow.Shadow.Default },
    innerShadow: () -> com.kyant.backdrop.shadow.InnerShadow = { com.kyant.backdrop.shadow.InnerShadow.Default },
    layerBlock: androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit = {},
    onDrawSurface: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit = {}
): Modifier = this.composed {
    val isEmulator = remember { isRunningOnEmulator() }

    if (isEmulator) {
        val calculatedShape = shape()
        this
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.12f)
                    )
                ),
                shape = androidx.compose.foundation.shape.CircleShape
            )
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.10f),
                shape = androidx.compose.foundation.shape.CircleShape
            )
    } else {
        this.drawBackdrop(
            backdrop = backdrop,
            shape = shape,
            effects = effects,
            highlight = highlight,
            shadow = shadow,
            innerShadow = innerShadow,
            layerBlock = layerBlock,
            onDrawSurface = onDrawSurface
        )
    }
}

package com.tamed.music.shared

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

class TamedIosBridge {
    fun makeRootViewController(): UIViewController = ComposeUIViewController { TamedRoot() }

    fun snapshot(): TamedAppSnapshot = TamedAppModel.snapshot()
}

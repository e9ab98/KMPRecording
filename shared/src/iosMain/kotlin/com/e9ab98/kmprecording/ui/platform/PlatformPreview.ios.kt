package com.e9ab98.kmprecording.ui.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import platform.UIKit.UIView

@Composable
actual fun PlatformCameraPreview(
    previewHandle: Any,
    modifier: Modifier
) {
    val view = previewHandle as? UIView
    if (view != null) {
        UIKitView(modifier = modifier, factory = { view })
    } else {
        Box(modifier = modifier.fillMaxSize().background(Color.Black))
    }
}

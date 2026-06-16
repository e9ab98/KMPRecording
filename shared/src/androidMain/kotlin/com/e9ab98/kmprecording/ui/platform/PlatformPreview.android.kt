package com.e9ab98.kmprecording.ui.platform

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun PlatformCameraPreview(
    previewHandle: Any,
    modifier: Modifier
) {
    val previewView = previewHandle as? PreviewView
    if (previewView != null) {
        AndroidView(modifier = modifier, factory = { previewView })
    } else {
        Box(modifier = modifier.fillMaxSize().background(Color.Black))
    }
}

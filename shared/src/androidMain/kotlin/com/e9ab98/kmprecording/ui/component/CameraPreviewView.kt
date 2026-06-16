package com.e9ab98.kmprecording.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import com.e9ab98.kmprecording.service.AndroidCameraService

@Composable
actual fun CameraPreviewView(
    modifier: Modifier,
    cameraService: Any?
) {
    if (cameraService is AndroidCameraService) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                val previewView = cameraService.getPreviewView() as PreviewView
                previewView
            }
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
        }
    }
}
package com.e9ab98.kmprecording.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import com.e9ab98.kmprecording.service.IOSCameraService
import com.e9ab98.kmprecording.service.RecordingBridge
import platform.UIKit.UIView

@Composable
actual fun CameraPreviewView(
    modifier: Modifier,
    cameraService: Any?
) {
    if (cameraService is IOSCameraService) {
        val captureSession = remember { cameraService.getCaptureSession() }

        UIKitView(
            modifier = modifier,
            factory = {
                val bridge = RecordingBridge.createPreviewView
                if (bridge != null) {
                    bridge()
                } else {
                    val view = UIView()
                    view.backgroundColor = platform.UIKit.UIColor.blackColor
                    view
                }
            },
            update = { view ->
                val session = captureSession
                if (session != null) {
                    RecordingBridge.setPreviewSession?.invoke(view, session)
                }
            },
            onRelease = {
                // 不需要移除 previewLayer，PreviewView 的 layer 是视图主 layer
                // session 断开由 IOSCameraService 单例管理
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

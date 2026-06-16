package com.e9ab98.kmprecording.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import com.e9ab98.kmprecording.service.RecordingBridge
import platform.Foundation.NSURL
import platform.UIKit.UIView

@Composable
actual fun VideoPlayerView(
    modifier: Modifier,
    videoPath: String
) {
    if (videoPath.isEmpty()) {
        Box(
            modifier = modifier.background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("无视频", color = Color.White)
        }
        return
    }

    val playerView = remember(videoPath) {
        RecordingBridge.createPlayerView?.invoke() ?: UIView()
    }

    LaunchedEffect(videoPath, playerView) {
        val url = NSURL.fileURLWithPath(videoPath)
        RecordingBridge.setPlayerVideoURL?.invoke(playerView, url)
        RecordingBridge.playerPlay?.invoke(playerView)
    }

    DisposableEffect(playerView) {
        onDispose {
            RecordingBridge.playerCleanup?.invoke(playerView)
        }
    }

    UIKitView(
        modifier = modifier.background(Color.Black),
        factory = { playerView },
        update = { view ->
            RecordingBridge.playerPlay?.invoke(view)
        },
        onRelease = { view ->
            RecordingBridge.playerCleanup?.invoke(view)
        }
    )
}

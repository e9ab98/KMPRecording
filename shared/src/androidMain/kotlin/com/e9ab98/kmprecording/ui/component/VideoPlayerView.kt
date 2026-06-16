package com.e9ab98.kmprecording.ui.component

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File

@Composable
actual fun VideoPlayerView(
    modifier: Modifier,
    videoPath: String,
    onTimeUpdate: ((Long) -> Unit)?
) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build()
    }

    LaunchedEffect(videoPath) {
        if (videoPath.isNotEmpty()) {
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(videoPath))))
            player.prepare()
            player.playWhenReady = true
        }
    }

    if (onTimeUpdate != null) {
        LaunchedEffect(player, videoPath) {
            while (true) {
                onTimeUpdate(player.currentPosition)
                kotlinx.coroutines.delay(100)
            }
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                keepScreenOn = true
            }
        },
        update = { view ->
            view.player = player
        }
    )
}

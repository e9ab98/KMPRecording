package com.e9ab98.kmprecording.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

expect @Composable
fun VideoPlayerView(
    modifier: Modifier = Modifier,
    videoPath: String
)
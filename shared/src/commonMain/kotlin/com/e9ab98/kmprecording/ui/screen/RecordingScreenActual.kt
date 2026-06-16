package com.e9ab98.kmprecording.ui.screen

import androidx.compose.runtime.Composable

expect @Composable
fun RecordingScreenActual(
    onNavigateToVideoList: () -> Unit,
    onNavigateToSettings: () -> Unit
)
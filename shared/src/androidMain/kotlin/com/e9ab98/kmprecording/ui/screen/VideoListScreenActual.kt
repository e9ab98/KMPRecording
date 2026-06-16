package com.e9ab98.kmprecording.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.e9ab98.kmprecording.domain.AndroidSettingsRepository
import com.e9ab98.kmprecording.domain.AndroidVideoStore
import com.e9ab98.kmprecording.presentation.VideoLibraryViewModel

@Composable
actual fun VideoListScreenActual(
    onNavigateToRecording: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val videoStore = remember { AndroidVideoStore(context) }
    val settingsRepository = remember { AndroidSettingsRepository(context) }
    val viewModel = remember { VideoLibraryViewModel(videoStore, settingsRepository) }

    VideoLibraryScreen(
        viewModel = viewModel,
        onNavigateToRecording = onNavigateToRecording,
        onNavigateToSettings = onNavigateToSettings
    )
}

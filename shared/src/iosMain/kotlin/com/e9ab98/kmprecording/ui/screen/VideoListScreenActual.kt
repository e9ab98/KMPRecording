package com.e9ab98.kmprecording.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.e9ab98.kmprecording.domain.IOSSettingsRepository
import com.e9ab98.kmprecording.domain.IOSVideoStore
import com.e9ab98.kmprecording.presentation.VideoLibraryViewModel

@Composable
actual fun VideoListScreenActual(
    onNavigateToRecording: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val videoStore = remember { IOSVideoStore() }
    val settingsRepository = remember { IOSSettingsRepository() }
    val viewModel = remember { VideoLibraryViewModel(videoStore, settingsRepository) }

    VideoLibraryScreen(
        viewModel = viewModel,
        onNavigateToRecording = onNavigateToRecording,
        onNavigateToSettings = onNavigateToSettings
    )
}

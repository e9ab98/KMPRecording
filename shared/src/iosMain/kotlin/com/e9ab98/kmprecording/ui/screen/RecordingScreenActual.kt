package com.e9ab98.kmprecording.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.e9ab98.kmprecording.domain.AVFoundationRecorderEngine
import com.e9ab98.kmprecording.domain.IOSSettingsRepository
import com.e9ab98.kmprecording.domain.IOSVideoStore
import com.e9ab98.kmprecording.domain.RecorderController
import com.e9ab98.kmprecording.model.CameraType
import com.e9ab98.kmprecording.presentation.RecordingViewModel
import com.e9ab98.kmprecording.service.IOSLocationTracker
import kotlinx.coroutines.launch

@Composable
actual fun RecordingScreenActual(
    onNavigateToVideoList: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val engine = remember { AVFoundationRecorderEngine() }
    val videoStore = remember { IOSVideoStore() }
    val settingsRepository = remember { IOSSettingsRepository() }
    val locationTracker = remember { IOSLocationTracker() }
    val controller = remember { RecorderController(engine, videoStore, locationTracker) }
    val viewModel = remember { RecordingViewModel(controller, settingsRepository, locationTracker) }

    LaunchedEffect(engine) {
        engine.prepare(settingsRepository.recordingConfig.value)
    }

    DisposableEffect(engine) {
        onDispose {
            engine.release()
        }
    }

    RecordingScreen(
        viewModel = viewModel,
        previewHandle = engine.previewHandle(),
        onNavigateToVideoList = onNavigateToVideoList,
        onNavigateToSettings = onNavigateToSettings,
        onSwitchCamera = {
            val nextCamera = if (viewModel.uiState.value.cameraType == CameraType.BACK) {
                CameraType.FRONT
            } else {
                CameraType.BACK
            }
            scope.launch {
                engine.switchCamera(nextCamera)
                settingsRepository.updateCameraType(nextCamera)
            }
        }
    )
}

package com.e9ab98.kmprecording.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.e9ab98.kmprecording.domain.AndroidSettingsRepository
import com.e9ab98.kmprecording.domain.AndroidVideoStore
import com.e9ab98.kmprecording.domain.CameraXRecorderEngine
import com.e9ab98.kmprecording.domain.RecorderController
import com.e9ab98.kmprecording.model.CameraType
import com.e9ab98.kmprecording.presentation.RecordingViewModel
import com.e9ab98.kmprecording.service.AndroidLocationTracker
import kotlinx.coroutines.launch

@Composable
actual fun RecordingScreenActual(
    onNavigateToVideoList: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val engine = remember { CameraXRecorderEngine(context, lifecycleOwner) }
    val videoStore = remember { AndroidVideoStore(context) }
    val settingsRepository = remember { AndroidSettingsRepository(context) }
    val locationTracker = remember { AndroidLocationTracker(context) }
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

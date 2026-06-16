package com.e9ab98.kmprecording.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.e9ab98.kmprecording.presentation.RecordingViewModel
import com.e9ab98.kmprecording.ui.component.RecordingOverlay
import com.e9ab98.kmprecording.ui.component.RightControlDeck
import com.e9ab98.kmprecording.ui.platform.PlatformCameraPreview

import androidx.compose.foundation.layout.systemBarsPadding

@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel,
    previewHandle: Any,
    onNavigateToVideoList: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSwitchCamera: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        PlatformCameraPreview(previewHandle = previewHandle, modifier = Modifier.fillMaxSize())
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            RecordingOverlay(
                state = state,
                modifier = Modifier.fillMaxSize()
            )
            
            RightControlDeck(
                state = state,
                onModeChanged = viewModel::modeChanged,
                onSwitchCamera = onSwitchCamera,
                onOpenLibrary = onNavigateToVideoList,
                onOpenSettings = onNavigateToSettings,
                onStart = viewModel::startClicked,
                onStop = viewModel::stopClicked,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
            
            state.errorMessage?.let { message ->
                Snackbar(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                    Text(message)
                }
            }
        }
    }
}

package com.e9ab98.kmprecording.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.e9ab98.kmprecording.domain.L10n
import com.e9ab98.kmprecording.domain.RecordingLifecycle
import com.e9ab98.kmprecording.domain.RecordingMode
import com.e9ab98.kmprecording.presentation.RecordingUiState

@Composable
fun RightControlDeck(
    state: RecordingUiState,
    onModeChanged: (RecordingMode) -> Unit,
    onSwitchCamera: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecording = state.lifecycle is RecordingLifecycle.Recording ||
            state.lifecycle is RecordingLifecycle.Paused
    val l10n = L10n.get(state.appLanguage)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(100.dp)
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top options
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = {
                    onModeChanged(
                        if (state.mode == RecordingMode.NORMAL) RecordingMode.LOOP else RecordingMode.NORMAL
                    )
                },
                enabled = !isRecording
            ) {
                Text(
                    text = if (state.mode == RecordingMode.LOOP) l10n.modeLoop else l10n.modeNormal,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.mode == RecordingMode.LOOP) MaterialTheme.colorScheme.primary else Color.White
                )
            }

            IconButton(onClick = onSwitchCamera, enabled = !isRecording) {
                Text(
                    text = l10n.switchCamera,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }

        // Record Button
        ShutterButton(
            isRecording = isRecording,
            onClick = if (isRecording) onStop else onStart
        )

        // Bottom options
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = onOpenLibrary) {
                Text(
                    text = "📂",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            IconButton(onClick = onOpenSettings, enabled = !isRecording) {
                Text(
                    text = "⚙️",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun ShutterButton(
    isRecording: Boolean,
    onClick: () -> Unit
) {
    val cornerRadius by animateFloatAsState(targetValue = if (isRecording) 12f else 32f)
    val innerSize by animateFloatAsState(targetValue = if (isRecording) 32f else 54f)

    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.3f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(innerSize.dp)
                    .clip(RoundedCornerShape(cornerRadius.dp))
                    .background(Color.Red)
            )
        }
    }
}

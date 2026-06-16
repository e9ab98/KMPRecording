package com.e9ab98.kmprecording.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.e9ab98.kmprecording.domain.L10n
import com.e9ab98.kmprecording.model.VideoRecord
import com.e9ab98.kmprecording.presentation.VideoLibraryViewModel
import com.e9ab98.kmprecording.ui.component.VideoPlayerView
import androidx.compose.ui.text.font.FontWeight
import com.e9ab98.kmprecording.domain.SrtParser
import com.e9ab98.kmprecording.domain.SrtCue
import com.e9ab98.kmprecording.service.readTextFromFile


@Composable
fun VideoLibraryScreen(
    viewModel: VideoLibraryViewModel,
    onNavigateToRecording: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val selectedVideo = state.selectedVideo
    val l10n = L10n.get(state.appLanguage)

    if (selectedVideo != null) {
        VideoPlaybackScreen(
            video = selectedVideo,
            onBack = viewModel::clearSelection
        )
        return
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
        ) {
            StorageSummary(
                usedSpaceMB = state.usedSpaceMB,
                availableSpaceMB = state.availableSpaceMB,
                videoCount = state.videos.size,
                totalDurationSeconds = state.totalDurationSeconds,
                l10n = l10n
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(l10n.loading, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    state.videos.isEmpty() -> {
                        EmptyVideoLibrary(
                            onNavigateToRecording = onNavigateToRecording,
                            l10n = l10n
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.videos, key = { it.id }) { video ->
                                VideoItem(
                                    video = video,
                                    onClick = { viewModel.selectVideo(video) },
                                    onDelete = { viewModel.deleteVideo(video.id) },
                                    l10n = l10n
                                )
                            }
                        }
                    }
                }

                state.errorMessage?.let { message ->
                    val displayMessage = when (message) {
                        "加载视频失败" -> l10n.loadVideosFailed
                        "删除视频失败" -> l10n.deleteVideoFailed
                        else -> message
                    }
                    Snackbar(modifier = Modifier.align(Alignment.BottomCenter)) {
                        Text(displayMessage)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            FloatingActionButton(
                onClick = onNavigateToRecording,
                modifier = Modifier.size(72.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Text(
                    text = "➕",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(l10n.startRecording, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(32.dp))

            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Text(
                    text = "⚙️",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(l10n.settingsButton, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun VideoPlaybackScreen(
    video: VideoRecord,
    onBack: () -> Unit
) {
    val srtPath = remember(video.filePath) {
        video.filePath.substringBeforeLast(".") + ".srt"
    }
    
    val srtCues = remember(srtPath) {
        val content = readTextFromFile(srtPath)
        SrtParser.parse(content)
    }
    
    var playbackTimeMs by remember { mutableStateOf(0L) }
    
    val currentCue = remember(playbackTimeMs, srtCues) {
        srtCues.find { playbackTimeMs in it.startTimeMs..it.endTimeMs }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VideoPlayerView(
            modifier = Modifier.fillMaxSize(),
            videoPath = video.filePath,
            onTimeUpdate = { timeMs ->
                playbackTimeMs = timeMs
            }
        )
        
        if (currentCue != null) {
            PlaybackHudOverlay(
                cue = currentCue,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 24.dp, vertical = 72.dp) // Offset above standard controller overlay
            )
        }
        
        // Top Bar Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Text(
                    text = "◀️",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = video.fileName,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlaybackHudOverlay(
    cue: SrtCue,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                val speed = cue.speedText.substringAfter("Speed: ").substringBefore("km/h").trim().ifEmpty { "--" }
                Text(
                    text = speed,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 32.sp
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "km/h",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            
            Text(
                text = "HUD PLAYBACK",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF64B5F6),
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        Column {
            val latText = cue.locationText.substringBefore(",").trim()
            val lonText = cue.locationText.substringAfter(",").trim()
            Text(
                text = latText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = lonText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun StorageSummary(
    usedSpaceMB: Long,
    availableSpaceMB: Long,
    videoCount: Int,
    totalDurationSeconds: Int,
    l10n: L10n
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(l10n.storageTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                val desc = l10n.storageDesc
                    .replaceFirst("%s", usedSpaceMB.toString())
                    .replaceFirst("%s", availableSpaceMB.toString())
                Text(desc, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(l10n.statisticsTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                val desc = l10n.statisticsDesc
                    .replaceFirst("%s", videoCount.toString())
                    .replaceFirst("%s", formatTotalDuration(totalDurationSeconds, l10n))
                Text(desc, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun EmptyVideoLibrary(
    onNavigateToRecording: () -> Unit,
    l10n: L10n
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("▶️", style = androidx.compose.ui.text.TextStyle(fontSize = 64.sp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(l10n.noVideos, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateToRecording) {
                Text("➕")
                Spacer(modifier = Modifier.width(8.dp))
                Text(l10n.startRecording)
            }
        }
    }
}

@Composable
private fun VideoItem(
    video: VideoRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    l10n: L10n
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("▶️", style = MaterialTheme.typography.headlineMedium)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    video.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${formatDuration(video.durationSeconds)} • ${formatFileSize(video.fileSizeBytes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(48.dp)
            ) {
                Text(
                    text = "🗑️",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(l10n.deleteTitle) },
            text = { Text(l10n.deleteConfirmMsg) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text(l10n.deleteButton, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(l10n.cancelButton)
                }
            }
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
    } else {
        "${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

private fun formatTotalDuration(totalSeconds: Int, l10n: L10n): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}${l10n.hourText}${minutes.toString().padStart(2, '0')}${l10n.minuteText}${secs.toString().padStart(2, '0')}${l10n.secondText}"
        minutes > 0 -> "${minutes}${l10n.minuteText}${secs.toString().padStart(2, '0')}${l10n.secondText}"
        else -> "${secs}${l10n.secondText}"
    }
}

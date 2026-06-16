package com.e9ab98.kmprecording.domain

import com.e9ab98.kmprecording.model.CameraType
import com.e9ab98.kmprecording.model.RecordingConfig
import com.e9ab98.kmprecording.model.Resolution
import com.e9ab98.kmprecording.model.VideoQuality
import kotlinx.datetime.LocalDateTime

data class RecordingSessionState(
    val lifecycle: RecordingLifecycle,
    val mode: RecordingMode,
    val elapsedSeconds: Int,
    val activeSegmentIndex: Int?
)

data class PreviewState(
    val ready: Boolean = false,
    val errorMessage: String? = null
)

data class EngineRecordingState(
    val isRecording: Boolean = false,
    val activeSegment: ActiveSegment? = null
)

data class SegmentOutput(
    val sessionId: String,
    val filePath: String,
    val segmentIndex: Int?,
    val mode: RecordingMode
)

data class ActiveSegment(
    val sessionId: String,
    val filePath: String,
    val segmentIndex: Int?,
    val startedAt: LocalDateTime
)

data class RecordedSegment(
    val sessionId: String,
    val filePath: String,
    val segmentIndex: Int?,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime,
    val durationSeconds: Int,
    val fileSizeBytes: Long,
    val mode: RecordingMode,
    val config: RecordingConfig
)

data class StorageState(
    val totalSpaceMB: Long = 0,
    val availableSpaceMB: Long = 0,
    val usedSpaceMB: Long = 0,
    val maxStorageMB: Long = 0,
    val isLowSpace: Boolean = false,
    val isCriticalSpace: Boolean = false
)

data class RecordingDefaults(
    val config: RecordingConfig,
    val mode: RecordingMode,
    val cameraType: CameraType = config.cameraType,
    val resolution: Resolution = config.resolution,
    val quality: VideoQuality = config.quality
)

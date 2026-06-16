package com.e9ab98.kmprecording.model

import kotlinx.datetime.LocalDateTime

data class VideoRecord(
    val id: String,
    val filePath: String,
    val fileName: String,
    val durationSeconds: Int,
    val fileSizeBytes: Long,
    val createdAt: LocalDateTime,
    val thumbnailPath: String? = null,
    val resolution: Resolution,
    val cameraType: CameraType,
    val isSegment: Boolean = false,
    val sessionId: String? = null
) {
    val fileSizeMB: Long
        get() = fileSizeBytes / (1024 * 1024)
}

data class VideoSegment(
    val id: String,
    val sessionId: String,
    val segmentIndex: Int,
    val filePath: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val durationSeconds: Int,
    val fileSizeBytes: Long,
    val isLocked: Boolean = false,
    val isEmergency: Boolean = false
) {
    val fileSizeMB: Long
        get() = fileSizeBytes / (1024 * 1024)
}

data class RecordingSessionInfo(
    val sessionId: String,
    val startTime: LocalDateTime,
    val config: RecordingConfig,
    val segments: List<VideoSegment>,
    val totalDurationSeconds: Int,
    val totalSizeMB: Long,
    val isActive: Boolean
)

data class SegmentRecordingConfig(
    val segmentDurationSeconds: Int = 60,
    val maxSegments: Int = 100,
    val maxStorageMB: Long = 1024 * 1024,
    val enableLoopRecording: Boolean = true,
    val enableEmergencyLock: Boolean = true
)
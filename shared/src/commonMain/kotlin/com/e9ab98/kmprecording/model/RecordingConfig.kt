package com.e9ab98.kmprecording.model

import kotlinx.datetime.LocalDateTime

enum class Resolution(val width: Int, val height: Int) {
    HD_720P(1280, 720),
    FHD_1080P(1920, 1080),
    UHD_4K(3840, 2160)
}

enum class VideoQuality(val bitrate: Int) {
    LOW(2_000_000),
    MEDIUM(5_000_000),
    HIGH(10_000_000),
    VERY_HIGH(20_000_000)
}

enum class CameraType {
    BACK,
    FRONT
}

data class RecordingConfig(
    val resolution: Resolution = Resolution.FHD_1080P,
    val quality: VideoQuality = VideoQuality.HIGH,
    val cameraType: CameraType = CameraType.BACK,
    val maxDurationSeconds: Int = 0,
    val enableAudio: Boolean = true,
    val segmentDurationSeconds: Int = 300,
    val maxStorageMB: Long = 1024 * 1024,
    val showHud: Boolean = true,
    val generateSrt: Boolean = true
)

data class RecordingSession(
    val id: String,
    val startTime: LocalDateTime,
    val config: RecordingConfig,
    val outputPath: String = ""
)
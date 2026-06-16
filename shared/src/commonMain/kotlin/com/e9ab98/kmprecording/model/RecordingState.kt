package com.e9ab98.kmprecording.model

sealed class RecordingState {
    object Idle : RecordingState()
    object Preparing : RecordingState()
    data class Recording(
        val sessionId: String,
        val durationSeconds: Int,
        val currentSegment: Int
    ) : RecordingState()
    data class Paused(
        val sessionId: String,
        val durationSeconds: Int
    ) : RecordingState()
    data class Error(val message: String) : RecordingState()
}

data class RecordingInfo(
    val state: RecordingState = RecordingState.Idle,
    val isCameraReady: Boolean = false,
    val availableStorageMB: Long = 0,
    val currentCamera: CameraType = CameraType.BACK,
    val resolution: Resolution = Resolution.FHD_1080P
)
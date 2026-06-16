package com.e9ab98.kmprecording.domain

sealed class RecordingLifecycle {
    object Idle : RecordingLifecycle()
    object Preparing : RecordingLifecycle()
    data class Recording(val sessionId: String) : RecordingLifecycle()
    data class Paused(val sessionId: String) : RecordingLifecycle()
    data class Stopping(val sessionId: String) : RecordingLifecycle()
    data class Error(val message: String) : RecordingLifecycle()
}

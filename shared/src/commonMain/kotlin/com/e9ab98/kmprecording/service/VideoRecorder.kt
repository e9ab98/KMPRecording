package com.e9ab98.kmprecording.service

import com.e9ab98.kmprecording.model.RecordingConfig
import com.e9ab98.kmprecording.model.RecordingSession
import com.e9ab98.kmprecording.model.RecordingState
import kotlinx.coroutines.flow.StateFlow

interface VideoRecorder {
    val state: StateFlow<RecordingState>
    val currentSession: StateFlow<RecordingSession?>
    val durationSeconds: StateFlow<Int>
    
    suspend fun startRecording(config: RecordingConfig): Result<RecordingSession>
    suspend fun stopRecording(): Result<String>
    suspend fun pauseRecording()
    suspend fun resumeRecording()
    
    suspend fun attachCamera(cameraService: CameraService)
    fun detachCamera()
}
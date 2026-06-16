package com.e9ab98.kmprecording.service

import com.e9ab98.kmprecording.model.RecordingConfig
import com.e9ab98.kmprecording.model.RecordingSession
import com.e9ab98.kmprecording.model.RecordingState
import com.e9ab98.kmprecording.model.Resolution
import com.e9ab98.kmprecording.model.VideoQuality
import com.e9ab98.kmprecording.model.CameraType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlin.random.Random

class IOSVideoRecorder : VideoRecorder {
    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    override val state: StateFlow<RecordingState> = _state.asStateFlow()

    private val _currentSession = MutableStateFlow<RecordingSession?>(null)
    override val currentSession: StateFlow<RecordingSession?> = _currentSession.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0)
    override val durationSeconds: StateFlow<Int> = _durationSeconds.asStateFlow()

    private var cameraService: CameraService? = null
    private var durationJob: kotlinx.coroutines.Job? = null
    private var currentSessionId: String? = null

    override suspend fun startRecording(config: RecordingConfig): Result<RecordingSession> {
        return try {
            _state.value = RecordingState.Preparing
            
            val sessionId = "session_${Random.nextLong().toString(16)}"
            
            val session = RecordingSession(
                id = sessionId,
                startTime = LocalDateTime(2025, 1, 1, 0, 0, 0),
                config = config
            )
            
            currentSessionId = sessionId
            _currentSession.value = session
            _state.value = RecordingState.Recording(sessionId, 0, 1)
            _durationSeconds.value = 0
            
            startDurationTimer()
            
            Result.success(session)
        } catch (e: Exception) {
            _state.value = RecordingState.Error(e.message ?: "Failed to start recording")
            Result.failure(e)
        }
    }

    private fun startDurationTimer() {
        durationJob?.cancel()
        durationJob = GlobalScope.launch {
            var elapsed = 0
            while (isActive) {
                val currentState = _state.value
                if (currentState is RecordingState.Recording) {
                    _durationSeconds.value = elapsed
                    _state.value = RecordingState.Recording(
                        sessionId = currentState.sessionId,
                        durationSeconds = elapsed,
                        currentSegment = currentState.currentSegment
                    )
                    elapsed++
                } else if (currentState is RecordingState.Paused) {
                    // Paused, do nothing
                } else {
                    // Not recording or paused, exit loop
                    break
                }
                delay(1000L)
            }
        }
    }

    override suspend fun stopRecording(): Result<String> {
        return try {
            durationJob?.cancel()
            _state.value = RecordingState.Idle
            _currentSession.value = null
            _durationSeconds.value = 0
            currentSessionId = null
            Result.success("")
        } catch (e: Exception) {
            _state.value = RecordingState.Error(e.message ?: "Failed to stop recording")
            Result.failure(e)
        }
    }

    override suspend fun pauseRecording() {
        val current = _state.value
        if (current is RecordingState.Recording) {
            _state.value = RecordingState.Paused(
                sessionId = current.sessionId,
                durationSeconds = current.durationSeconds
            )
        }
    }

    override suspend fun resumeRecording() {
        val current = _state.value
        if (current is RecordingState.Paused) {
            _state.value = RecordingState.Recording(
                sessionId = current.sessionId,
                durationSeconds = current.durationSeconds,
                currentSegment = 1
            )
            startDurationTimer()
        }
    }

    override suspend fun attachCamera(cameraService: CameraService) {
        this.cameraService = cameraService
    }

    override fun detachCamera() {
        this.cameraService = null
    }
}
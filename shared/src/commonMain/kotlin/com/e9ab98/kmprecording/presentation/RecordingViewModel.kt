package com.e9ab98.kmprecording.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.e9ab98.kmprecording.domain.RecorderController
import com.e9ab98.kmprecording.domain.RecordingLifecycle
import com.e9ab98.kmprecording.domain.RecordingMode
import com.e9ab98.kmprecording.domain.SettingsRepository
import com.e9ab98.kmprecording.domain.AppLanguage
import com.e9ab98.kmprecording.service.LocationTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.e9ab98.kmprecording.currentTimeMillis

class RecordingViewModel(
    private val recorderController: RecorderController,
    private val settingsRepository: SettingsRepository,
    private val locationTracker: LocationTracker,
    coroutineScope: CoroutineScope? = null,
    observeExternalState: Boolean = true,
    private val timestampProvider: () -> String = ::currentRecordingTimestampText,
    private val timeProvider: () -> Long = ::currentTimeMillis
) : ViewModel() {
    private val scope = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(
        RecordingUiState(
            mode = settingsRepository.defaultMode.value,
            cameraType = settingsRepository.recordingConfig.value.cameraType,
            resolution = settingsRepository.recordingConfig.value.resolution,
            quality = settingsRepository.recordingConfig.value.quality,
            audioEnabled = settingsRepository.recordingConfig.value.enableAudio,
            timestampText = timestampProvider(),
            appLanguage = settingsRepository.appLanguage.value,
            showHud = settingsRepository.recordingConfig.value.showHud,
            generateSrt = settingsRepository.recordingConfig.value.generateSrt
        )
    )
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()
    private var tickerJob: Job? = null
    private var sessionStartTime: Long = 0L
    private var segmentStartTime: Long = 0L
    private var pausedTime: Long = 0L

    init {
        // Start location tracking for real-time HUD view if needed
        updateLocationTrackingState()

        if (observeExternalState) {
            scope.launch {
                settingsRepository.recordingConfig.collect { config ->
                    _uiState.value = _uiState.value.copy(
                        resolution = config.resolution,
                        quality = config.quality,
                        audioEnabled = config.enableAudio,
                        cameraType = config.cameraType,
                        showHud = config.showHud,
                        generateSrt = config.generateSrt
                    )
                    updateLocationTrackingState()
                }
            }
            scope.launch {
                settingsRepository.defaultMode.collect { mode ->
                    println("RecordingViewModel: defaultMode collected: $mode")
                    _uiState.value = _uiState.value.copy(mode = mode)
                }
            }
            scope.launch {
                settingsRepository.appLanguage.collect { language ->
                    _uiState.value = _uiState.value.copy(appLanguage = language)
                }
            }
            scope.launch {
                recorderController.state.collect { state ->
                    applyControllerState(state)
                    updateLocationTrackingState()
                }
            }
            scope.launch {
                locationTracker.locationState.collect { location ->
                    _uiState.value = _uiState.value.copy(
                        latitude = location?.latitude,
                        longitude = location?.longitude,
                        speedKmh = location?.speedKmh ?: 0.0
                    )
                }
            }
        }
    }

    private fun updateLocationTrackingState() {
        val config = settingsRepository.recordingConfig.value
        val isRecording = recorderController.state.value.lifecycle is RecordingLifecycle.Recording
        val needsLocation = config.showHud || (isRecording && config.generateSrt)
        if (needsLocation) {
            locationTracker.startTracking()
        } else {
            locationTracker.stopTracking()
        }
    }


    fun startClicked() {
        scope.launch {
            val now = timeProvider()
            sessionStartTime = now
            segmentStartTime = now
            pausedTime = 0L
            recorderController.start(settingsRepository.defaultMode.value, settingsRepository.recordingConfig.value)
            applyControllerState(recorderController.state.value)
            startTicker()
        }
    }

    fun stopClicked() {
        scope.launch {
            recorderController.stop()
            applyControllerState(recorderController.state.value)
            stopTicker()
        }
    }

    fun pauseClicked() {
        scope.launch {
            recorderController.pause()
            applyControllerState(recorderController.state.value)
            stopTicker()
            pausedTime = timeProvider()
        }
    }

    fun resumeClicked() {
        scope.launch {
            val now = timeProvider()
            if (pausedTime > 0L) {
                val pausedDuration = now - pausedTime
                sessionStartTime += pausedDuration
                segmentStartTime += pausedDuration
                pausedTime = 0L
            } else {
                sessionStartTime = now
                segmentStartTime = now
            }
            recorderController.resume()
            applyControllerState(recorderController.state.value)
            startTicker()
        }
    }

    fun modeChanged(mode: RecordingMode) {
        println("RecordingViewModel: modeChanged called with $mode")
        scope.launch {
            settingsRepository.updateDefaultMode(mode)
        }
    }

    fun dismissErrorClicked() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun applyControllerState(state: com.e9ab98.kmprecording.domain.RecordingSessionState) {
        val isRecordingActive = state.lifecycle is RecordingLifecycle.Recording ||
                state.lifecycle is RecordingLifecycle.Paused ||
                state.lifecycle is RecordingLifecycle.Stopping ||
                state.lifecycle is RecordingLifecycle.Preparing

        _uiState.value = _uiState.value.copy(
            lifecycle = state.lifecycle,
            mode = if (isRecordingActive) state.mode else settingsRepository.defaultMode.value,
            elapsedSeconds = state.elapsedSeconds,
            activeSegmentIndex = state.activeSegmentIndex,
            errorMessage = (state.lifecycle as? RecordingLifecycle.Error)?.message
        )
    }

    private fun startTicker() {
        if (_uiState.value.lifecycle !is RecordingLifecycle.Recording) return
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (_uiState.value.lifecycle is RecordingLifecycle.Recording) {
                delay(1_000)
                val now = timeProvider()
                val totalElapsed = ((now - sessionStartTime) / 1000).toInt()
                recorderController.updateElapsedSeconds(totalElapsed)
                applyControllerState(recorderController.state.value)
                _uiState.value = _uiState.value.copy(timestampText = timestampProvider())
                rolloverLoopSegmentIfNeeded()
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private suspend fun rolloverLoopSegmentIfNeeded() {
        val state = _uiState.value
        val segmentDuration = settingsRepository.recordingConfig.value.segmentDurationSeconds
        if (state.mode != RecordingMode.LOOP) return
        if (state.lifecycle !is RecordingLifecycle.Recording) return
        if (segmentDuration <= 0) return
        
        val now = timeProvider()
        val elapsedInSegment = (now - segmentStartTime) / 1000
        if (elapsedInSegment < segmentDuration) return

        segmentStartTime = now
        recorderController.rolloverLoopSegment()
        applyControllerState(recorderController.state.value)
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.stopTracking()
    }
}

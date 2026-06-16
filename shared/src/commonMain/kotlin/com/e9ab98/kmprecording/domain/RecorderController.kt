package com.e9ab98.kmprecording.domain

import com.e9ab98.kmprecording.model.RecordingConfig
import com.e9ab98.kmprecording.service.LocationTracker
import com.e9ab98.kmprecording.service.appendTextToFile
import com.e9ab98.kmprecording.service.writeTextToFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt

class RecorderController(
    private val engine: RecorderEngine,
    private val videoStore: VideoStore,
    private val locationTracker: LocationTracker
) {
    private val controllerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _state = MutableStateFlow(
        RecordingSessionState(
            lifecycle = RecordingLifecycle.Idle,
            mode = RecordingMode.NORMAL,
            elapsedSeconds = 0,
            activeSegmentIndex = null
        )
    )
    val state: StateFlow<RecordingSessionState> = _state.asStateFlow()

    private var currentConfig: RecordingConfig? = null
    private var currentMode: RecordingMode = RecordingMode.NORMAL
    private var nextSegmentIndex: Int = 0
    private var segmentElapsedSeconds: Int = 0

    private fun Double.formatDecimal(decimals: Int): String {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        val rounded = (this * multiplier).roundToInt() / multiplier
        val str = rounded.toString()
        val parts = str.split(".")
        if (parts.size == 2) {
            val frac = parts[1]
            if (frac.length < decimals) {
                return "${parts[0]}.${frac.padEnd(decimals, '0')}"
            }
        } else if (parts.size == 1 && decimals > 0) {
            return "$str.${"0".repeat(decimals)}"
        }
        return str
    }

    private fun formatSrtTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')},000"
    }

    suspend fun start(mode: RecordingMode, config: RecordingConfig): Result<Unit> {
        _state.value = _state.value.copy(lifecycle = RecordingLifecycle.Preparing, mode = mode)
        currentConfig = config
        currentMode = mode
        nextSegmentIndex = 0

        engine.prepare(config).getOrElse { error ->
            _state.value = _state.value.copy(
                lifecycle = RecordingLifecycle.Error(error.message ?: "Recording prepare failed")
            )
            return Result.failure(error)
        }

        return startNextSegment()
    }

    suspend fun stop(): Result<Unit> {
        val active = engine.engineState.value.activeSegment
        if (active == null) {
            _state.value = _state.value.copy(lifecycle = RecordingLifecycle.Idle, activeSegmentIndex = null)
            return Result.success(Unit)
        }

        _state.value = _state.value.copy(lifecycle = RecordingLifecycle.Stopping(active.sessionId))
        val recorded = engine.stopSegment().getOrElse { error ->
            _state.value = _state.value.copy(
                lifecycle = RecordingLifecycle.Error(error.message ?: "Recording stop failed")
            )
            return Result.failure(error)
        }
        videoStore.saveRecordedSegment(recorded).getOrElse { error ->
            _state.value = _state.value.copy(
                lifecycle = RecordingLifecycle.Error(error.message ?: "Saving recording failed")
            )
            return Result.failure(error)
        }
        _state.value = RecordingSessionState(
            lifecycle = RecordingLifecycle.Idle,
            mode = currentMode,
            elapsedSeconds = 0,
            activeSegmentIndex = null
        )
        return Result.success(Unit)
    }


    suspend fun rolloverLoopSegment(): Result<Unit> {
        if (currentMode != RecordingMode.LOOP) return Result.success(Unit)

        val recorded = engine.stopSegment().getOrElse { error ->
            _state.value = _state.value.copy(
                lifecycle = RecordingLifecycle.Error(error.message ?: "Segment stop failed")
            )
            return Result.failure(error)
        }

        // Save segment and clean up asynchronously to avoid blocking the rollover transition
        controllerScope.launch {
            videoStore.saveRecordedSegment(recorded).onFailure { error ->
                _state.value = _state.value.copy(
                    lifecycle = RecordingLifecycle.Error(error.message ?: "Segment save failed")
                )
            }
            currentConfig?.let { videoStore.cleanupOldVideos(it.maxStorageMB) }
        }

        nextSegmentIndex += 1
        return startNextSegment()
    }

    suspend fun pause(): Result<Unit> {
        val active = engine.engineState.value.activeSegment ?: return Result.success(Unit)
        return engine.pause().onSuccess {
            _state.value = _state.value.copy(lifecycle = RecordingLifecycle.Paused(active.sessionId))
        }
    }

    suspend fun resume(): Result<Unit> {
        val active = engine.engineState.value.activeSegment ?: return Result.success(Unit)
        return engine.resume().onSuccess {
            _state.value = _state.value.copy(lifecycle = RecordingLifecycle.Recording(active.sessionId))
        }
    }

    fun tickElapsed() {
        val currentState = _state.value
        if (currentState.lifecycle is RecordingLifecycle.Recording) {
            val nextSec = currentState.elapsedSeconds + 1
            updateElapsedSeconds(nextSec)
        }
    }

    fun updateElapsedSeconds(seconds: Int) {
        val currentState = _state.value
        if (currentState.lifecycle is RecordingLifecycle.Recording) {
            val oldSeconds = currentState.elapsedSeconds
            _state.value = currentState.copy(elapsedSeconds = seconds)
            
            if (seconds > oldSeconds) {
                val diff = seconds - oldSeconds
                val currentSegmentSec = segmentElapsedSeconds
                segmentElapsedSeconds += diff
                
                val config = currentConfig
                if (config != null && config.generateSrt) {
                    val activeSegment = engine.engineState.value.activeSegment
                    if (activeSegment != null) {
                        val srtPath = activeSegment.filePath.substringBeforeLast(".") + ".srt"
                        val loc = locationTracker.locationState.value
                        
                        val speedText = if (loc != null) {
                            "${loc.speedKmh.formatDecimal(1)} km/h"
                        } else {
                            "-- km/h"
                        }
                        
                        val coordText = if (loc != null) {
                            "Lat: ${loc.latitude.formatDecimal(6)}, Lon: ${loc.longitude.formatDecimal(6)}"
                        } else {
                            "Lat: --, Lon: --"
                        }

                        for (i in 1..diff) {
                            val startSec = currentSegmentSec + i - 1
                            val endSec = currentSegmentSec + i
                            val blockNumber = startSec + 1
                            
                            val subtitleBlock = buildString {
                                appendLine(blockNumber)
                                appendLine("${formatSrtTime(startSec)} --> ${formatSrtTime(endSec)}")
                                appendLine("Speed: $speedText")
                                appendLine(coordText)
                                appendLine()
                            }
                            
                            appendTextToFile(srtPath, subtitleBlock)
                        }
                    }
                }
            }
        }
    }

    private suspend fun startNextSegment(): Result<Unit> {
        val config = currentConfig ?: return Result.failure(IllegalStateException("No recording config"))
        val segmentIndex = if (currentMode == RecordingMode.LOOP) nextSegmentIndex else null
        val output = videoStore.createOutput(currentMode, segmentIndex).getOrElse { error ->
            _state.value = _state.value.copy(
                lifecycle = RecordingLifecycle.Error(error.message ?: "Output creation failed")
            )
            return Result.failure(error)
        }
        val active = engine.startSegment(output).getOrElse { error ->
            _state.value = _state.value.copy(
                lifecycle = RecordingLifecycle.Error(error.message ?: "Recording start failed")
            )
            return Result.failure(error)
        }
        
        segmentElapsedSeconds = 0
        if (config.generateSrt) {
            val srtPath = output.filePath.substringBeforeLast(".") + ".srt"
            writeTextToFile(srtPath, "")
        }
        
        _state.value = RecordingSessionState(
            lifecycle = RecordingLifecycle.Recording(active.sessionId),
            mode = currentMode,
            elapsedSeconds = _state.value.elapsedSeconds,
            activeSegmentIndex = segmentIndex
        )
        if (config.maxStorageMB > 0) {
            videoStore.refreshStorage()
        }
        return Result.success(Unit)
    }
}

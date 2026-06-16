package com.e9ab98.kmprecording.domain

import com.e9ab98.kmprecording.model.CameraType
import com.e9ab98.kmprecording.model.RecordingConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDateTime

class FakeRecorderEngine : RecorderEngine {
    private val _previewState = MutableStateFlow(PreviewState(ready = true))
    override val previewState: StateFlow<PreviewState> = _previewState

    private val _engineState = MutableStateFlow(EngineRecordingState())
    override val engineState: StateFlow<EngineRecordingState> = _engineState

    var preparedConfig: RecordingConfig? = null
    var startCalls = 0
    var stopCalls = 0
    var activeOutput: SegmentOutput? = null
    var now = LocalDateTime(2026, 6, 3, 12, 0, 0)

    override suspend fun prepare(config: RecordingConfig): Result<Unit> {
        preparedConfig = config
        return Result.success(Unit)
    }

    override suspend fun startSegment(output: SegmentOutput): Result<ActiveSegment> {
        startCalls += 1
        activeOutput = output
        val segment = ActiveSegment(output.sessionId, output.filePath, output.segmentIndex, now)
        _engineState.value = EngineRecordingState(isRecording = true, activeSegment = segment)
        return Result.success(segment)
    }

    override suspend fun stopSegment(): Result<RecordedSegment> {
        stopCalls += 1
        val output = activeOutput ?: return Result.failure(IllegalStateException("No active segment"))
        val config = preparedConfig ?: return Result.failure(IllegalStateException("Engine not prepared"))
        val recorded = RecordedSegment(
            sessionId = output.sessionId,
            filePath = output.filePath,
            segmentIndex = output.segmentIndex,
            startedAt = now,
            endedAt = now,
            durationSeconds = config.segmentDurationSeconds,
            fileSizeBytes = 1024,
            mode = output.mode,
            config = config
        )
        activeOutput = null
        _engineState.value = EngineRecordingState()
        return Result.success(recorded)
    }

    override suspend fun pause(): Result<Unit> = Result.success(Unit)

    override suspend fun resume(): Result<Unit> = Result.success(Unit)

    override suspend fun switchCamera(cameraType: CameraType): Result<Unit> = Result.success(Unit)

    override fun previewHandle(): Any = Unit

    override fun release() = Unit
}

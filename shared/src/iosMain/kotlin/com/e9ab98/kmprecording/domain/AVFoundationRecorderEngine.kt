package com.e9ab98.kmprecording.domain

import com.e9ab98.kmprecording.model.CameraType
import com.e9ab98.kmprecording.model.RecordingConfig
import com.e9ab98.kmprecording.service.IOSCameraService
import com.e9ab98.kmprecording.service.RecordingBridge
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDateTime
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
class AVFoundationRecorderEngine(
    private val cameraService: IOSCameraService = IOSCameraService()
) : RecorderEngine {
    private val _previewState = MutableStateFlow(PreviewState())
    override val previewState: StateFlow<PreviewState> = _previewState

    private val _engineState = MutableStateFlow(EngineRecordingState())
    override val engineState: StateFlow<EngineRecordingState> = _engineState

    private var currentConfig: RecordingConfig? = null
    private var currentOutput: SegmentOutput? = null
    private var currentStartedAt = now()
    private val previewView: UIView? by lazy {
        RecordingBridge.createPreviewView?.invoke()
    }

    override suspend fun prepare(config: RecordingConfig): Result<Unit> {
        return try {
            currentConfig = config
            if (!cameraService.isReady.value) {
                cameraService.initialize()
            }
            if (cameraService.currentResolution.value != config.resolution) {
                cameraService.setResolution(config.resolution)
            }
            if (cameraService.currentCamera.value != config.cameraType) {
                cameraService.setCameraType(config.cameraType)
            }
            bindPreviewSession()
            _previewState.value = PreviewState(ready = true)
            Result.success(Unit)
        } catch (error: Exception) {
            _previewState.value = PreviewState(ready = false, errorMessage = error.message)
            Result.failure(error)
        }
    }

    override suspend fun startSegment(output: SegmentOutput): Result<ActiveSegment> {
        val movieOutput = cameraService.getMovieOutput()
            ?: return Result.failure(IllegalStateException("Movie output is not ready"))
        val bridge = RecordingBridge.startRecording
            ?: return Result.failure(IllegalStateException("Recording bridge is not configured"))

        val delegate = cameraService.createRecordingDelegate()
        val url = NSURL.fileURLWithPath(output.filePath)
        currentOutput = output
        currentStartedAt = now()
        bridge(movieOutput, url, delegate)

        val active = ActiveSegment(output.sessionId, output.filePath, output.segmentIndex, currentStartedAt)
        _engineState.value = EngineRecordingState(isRecording = true, activeSegment = active)
        return Result.success(active)
    }

    override suspend fun stopSegment(): Result<RecordedSegment> {
        val output = currentOutput ?: return Result.failure(IllegalStateException("No active recording"))
        val config = currentConfig ?: return Result.failure(IllegalStateException("Recorder not prepared"))
        cameraService.getMovieOutput()?.stopRecording()
        delay(300)
        currentOutput = null
        _engineState.value = EngineRecordingState()
        val fileManager = NSFileManager.defaultManager
        val attrs = fileManager.attributesOfItemAtPath(output.filePath, error = null)
        val size = (attrs?.get("NSFileSize") as? platform.Foundation.NSNumber)?.longLongValue ?: 0L
        return Result.success(
            RecordedSegment(
                sessionId = output.sessionId,
                filePath = output.filePath,
                segmentIndex = output.segmentIndex,
                startedAt = currentStartedAt,
                endedAt = now(),
                durationSeconds = config.segmentDurationSeconds,
                fileSizeBytes = size,
                mode = output.mode,
                config = config
            )
        )
    }

    override suspend fun pause(): Result<Unit> = Result.success(Unit)

    override suspend fun resume(): Result<Unit> = Result.success(Unit)

    override suspend fun switchCamera(cameraType: CameraType): Result<Unit> {
        cameraService.setCameraType(cameraType)
        currentConfig = currentConfig?.copy(cameraType = cameraType)
        return Result.success(Unit)
    }

    override fun previewHandle(): Any {
        return previewView ?: Unit
    }

    override fun release() {
        cameraService.release()
        _previewState.value = PreviewState()
        _engineState.value = EngineRecordingState()
    }

    private fun bindPreviewSession() {
        val view = previewView ?: return
        cameraService.getCaptureSession()?.let { session ->
            RecordingBridge.setPreviewSession?.invoke(view, session)
        }
    }

    private fun now(): LocalDateTime {
        val formatter = NSDateFormatter()
        formatter.dateFormat = "yyyyMMdd_HHmmss"
        val value = formatter.stringFromDate(NSDate())
        val dateTime = value.split("_")
        val date = dateTime.getOrElse(0) { "20260101" }
        val time = dateTime.getOrElse(1) { "000000" }
        return LocalDateTime(
            year = date.substring(0, 4).toIntOrNull() ?: 2026,
            monthNumber = date.substring(4, 6).toIntOrNull() ?: 1,
            dayOfMonth = date.substring(6, 8).toIntOrNull() ?: 1,
            hour = time.substring(0, 2).toIntOrNull() ?: 0,
            minute = time.substring(2, 4).toIntOrNull() ?: 0,
            second = time.substring(4, 6).toIntOrNull() ?: 0
        )
    }
}

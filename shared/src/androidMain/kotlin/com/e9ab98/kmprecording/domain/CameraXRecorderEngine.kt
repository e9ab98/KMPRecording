package com.e9ab98.kmprecording.domain

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.e9ab98.kmprecording.model.CameraType
import com.e9ab98.kmprecording.model.RecordingConfig
import com.e9ab98.kmprecording.model.VideoQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import kotlin.coroutines.resume

class CameraXRecorderEngine(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : RecorderEngine {
    private val previewView = PreviewView(context).apply {
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    }

    private val _previewState = MutableStateFlow(PreviewState())
    override val previewState: StateFlow<PreviewState> = _previewState

    private val _engineState = MutableStateFlow(EngineRecordingState())
    override val engineState: StateFlow<EngineRecordingState> = _engineState

    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var currentRecording: Recording? = null
    private var currentConfig: RecordingConfig? = null
    private var currentOutput: SegmentOutput? = null
    private var currentStartedAt = now()
    private var stopContinuation: (Result<RecordedSegment>) -> Unit = {}

    override suspend fun prepare(config: RecordingConfig): Result<Unit> {
        return try {
            currentConfig = config
            val provider = ProcessCameraProvider.getInstance(context).get()
            cameraProvider = provider

            val preview = Preview.Builder()
                .setResolutionSelector(createResolutionSelector(config))
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(config.quality.toCameraXQuality()))
                .build()

            videoCapture = VideoCapture.Builder(recorder)
                .setResolutionSelector(createResolutionSelector(config))
                .build()

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                config.cameraType.toCameraSelector(),
                preview,
                videoCapture
            )
            _previewState.value = PreviewState(ready = true)
            Result.success(Unit)
        } catch (error: Exception) {
            _previewState.value = PreviewState(ready = false, errorMessage = error.message)
            Result.failure(error)
        }
    }

    override suspend fun startSegment(output: SegmentOutput): Result<ActiveSegment> {
        val config = currentConfig ?: return Result.failure(IllegalStateException("Recorder not prepared"))
        val capture = videoCapture ?: return Result.failure(IllegalStateException("VideoCapture not prepared"))
        return try {
            val outputFile = File(output.filePath)
            outputFile.parentFile?.mkdirs()
            val outputOptions = FileOutputOptions.Builder(outputFile).build()
            val pendingRecording = capture.output.prepareRecording(context, outputOptions)
            currentOutput = output
            currentStartedAt = now()

            currentRecording = if (config.enableAudio) {
                pendingRecording.withAudioEnabled()
            } else {
                pendingRecording
            }.start(ContextCompat.getMainExecutor(context)) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    finishSegment(event)
                }
            }

            val active = ActiveSegment(
                sessionId = output.sessionId,
                filePath = output.filePath,
                segmentIndex = output.segmentIndex,
                startedAt = currentStartedAt
            )
            _engineState.value = EngineRecordingState(isRecording = true, activeSegment = active)
            Result.success(active)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override suspend fun stopSegment(): Result<RecordedSegment> {
        val recording = currentRecording ?: return Result.failure(IllegalStateException("No active recording"))
        return suspendCancellableCoroutine { continuation ->
            stopContinuation = { result ->
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }
            recording.stop()
        }
    }

    override suspend fun pause(): Result<Unit> {
        currentRecording?.pause()
        return Result.success(Unit)
    }

    override suspend fun resume(): Result<Unit> {
        currentRecording?.resume()
        return Result.success(Unit)
    }

    override suspend fun switchCamera(cameraType: CameraType): Result<Unit> {
        val config = currentConfig ?: return Result.success(Unit)
        return prepare(config.copy(cameraType = cameraType))
    }

    override fun previewHandle(): Any = previewView

    override fun release() {
        currentRecording?.stop()
        currentRecording = null
        cameraProvider?.unbindAll()
        _previewState.value = PreviewState()
        _engineState.value = EngineRecordingState()
    }

    private fun finishSegment(event: VideoRecordEvent.Finalize) {
        val output = currentOutput
        val config = currentConfig
        currentRecording = null
        currentOutput = null
        _engineState.value = EngineRecordingState()

        if (event.hasError()) {
            stopContinuation(Result.failure(event.cause ?: IllegalStateException("Recording finalize failed")))
            stopContinuation = {}
            return
        }

        if (output == null || config == null) {
            stopContinuation(Result.failure(IllegalStateException("Missing active segment state")))
            stopContinuation = {}
            return
        }

        val file = File(output.filePath)
        val recorded = RecordedSegment(
            sessionId = output.sessionId,
            filePath = output.filePath,
            segmentIndex = output.segmentIndex,
            startedAt = currentStartedAt,
            endedAt = now(),
            durationSeconds = config.segmentDurationSeconds,
            fileSizeBytes = if (file.exists()) file.length() else 0,
            mode = output.mode,
            config = config
        )
        stopContinuation(Result.success(recorded))
        stopContinuation = {}
    }

    private fun createResolutionSelector(config: RecordingConfig): ResolutionSelector {
        return ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(config.resolution.width, config.resolution.height),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()
    }

    private fun CameraType.toCameraSelector(): CameraSelector {
        return when (this) {
            CameraType.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            CameraType.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }

    private fun VideoQuality.toCameraXQuality(): Quality {
        return when (this) {
            VideoQuality.LOW -> Quality.LOWEST
            VideoQuality.MEDIUM -> Quality.SD
            VideoQuality.HIGH -> Quality.HD
            VideoQuality.VERY_HIGH -> Quality.UHD
        }
    }

    private fun now() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
}

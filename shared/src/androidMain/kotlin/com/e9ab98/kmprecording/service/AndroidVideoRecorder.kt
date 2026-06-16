package com.e9ab98.kmprecording.service

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.e9ab98.kmprecording.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File

class AndroidVideoRecorder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : VideoRecorder {
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var recorder: Recorder? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var currentOutputFile: File? = null
    private var currentRecording: Recording? = null
    
    private var currentConfig: RecordingConfig? = null
    private var startTime: LocalDateTime? = null
    
    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    override val state: StateFlow<RecordingState> = _state.asStateFlow()
    
    private val _currentSession = MutableStateFlow<RecordingSession?>(null)
    override val currentSession: StateFlow<RecordingSession?> = _currentSession.asStateFlow()
    
    private val _durationSeconds = MutableStateFlow(0)
    override val durationSeconds: StateFlow<Int> = _durationSeconds.asStateFlow()
    
    private var durationUpdateJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    
    override suspend fun startRecording(config: RecordingConfig): Result<RecordingSession> {
        return try {
            _state.value = RecordingState.Preparing
            
            currentConfig = config
            val sessionId = generateSessionId()
            val outputPath = createOutputFile(sessionId).absolutePath
            currentOutputFile = File(outputPath)
            startTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            
            val session = RecordingSession(
                id = sessionId,
                startTime = startTime!!,
                config = config,
                outputPath = outputPath
            )
            _currentSession.value = session
            
            setupVideoCapture(config)
            startVideoRecording(currentOutputFile!!)
            
            startDurationTracking()
            
            _state.value = RecordingState.Recording(
                sessionId = sessionId,
                durationSeconds = 0,
                currentSegment = 1
            )
            
            Result.success(session)
        } catch (e: Exception) {
            _state.value = RecordingState.Error(e.message ?: "Failed to start recording")
            Result.failure(e)
        }
    }
    
    private fun setupVideoCapture(config: RecordingConfig) {
        val quality = when (config.quality) {
            VideoQuality.LOW -> Quality.LOWEST
            VideoQuality.MEDIUM -> Quality.SD
            VideoQuality.HIGH -> Quality.HD
            VideoQuality.VERY_HIGH -> Quality.UHD
        }
        
        val resolution = Size(config.resolution.width, config.resolution.height)
        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    resolution,
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()
        
        recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(quality))
            .build()
        
        videoCapture = VideoCapture.Builder(recorder!!)
            .setResolutionSelector(resolutionSelector)
            .build()
        
        val cameraSelector = when (config.cameraType) {
            CameraType.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            CameraType.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
        }
        
        cameraProvider?.unbindAll()
        cameraProvider?.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            videoCapture
        )
    }
    
    private fun startVideoRecording(outputFile: File) {
        val outputOptions = FileOutputOptions.Builder(outputFile).build()
        
        currentRecording = recorder!!.prepareRecording(
            context,
            outputOptions
        ).withAudioEnabled()
        .start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                }
                is VideoRecordEvent.Pause -> {
                }
                is VideoRecordEvent.Resume -> {
                }
                is VideoRecordEvent.Status -> {
                }
                is VideoRecordEvent.Finalize -> {
                    if (!event.hasError()) {
                    } else {
                        _state.value = RecordingState.Error("Recording error: ${event.cause?.message ?: "Unknown"}")
                    }
                }
            }
        }
    }
    
    override suspend fun stopRecording(): Result<String> {
        return try {
            currentRecording?.stop()
            currentRecording = null
            
            val outputPath = currentOutputFile?.absolutePath ?: ""
            
            stopDurationTracking()
            
            _state.value = RecordingState.Idle
            _currentSession.value = null
            _durationSeconds.value = 0
            
            Result.success(outputPath)
        } catch (e: Exception) {
            _state.value = RecordingState.Error(e.message ?: "Failed to stop recording")
            Result.failure(e)
        }
    }
    
    override suspend fun pauseRecording() {
        val current = _state.value
        if (current is RecordingState.Recording) {
            currentRecording?.pause()
            _state.value = RecordingState.Paused(
                sessionId = current.sessionId,
                durationSeconds = current.durationSeconds
            )
            stopDurationTracking()
        }
    }
    
    override suspend fun resumeRecording() {
        val current = _state.value
        if (current is RecordingState.Paused) {
            currentRecording?.resume()
            startDurationTracking()
            
            _state.value = RecordingState.Recording(
                sessionId = current.sessionId,
                durationSeconds = current.durationSeconds,
                currentSegment = 1
            )
        }
    }
    
    override suspend fun attachCamera(cameraService: CameraService) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        cameraProvider = providerFuture.get()
    }
    
    override fun detachCamera() {
        cameraProvider?.unbindAll()
    }
    
    private fun generateSessionId(): String {
        return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            .toString()
            .replace(":", "-")
            .replace(".", "-")
    }
    
    private fun createOutputFile(sessionId: String): File {
        val videoDir = File(context.filesDir, "videos")
        if (!videoDir.exists()) {
            videoDir.mkdirs()
        }
        
        val fileName = "${sessionId}.mp4"
        return File(videoDir, fileName)
    }
    
    private fun startDurationTracking() {
        durationUpdateJob = coroutineScope.launch {
            while (_state.value is RecordingState.Recording || _state.value is RecordingState.Paused) {
                delay(1000)
                if (_state.value is RecordingState.Recording) {
                    _durationSeconds.value += 1
                    
                    val current = _state.value
                    if (current is RecordingState.Recording) {
                        _state.value = RecordingState.Recording(
                            sessionId = current.sessionId,
                            durationSeconds = _durationSeconds.value,
                            currentSegment = current.currentSegment
                        )
                    }
                }
            }
        }
    }
    
    private fun stopDurationTracking() {
        durationUpdateJob?.cancel()
        durationUpdateJob = null
    }
    
    fun release() {
        currentRecording?.stop()
        cameraProvider?.unbindAll()
    }
}
package com.e9ab98.kmprecording.service

import com.e9ab98.kmprecording.model.CameraType
import com.e9ab98.kmprecording.model.Resolution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFoundation.*
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.Foundation.NSURL
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSFileManager
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IOSCameraService : CameraService {
    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _currentCamera = MutableStateFlow(CameraType.BACK)
    override val currentCamera: StateFlow<CameraType> = _currentCamera.asStateFlow()

    private val _currentResolution = MutableStateFlow(Resolution.FHD_1080P)
    override val currentResolution: StateFlow<Resolution> = _currentResolution.asStateFlow()

    override val availableResolutions: List<Resolution> = listOf(
        Resolution.HD_720P,
        Resolution.FHD_1080P,
        Resolution.UHD_4K
    )

    private var captureSession: AVCaptureSession? = null
    private var videoDevice: AVCaptureDevice? = null
    private var videoInput: AVCaptureDeviceInput? = null
    private var audioDevice: AVCaptureDevice? = null
    private var audioInput: AVCaptureDeviceInput? = null
    private var movieOutput: AVCaptureMovieFileOutput? = null
    private var isRecording: Boolean = false
    private var currentOutputURL: NSURL? = null
    private var recordingDelegate: RecordingDelegate? = null
    private var segmentIndex: Int = 0
    private var segmentDurationSeconds: Int = 300
    private var segmentJob: kotlinx.coroutines.Job? = null

    override suspend fun initialize() {
        withContext(Dispatchers.Main) {
            println("IOSCameraService: Starting initialization...")

            val hasCameraPermission = requestCameraPermission()
            println("IOSCameraService: Camera permission = $hasCameraPermission")

            val hasMicPermission = requestMicrophonePermission()
            println("IOSCameraService: Microphone permission = $hasMicPermission")

            if (!hasCameraPermission) {
                println("IOSCameraService: Camera permission not granted, cannot initialize")
                return@withContext
            }

            captureSession = AVCaptureSession()
            println("IOSCameraService: AVCaptureSession created")

            setupCamera(hasMicPermission)

            println("IOSCameraService: Camera setup complete, session is running: ${captureSession?.isRunning()}")
            _isReady.value = true
            println("IOSCameraService: Initialization complete, isReady = true")
        }
    }

    private suspend fun requestCameraPermission(): Boolean {
        return withContext(Dispatchers.Main) {
            val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
            println("IOSCameraService: Camera authorization status = $status")
            when (status) {
                AVAuthorizationStatusAuthorized -> true
                AVAuthorizationStatusNotDetermined -> {
                    kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                            println("IOSCameraService: Camera permission request result = $granted")
                            continuation.resume(granted)
                        }
                    }
                }
                else -> false
            }
        }
    }

    private suspend fun requestMicrophonePermission(): Boolean {
        return withContext(Dispatchers.Main) {
            val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeAudio)
            println("IOSCameraService: Microphone authorization status = $status")
            when (status) {
                AVAuthorizationStatusAuthorized -> true
                AVAuthorizationStatusNotDetermined -> {
                    kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeAudio) { granted ->
                            println("IOSCameraService: Microphone permission request result = $granted")
                            continuation.resume(granted)
                        }
                    }
                }
                else -> false
            }
        }
    }

    private fun setupCamera(hasMicPermission: Boolean) {
        val session = captureSession ?: return

        println("IOSCameraService: Setting up camera...")

        session.beginConfiguration()

        val cameraPosition = if (_currentCamera.value == CameraType.BACK) {
            AVCaptureDevicePositionBack
        } else {
            AVCaptureDevicePositionFront
        }

        println("IOSCameraService: Looking for camera at position $cameraPosition")

        val devices = AVCaptureDevice.devicesWithMediaType(AVMediaTypeVideo)
        println("IOSCameraService: Available video devices: ${devices.size}")

        videoDevice = devices
            .filter { (it as AVCaptureDevice).position == cameraPosition }
            .firstOrNull() as? AVCaptureDevice

        if (videoDevice == null) {
            println("IOSCameraService: No camera at position $cameraPosition, using default")
            videoDevice = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        }

        println("IOSCameraService: Selected video device: ${videoDevice?.localizedName}")

        videoDevice?.let { device ->
            try {
                val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null)
                if (input != null && session.canAddInput(input)) {
                    session.addInput(input)
                    videoInput = input
                    println("IOSCameraService: Video input added successfully")
                } else {
                    println("IOSCameraService: Cannot add video input to session")
                }

                val preset = when (_currentResolution.value) {
                    Resolution.HD_720P -> AVCaptureSessionPreset1280x720
                    Resolution.FHD_1080P -> AVCaptureSessionPreset1920x1080
                    Resolution.UHD_4K -> AVCaptureSessionPreset3840x2160
                    else -> AVCaptureSessionPreset1920x1080
                }

                if (session.canSetSessionPreset(preset)) {
                    session.sessionPreset = preset
                    println("IOSCameraService: Session preset set to $preset")
                } else {
                    println("IOSCameraService: Cannot set session preset to $preset")
                }
            } catch (e: Exception) {
                println("IOSCameraService: Failed to create video input: ${e.message}")
            }
        }

        if (hasMicPermission) {
            audioDevice = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeAudio)
            println("IOSCameraService: Audio device: ${audioDevice?.localizedName}")
            audioDevice?.let { device ->
                try {
                    val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null)
                    if (input != null && session.canAddInput(input)) {
                        session.addInput(input)
                        audioInput = input
                        println("IOSCameraService: Audio input added successfully")
                    } else {
                        println("IOSCameraService: Cannot add audio input to session")
                    }
                } catch (e: Exception) {
                    println("IOSCameraService: Failed to create audio input: ${e.message}")
                }
            }
        }

        movieOutput = AVCaptureMovieFileOutput()
        if (session.canAddOutput(movieOutput!!)) {
            session.addOutput(movieOutput!!)
            println("IOSCameraService: Movie output added successfully")
        } else {
            println("IOSCameraService: Cannot add movie output to session")
        }

        session.commitConfiguration()

        println("IOSCameraService: Starting capture session...")
        session.startRunning()
        println("IOSCameraService: Session running state: ${session.isRunning()}")
    }

    private fun configureCameraInput(cameraType: CameraType) {
        captureSession?.stopRunning()
        captureSession?.beginConfiguration()

        if (videoInput != null) {
            captureSession?.removeInput(videoInput!!)
        }

        val cameraPosition = if (cameraType == CameraType.BACK) {
            AVCaptureDevicePositionBack
        } else {
            AVCaptureDevicePositionFront
        }

        videoDevice = AVCaptureDevice.devicesWithMediaType(AVMediaTypeVideo)
            .filter { (it as AVCaptureDevice).position == cameraPosition }
            .firstOrNull() as? AVCaptureDevice

        videoDevice?.let { device ->
            try {
                videoInput = AVCaptureDeviceInput.deviceInputWithDevice(device, null)
                if (videoInput != null && captureSession?.canAddInput(videoInput!!) == true) {
                    captureSession?.addInput(videoInput!!)
                }
            } catch (e: Exception) {
                println("Failed to switch camera: ${e.message}")
            }
        }

        captureSession?.commitConfiguration()
        captureSession?.startRunning()
    }

    override suspend fun switchCamera() {
        val newCamera = if (_currentCamera.value == CameraType.BACK) {
            CameraType.FRONT
        } else {
            CameraType.BACK
        }
        _currentCamera.value = newCamera
        configureCameraInput(newCamera)
    }

    override suspend fun setResolution(resolution: Resolution) {
        _currentResolution.value = resolution

        val preset = when (resolution) {
            Resolution.HD_720P -> AVCaptureSessionPreset1280x720
            Resolution.FHD_1080P -> AVCaptureSessionPreset1920x1080
            Resolution.UHD_4K -> AVCaptureSessionPreset3840x2160
            else -> AVCaptureSessionPreset1920x1080
        }

        captureSession?.beginConfiguration()
        if (captureSession?.canSetSessionPreset(preset) == true) {
            captureSession?.sessionPreset = preset
        }
        captureSession?.commitConfiguration()
    }

    override suspend fun setCameraType(cameraType: CameraType) {
        if (_currentCamera.value != cameraType) {
            _currentCamera.value = cameraType
            configureCameraInput(cameraType)
        }
    }

    override fun release() {
        captureSession?.stopRunning()
        captureSession = null
        videoDevice = null
        videoInput = null
        audioDevice = null
        audioInput = null
        movieOutput = null
        _isReady.value = false
    }

    fun getCaptureSession(): AVCaptureSession? = captureSession

    override fun getPreviewView(): Any = Unit

    fun getMovieOutput(): AVCaptureMovieFileOutput? = movieOutput

    fun createRecordingDelegate(): NSObject {
        return RecordingDelegate()
    }

    fun startRecording(): String? {
        val output = movieOutput ?: return null
        if (isRecording) return null

        val url = generateOutputURL() ?: return null
        currentOutputURL = url

        val delegate = RecordingDelegate()
        recordingDelegate = delegate

        val bridge = RecordingBridge.startRecording
        if (bridge != null) {
            bridge(output, url, delegate)
            println("IOSCameraService: Started recording via bridge to $url")
        } else {
            println("IOSCameraService: ERROR - RecordingBridge not set! Call RecordingHelper.setupBridge() from Swift first.")
            return null
        }

        isRecording = true
        segmentIndex = 0
        return url.path
    }

    fun startSegmentedRecording(segmentDurationSec: Int): String? {
        val path = startRecording() ?: return null
        segmentDurationSeconds = segmentDurationSec
        startSegmentTimer()
        return path
    }

    private fun startSegmentTimer() {
        segmentJob?.cancel()
        segmentJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(segmentDurationSeconds * 1000L)
                if (isRecording) {
                    stopRecording()
                    segmentIndex++
                    startRecording()
                    println("IOSCameraService: Segment $segmentIndex started")
                }
            }
        }
    }

    fun stopSegmentedRecording(): String? {
        segmentJob?.cancel()
        segmentJob = null
        return stopRecording()
    }

    fun stopRecording(): String? {
        val output = movieOutput ?: return null
        if (!isRecording) return null

        output.stopRecording()
        isRecording = false
        val path = currentOutputURL?.path
        println("IOSCameraService: Stopped recording, file: $path")
        return path
    }

    fun getIsRecording(): Boolean = isRecording

    fun getSegmentIndex(): Int = segmentIndex

    private fun generateOutputURL(): NSURL? {
        val documentsDir = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).firstOrNull() as? String ?: return null

        val dateFormatter = NSDateFormatter()
        dateFormatter.dateFormat = "yyyyMMdd_HHmmss"
        val timestamp = dateFormatter.stringFromDate(NSDate())

        val videoDir = "$documentsDir/KMPRecording/Videos"
        NSFileManager.defaultManager.createDirectoryAtPath(
            videoDir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )

        val fileName = "VID_${timestamp}.mp4"
        val filePath = "$videoDir/$fileName"

        return NSURL.fileURLWithPath(filePath)
    }
}

@OptIn(ExperimentalForeignApi::class)
private class RecordingDelegate : NSObject(), AVCaptureFileOutputRecordingDelegateProtocol {

    override fun captureOutput(
        output: AVCaptureFileOutput,
        didFinishRecordingToOutputFileAtURL: NSURL,
        fromConnections: List<*>,
        error: NSError?
    ) {
        if (error != null) {
            println("RecordingDelegate: Recording finished with error: ${error.localizedDescription}")
        } else {
            println("RecordingDelegate: Recording finished successfully: $didFinishRecordingToOutputFileAtURL")
        }
    }

    override fun captureOutput(
        output: AVCaptureFileOutput,
        didStartRecordingToOutputFileAtURL: NSURL,
        fromConnections: List<*>
    ) {
        println("RecordingDelegate: Did start recording to $didStartRecordingToOutputFileAtURL")
    }
}

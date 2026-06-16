package com.e9ab98.kmprecording.service

import android.content.Context
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.e9ab98.kmprecording.model.CameraType
import com.e9ab98.kmprecording.model.Resolution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AndroidCameraService(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : CameraService {
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var previewView: PreviewView? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
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
    
    override suspend fun initialize() {
        try {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            cameraProvider = providerFuture.get()
            
            previewView = PreviewView(context)
            previewView?.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            
            preview = Preview.Builder()
                .setResolutionSelector(createResolutionSelector(_currentResolution.value))
                .build()
            
            preview?.setSurfaceProvider(previewView?.surfaceProvider)
            
            bindCameraUseCases()
            _isReady.value = true
        } catch (e: Exception) {
            _isReady.value = false
            e.printStackTrace()
        }
    }
    
    private fun bindCameraUseCases() {
        val cameraSelector = getCameraSelector(_currentCamera.value)
        
        cameraProvider?.unbindAll()
        camera = cameraProvider?.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview
        )
    }
    
    private fun getCameraSelector(cameraType: CameraType): CameraSelector {
        return when (cameraType) {
            CameraType.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            CameraType.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }
    
    private fun createResolutionSelector(resolution: Resolution): ResolutionSelector {
        val size = Size(resolution.width, resolution.height)
        return ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                ResolutionStrategy(
                    size,
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()
    }
    
    override suspend fun switchCamera() {
        val newCamera = if (_currentCamera.value == CameraType.BACK) {
            CameraType.FRONT
        } else {
            CameraType.BACK
        }
        setCameraType(newCamera)
    }
    
    override suspend fun setResolution(resolution: Resolution) {
        _currentResolution.value = resolution
        preview = Preview.Builder()
            .setResolutionSelector(createResolutionSelector(resolution))
            .build()
        preview?.setSurfaceProvider(previewView?.surfaceProvider)
        bindCameraUseCases()
    }
    
    override suspend fun setCameraType(cameraType: CameraType) {
        _currentCamera.value = cameraType
        bindCameraUseCases()
    }
    
    override fun release() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        _isReady.value = false
    }
    
    override fun getPreviewView(): Any {
        return previewView ?: PreviewView(context)
    }
}
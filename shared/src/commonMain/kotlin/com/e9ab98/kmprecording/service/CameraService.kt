package com.e9ab98.kmprecording.service

import com.e9ab98.kmprecording.model.CameraType
import com.e9ab98.kmprecording.model.Resolution
import kotlinx.coroutines.flow.StateFlow

interface CameraService {
    val isReady: StateFlow<Boolean>
    val currentCamera: StateFlow<CameraType>
    val currentResolution: StateFlow<Resolution>
    val availableResolutions: List<Resolution>
    
    suspend fun initialize()
    suspend fun switchCamera()
    suspend fun setResolution(resolution: Resolution)
    suspend fun setCameraType(cameraType: CameraType)
    fun release()
    
    fun getPreviewView(): Any
}
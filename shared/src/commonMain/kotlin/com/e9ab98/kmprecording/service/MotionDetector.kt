package com.e9ab98.kmprecording.service

import kotlinx.coroutines.flow.StateFlow

interface MotionDetector {
    val isMotionDetected: StateFlow<Boolean>
    val sensitivity: StateFlow<Float>
    
    suspend fun setSensitivity(level: Float)
    suspend fun startDetection()
    suspend fun stopDetection()
    
    fun attachFrameProvider(frameProvider: () -> ByteArray?)
}
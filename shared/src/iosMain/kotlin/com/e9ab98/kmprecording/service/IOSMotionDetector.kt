package com.e9ab98.kmprecording.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IOSMotionDetector(
    initialSensitivity: Float = 0.5f
) : MotionDetector {
    private val _isMotionDetected = MutableStateFlow(false)
    override val isMotionDetected: StateFlow<Boolean> = _isMotionDetected.asStateFlow()

    private val _sensitivity = MutableStateFlow(initialSensitivity)
    override val sensitivity: StateFlow<Float> = _sensitivity.asStateFlow()

    override suspend fun setSensitivity(level: Float) {
        _sensitivity.value = level.coerceIn(0.1f, 1.0f)
    }

    override suspend fun startDetection() {
        _isMotionDetected.value = false
    }

    override suspend fun stopDetection() {
        _isMotionDetected.value = false
    }

    override fun attachFrameProvider(frameProvider: () -> ByteArray?) {
    }
}
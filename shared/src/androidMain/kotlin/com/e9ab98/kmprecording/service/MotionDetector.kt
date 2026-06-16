package com.e9ab98.kmprecording.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.ByteBuffer

class AndroidMotionDetector(
    private val context: Context,
    initialSensitivity: Int = 30
) : MotionDetector {
    
    private val _isMotionDetected = MutableStateFlow(false)
    override val isMotionDetected: StateFlow<Boolean> = _isMotionDetected
    
    private val _sensitivity = MutableStateFlow(initialSensitivity.toFloat())
    override val sensitivity: StateFlow<Float> = _sensitivity
    
    private val _motionLevel = MutableStateFlow(0f)
    val motionLevel: StateFlow<Float> = _motionLevel
    
    private var previousFrame: ByteArray? = null
    private var frameWidth = 0
    private var frameHeight = 0
    private var motionThreshold = initialSensitivity
    private var cooldownFrames = 0
    private val cooldownDuration = 10
    
    private var isAnalyzing = false
    
    override suspend fun setSensitivity(level: Float) {
        _sensitivity.value = level
        motionThreshold = level.toInt()
        Log.d("MotionDetector", "灵敏度设置为: $level")
    }
    
    fun setSensitivityInt(value: Int) {
        motionThreshold = value
        _sensitivity.value = value.toFloat()
        Log.d("MotionDetector", "灵敏度设置为: $value")
    }
    
    fun createImageAnalysis(): ImageAnalysis {
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
        
        imageAnalysis.setAnalyzer(context.mainExecutor) { imageProxy ->
            analyzeFrame(imageProxy)
        }
        
        return imageAnalysis
    }
    
    private fun analyzeFrame(imageProxy: ImageProxy) {
        if (!isAnalyzing) {
            imageProxy.close()
            return
        }
        
        try {
            val buffer = imageProxy.planes[0].buffer
            val width = imageProxy.width
            val height = imageProxy.height
            
            val currentFrame = ByteArray(buffer.remaining())
            buffer.get(currentFrame)
            
            if (previousFrame == null) {
                previousFrame = currentFrame
                frameWidth = width
                frameHeight = height
                imageProxy.close()
                return
            }
            
            if (cooldownFrames > 0) {
                cooldownFrames--
                previousFrame = currentFrame
                imageProxy.close()
                return
            }
            
            val motionScore = calculateMotionScore(previousFrame!!, currentFrame, width, height)
            val normalizedScore = motionScore / (width * height * 255f)
            
            _motionLevel.value = normalizedScore
            
            val isMotionDetected = normalizedScore > (motionThreshold / 100f)
            
            if (isMotionDetected && !_isMotionDetected.value) {
                Log.d("MotionDetector", "检测到运动! 级别: ${normalizedScore}")
                _isMotionDetected.value = true
                cooldownFrames = cooldownDuration
            } else if (!isMotionDetected && _isMotionDetected.value && cooldownFrames == 0) {
                Log.d("MotionDetector", "运动停止")
                _isMotionDetected.value = false
            }
            
            previousFrame = currentFrame
        } catch (e: Exception) {
            Log.e("MotionDetector", "分析帧失败", e)
        } finally {
            imageProxy.close()
        }
    }
    
    private fun calculateMotionScore(
        previous: ByteArray,
        current: ByteArray,
        width: Int,
        height: Int
    ): Float {
        if (previous.size != current.size) return 0f
        
        var totalDiff = 0L
        val sampleStep = 4
        
        for (i in previous.indices step sampleStep) {
            if (i + 2 < previous.size && i + 2 < current.size) {
                val prevR = previous[i].toInt() and 0xFF
                val prevG = previous[i + 1].toInt() and 0xFF
                val prevB = previous[i + 2].toInt() and 0xFF
                
                val currR = current[i].toInt() and 0xFF
                val currG = current[i + 1].toInt() and 0xFF
                val currB = current[i + 2].toInt() and 0xFF
                
                val diffR = Math.abs(prevR - currR)
                val diffG = Math.abs(prevG - currG)
                val diffB = Math.abs(prevB - currB)
                
                totalDiff += (diffR + diffG + diffB)
            }
        }
        
        val sampledPixels = previous.size / sampleStep
        return totalDiff.toFloat() / sampledPixels
    }
    
    override suspend fun startDetection() {
        isAnalyzing = true
        previousFrame = null
        cooldownFrames = 0
        _isMotionDetected.value = false
        _motionLevel.value = 0f
        Log.d("MotionDetector", "开始运动检测")
    }
    
    override suspend fun stopDetection() {
        isAnalyzing = false
        previousFrame = null
        _isMotionDetected.value = false
        _motionLevel.value = 0f
        Log.d("MotionDetector", "停止运动检测")
    }
    
    fun startDetectionImmediate() {
        isAnalyzing = true
        previousFrame = null
        cooldownFrames = 0
        _isMotionDetected.value = false
        _motionLevel.value = 0f
        Log.d("MotionDetector", "开始运动检测")
    }
    
    fun stopDetectionImmediate() {
        isAnalyzing = false
        previousFrame = null
        _isMotionDetected.value = false
        _motionLevel.value = 0f
        Log.d("MotionDetector", "停止运动检测")
    }
    
    fun reset() {
        previousFrame = null
        cooldownFrames = 0
        _isMotionDetected.value = false
        _motionLevel.value = 0f
    }
    
    override fun attachFrameProvider(frameProvider: () -> ByteArray?) {
    }
}
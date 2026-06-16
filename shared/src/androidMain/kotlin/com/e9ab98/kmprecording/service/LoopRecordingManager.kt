package com.e9ab98.kmprecording.service

import android.content.Context
import android.util.Log
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File

class LoopRecordingManager(
    private val context: Context,
    private val storageService: AndroidStorageService
) {
    private val _isLoopRecording = MutableStateFlow(false)
    val isLoopRecording: StateFlow<Boolean> = _isLoopRecording.asStateFlow()
    
    private val _currentSegmentIndex = MutableStateFlow(0)
    val currentSegmentIndex: StateFlow<Int> = _currentSegmentIndex.asStateFlow()
    
    private val _totalDurationSeconds = MutableStateFlow(0)
    val totalDurationSeconds: StateFlow<Int> = _totalDurationSeconds.asStateFlow()
    
    private var recordingJob: Job? = null
    private var currentRecording: Recording? = null
    private var segmentStartTime: Long = 0
    
    private val videoDir = File(context.filesDir, "videos")
    
    fun startLoopRecording(
        recorder: Recorder,
        segmentDurationSeconds: Int = 300,
        maxStorageMB: Long = 1024,
        enableAudio: Boolean = true,
        scope: CoroutineScope
    ) {
        _isLoopRecording.value = true
        _currentSegmentIndex.value = 0
        _totalDurationSeconds.value = 0
        
        recordingJob = scope.launch {
            while (_isLoopRecording.value) {
                cleanupOldVideosIfNeeded(maxStorageMB)
                
                val segmentFile = createSegmentFile()
                val outputOptions = FileOutputOptions.Builder(segmentFile).build()
                
                currentRecording = recorder
                    .prepareRecording(context, outputOptions)
                    .let { 
                        if (enableAudio) it.withAudioEnabled() else it 
                    }
                    .start(ContextCompat.getMainExecutor(context)) { event ->
                        when (event) {
                            is VideoRecordEvent.Start -> {
                                segmentStartTime = System.currentTimeMillis()
                                Log.d("LoopRecording", "开始录制片段 ${_currentSegmentIndex.value}")
                            }
                            is VideoRecordEvent.Finalize -> {
                                if (!event.hasError()) {
                                    Log.d("LoopRecording", "片段 ${_currentSegmentIndex.value} 完成")
                                    storageService.loadVideoRecords()
                                } else {
                                    Log.e("LoopRecording", "片段录制失败: ${event.cause}")
                                }
                            }
                            else -> {}
                        }
                    }
                
                delay(segmentDurationSeconds * 1000L)
                
                currentRecording?.stop()
                currentRecording = null
                
                _currentSegmentIndex.value++
                _totalDurationSeconds.value += segmentDurationSeconds
            }
        }
    }
    
    fun stopLoopRecording() {
        _isLoopRecording.value = false
        recordingJob?.cancel()
        recordingJob = null
        
        currentRecording?.stop()
        currentRecording = null
        
        _currentSegmentIndex.value = 0
        _totalDurationSeconds.value = 0
    }
    
    private fun createSegmentFile(): File {
        if (!videoDir.exists()) videoDir.mkdirs()
        
        val timestamp = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .toString()
            .replace(":", "-")
            .replace(".", "-")
        
        return File(videoDir, "loop_${timestamp}_seg${_currentSegmentIndex.value}.mp4")
    }
    
    private suspend fun cleanupOldVideosIfNeeded(maxStorageMB: Long) {
        val usedMB = storageService.usedSpaceMB.value
        if (usedMB > maxStorageMB * 0.9) {
            Log.d("LoopRecording", "存储空间不足，清理旧视频")
            storageService.cleanupOldVideos(maxStorageMB)
        }
    }
    
    fun release() {
        stopLoopRecording()
    }
}
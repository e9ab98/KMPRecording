package com.e9ab98.kmprecording.service

import com.e9ab98.kmprecording.model.RecordingConfig
import com.e9ab98.kmprecording.model.RecordingSessionInfo
import com.e9ab98.kmprecording.model.SegmentRecordingConfig
import com.e9ab98.kmprecording.model.VideoSegment
import kotlinx.coroutines.flow.StateFlow

interface SegmentRecorder {
    val segmentConfig: SegmentRecordingConfig
    val currentSession: StateFlow<RecordingSessionInfo?>
    val currentSegmentIndex: StateFlow<Int>
    val isRecording: StateFlow<Boolean>
    val recordingDurationSeconds: StateFlow<Int>
    
    suspend fun startSegmentedRecording(config: RecordingConfig): Result<String>
    suspend fun stopSegmentedRecording(): Result<List<VideoSegment>>
    suspend fun emergencyLock(): Result<List<String>>
    
    fun getSegments(sessionId: String): List<VideoSegment>
    fun getAllSegments(): List<VideoSegment>
    fun deleteSegment(segmentId: String): Result<Boolean>
    fun lockSegment(segmentId: String): Result<Boolean>
    fun unlockSegment(segmentId: String): Result<Boolean>
    
    fun getStorageInfo(): StorageInfo
    fun cleanupOldSegments(): Result<Int>
}

data class StorageInfo(
    val totalStorageMB: Long,
    val usedStorageMB: Long,
    val availableStorageMB: Long,
    val segmentCount: Int,
    val lockedSegmentCount: Int
)
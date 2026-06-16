package com.e9ab98.kmprecording.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import com.e9ab98.kmprecording.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class AndroidSegmentRecorder(
    private val context: Context,
    override val segmentConfig: SegmentRecordingConfig
) : SegmentRecorder {
    
    private val _currentSession = MutableStateFlow<RecordingSessionInfo?>(null)
    private val _currentSegmentIndex = MutableStateFlow(0)
    private val _isRecording = MutableStateFlow(false)
    private val _recordingDurationSeconds = MutableStateFlow(0)
    
    override val currentSession: StateFlow<RecordingSessionInfo?> = _currentSession
    override val currentSegmentIndex: StateFlow<Int> = _currentSegmentIndex
    override val isRecording: StateFlow<Boolean> = _isRecording
    override val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds
    
    private var mediaRecorder: MediaRecorder? = null
    private var recordingScope: CoroutineScope? = null
    private var segmentTimer: Job? = null
    private var durationTimer: Job? = null
    
    private val segmentsDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
        "KMPRecording/Segments"
    )
    
    private val allSegments = mutableListOf<VideoSegment>()
    
    init {
        segmentsDir.mkdirs()
        loadExistingSegments()
    }
    
    override suspend fun startSegmentedRecording(config: RecordingConfig): Result<String> {
        return try {
            val sessionId = generateSessionId()
            val sessionDir = createSessionDir(sessionId)
            
            _isRecording.value = true
            _currentSegmentIndex.value = 0
            _recordingDurationSeconds.value = 0
            
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val sessionInfo = RecordingSessionInfo(
                sessionId = sessionId,
                startTime = now,
                config = config,
                segments = emptyList(),
                totalDurationSeconds = 0,
                totalSizeMB = 0,
                isActive = true
            )
            _currentSession.value = sessionInfo
            
            recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            
            startNewSegment(sessionId, sessionDir, config)
            startSegmentTimer(sessionId, sessionDir, config)
            startDurationTimer()
            
            Result.success(sessionId)
        } catch (e: Exception) {
            _isRecording.value = false
            Result.failure(e)
        }
    }
    
    override suspend fun stopSegmentedRecording(): Result<List<VideoSegment>> {
        return try {
            segmentTimer?.cancel()
            durationTimer?.cancel()
            
            stopCurrentSegment()
            
            _isRecording.value = false
            recordingScope?.cancel()
            
            val segments = allSegments.filter { it.sessionId == _currentSession.value?.sessionId }
            
            _currentSession.value = _currentSession.value?.copy(
                isActive = false,
                segments = segments,
                totalDurationSeconds = segments.sumOf { it.durationSeconds },
                totalSizeMB = segments.sumOf { it.fileSizeMB }
            )
            
            Result.success(segments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun emergencyLock(): Result<List<String>> {
        return try {
            val sessionId = _currentSession.value?.sessionId ?: return Result.failure(Exception("No active session"))
            
            val lockedIds = mutableListOf<String>()
            val currentIndex = _currentSegmentIndex.value
            
            allSegments.filter { it.sessionId == sessionId }
                .forEach { segment ->
                    if (segment.segmentIndex >= currentIndex - 2 || segment.segmentIndex == currentIndex) {
                        val lockedSegment = segment.copy(
                            isLocked = true,
                            isEmergency = true
                        )
                        allSegments[allSegments.indexOf(segment)] = lockedSegment
                        lockedIds.add(segment.id)
                    }
                }
            
            Result.success(lockedIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun startNewSegment(sessionId: String, sessionDir: File, config: RecordingConfig) {
        val segmentIndex = _currentSegmentIndex.value
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "seg_${segmentIndex}_${timestamp}.mp4"
        val segmentFile = File(sessionDir, fileName)
        
        mediaRecorder = MediaRecorder(context).apply {
            setVideoSource(MediaRecorder.VideoSource.CAMERA)
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoSize(config.resolution.width, config.resolution.height)
            setVideoEncodingBitRate(config.quality.bitrate)
            setOutputFile(segmentFile.absolutePath)
            prepare()
            start()
        }
        
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val segment = VideoSegment(
            id = "seg_${sessionId}_${segmentIndex}",
            sessionId = sessionId,
            segmentIndex = segmentIndex,
            startTime = now,
            endTime = now,
            durationSeconds = 0,
            filePath = segmentFile.absolutePath,
            fileSizeBytes = 0
        )
        allSegments.add(segment)
    }
    
    private fun stopCurrentSegment() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        
        val currentSegment = allSegments.lastOrNull()
        if (currentSegment != null && currentSegment.filePath.isNotEmpty()) {
            val file = File(currentSegment.filePath)
            if (file.exists()) {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val updatedSegment = currentSegment.copy(
                    endTime = now,
                    durationSeconds = segmentConfig.segmentDurationSeconds,
                    fileSizeBytes = file.length()
                )
                allSegments[allSegments.indexOf(currentSegment)] = updatedSegment
            }
        }
    }
    
    private fun startSegmentTimer(sessionId: String, sessionDir: File, config: RecordingConfig) {
        segmentTimer = recordingScope?.launch {
            while (_isRecording.value) {
                delay(segmentConfig.segmentDurationSeconds * 1000L)
                
                if (_isRecording.value) {
                    stopCurrentSegment()
                    
                    if (segmentConfig.enableLoopRecording) {
                        cleanupOldSegments()
                    }
                    
                    _currentSegmentIndex.value++
                    startNewSegment(sessionId, sessionDir, config)
                }
            }
        }
    }
    
    private fun startDurationTimer() {
        durationTimer = recordingScope?.launch {
            while (_isRecording.value) {
                delay(1000L)
                _recordingDurationSeconds.value++
            }
        }
    }
    
    override fun getSegments(sessionId: String): List<VideoSegment> {
        return allSegments.filter { it.sessionId == sessionId }
    }
    
    override fun getAllSegments(): List<VideoSegment> {
        return allSegments.toList()
    }
    
    override fun deleteSegment(segmentId: String): Result<Boolean> {
        return try {
            val segment = allSegments.find { it.id == segmentId }
            if (segment == null) return Result.failure(Exception("Segment not found"))
            if (segment.isLocked) return Result.failure(Exception("Segment is locked"))
            
            val file = File(segment.filePath)
            if (file.exists()) file.delete()
            
            allSegments.remove(segment)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun lockSegment(segmentId: String): Result<Boolean> {
        return try {
            val segment = allSegments.find { it.id == segmentId }
            if (segment == null) return Result.failure(Exception("Segment not found"))
            
            val lockedSegment = segment.copy(isLocked = true)
            allSegments[allSegments.indexOf(segment)] = lockedSegment
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun unlockSegment(segmentId: String): Result<Boolean> {
        return try {
            val segment = allSegments.find { it.id == segmentId }
            if (segment == null) return Result.failure(Exception("Segment not found"))
            
            val unlockedSegment = segment.copy(isLocked = false, isEmergency = false)
            allSegments[allSegments.indexOf(segment)] = unlockedSegment
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getStorageInfo(): StorageInfo {
        val totalSize = allSegments.sumOf { it.fileSizeMB }
        val lockedCount = allSegments.count { it.isLocked }
        
        return StorageInfo(
            totalStorageMB = segmentConfig.maxStorageMB,
            usedStorageMB = totalSize,
            availableStorageMB = segmentConfig.maxStorageMB - totalSize,
            segmentCount = allSegments.size,
            lockedSegmentCount = lockedCount
        )
    }
    
    override fun cleanupOldSegments(): Result<Int> {
        return try {
            val storageInfo = getStorageInfo()
            if (storageInfo.usedStorageMB >= segmentConfig.maxStorageMB) {
                val unlockedSegments = allSegments
                    .filter { !it.isLocked }
                    .sortedBy { it.startTime }
                
                var deletedCount = 0
                for (segment in unlockedSegments) {
                    if (getStorageInfo().usedStorageMB < segmentConfig.maxStorageMB * 0.8) break
                    
                    deleteSegment(segment.id)
                    deletedCount++
                }
                
                Result.success(deletedCount)
            } else {
                Result.success(0)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun generateSessionId(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "session_${timestamp}_${Random.nextInt(1000, 9999)}"
    }
    
    private fun createSessionDir(sessionId: String): File {
        val dateDir = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val sessionDir = File(segmentsDir, "$dateDir/$sessionId")
        sessionDir.mkdirs()
        return sessionDir
    }
    
    private fun loadExistingSegments() {
        segmentsDir.walkTopDown()
            .filter { it.isFile && it.extension == "mp4" }
            .forEach { file ->
                val pathParts = file.relativeTo(segmentsDir).path.split("/")
                if (pathParts.size >= 3) {
                    val sessionId = pathParts[1]
                    val fileName = pathParts[2]
                    val segmentIndex = fileName.split("_")[1].toIntOrNull() ?: 0
                    
                    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    val segment = VideoSegment(
                        id = "seg_${sessionId}_${segmentIndex}",
                        sessionId = sessionId,
                        segmentIndex = segmentIndex,
                        startTime = now,
                        endTime = now,
                        durationSeconds = segmentConfig.segmentDurationSeconds,
                        filePath = file.absolutePath,
                        fileSizeBytes = file.length()
                    )
                    allSegments.add(segment)
                }
            }
    }
}
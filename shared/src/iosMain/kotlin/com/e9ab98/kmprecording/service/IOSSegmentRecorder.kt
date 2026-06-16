package com.e9ab98.kmprecording.service

import com.e9ab98.kmprecording.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.CoreFoundation.kCFAbsoluteTimeIntervalSince1970
import platform.Foundation.*
import kotlin.random.Random

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class IOSSegmentRecorder(
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
    
    private var recordingScope: CoroutineScope? = null
    private var segmentTimer: Job? = null
    private var durationTimer: Job? = null
    
    private val allSegments = mutableListOf<VideoSegment>()
    
    private val dateFormatter = NSDateFormatter()
    
    init {
        dateFormatter.dateFormat = "yyyyMMdd_HHmmss"
        loadExistingSegments()
    }
    
    private fun getCurrentLocalDateTime(): LocalDateTime {
        val timeMillis = (NSDate().timeIntervalSinceReferenceDate + kCFAbsoluteTimeIntervalSince1970).toLong() * 1000
        val instant = Instant.fromEpochMilliseconds(timeMillis)
        return instant.toLocalDateTime(TimeZone.currentSystemDefault())
    }
    
    override suspend fun startSegmentedRecording(config: RecordingConfig): Result<String> {
        return try {
            val sessionId = generateSessionId()
            
            _isRecording.value = true
            _currentSegmentIndex.value = 0
            _recordingDurationSeconds.value = 0
            
            val now = getCurrentLocalDateTime()
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
            
            recordingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            
            startSegmentTimer(sessionId, config)
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
            val sessionId = _currentSession.value?.sessionId 
                ?: return Result.failure(Exception("No active session"))
            
            val lockedIds = mutableListOf<String>()
            val currentIndex = _currentSegmentIndex.value
            
            allSegments.filter { it.sessionId == sessionId }
                .forEach { segment ->
                    if (segment.segmentIndex >= currentIndex - 2 || 
                        segment.segmentIndex == currentIndex) {
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
    
    private fun startSegmentTimer(sessionId: String, config: RecordingConfig) {
        segmentTimer = recordingScope?.launch {
            while (_isRecording.value) {
                val segmentIndex = _currentSegmentIndex.value
                val timestamp = dateFormatter.stringFromDate(NSDate())
                val fileName = "seg_${segmentIndex}_${timestamp}.mp4"
                val filePath = createSegmentPath(sessionId, fileName)
                
                val now = getCurrentLocalDateTime()
                val segment = VideoSegment(
                    id = "seg_${sessionId}_${segmentIndex}",
                    sessionId = sessionId,
                    segmentIndex = segmentIndex,
                    startTime = now,
                    endTime = now,
                    durationSeconds = segmentConfig.segmentDurationSeconds,
                    filePath = filePath,
                    fileSizeBytes = 10L * 1024L * 1024L
                )
                allSegments.add(segment)
                
                if (segmentConfig.enableLoopRecording) {
                    cleanupOldSegments()
                }
                
                _currentSegmentIndex.value++
                
                kotlinx.coroutines.delay(segmentConfig.segmentDurationSeconds * 1000L)
            }
        }
    }
    
    private fun startDurationTimer() {
        durationTimer = recordingScope?.launch {
            while (_isRecording.value) {
                kotlinx.coroutines.delay(1000L)
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
            
            val fileManager = NSFileManager.defaultManager
            fileManager.removeItemAtPath(segment.filePath, null)
            
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
        val timestamp = dateFormatter.stringFromDate(NSDate())
        return "session_${timestamp}_${Random.nextInt(1000, 9999)}"
    }
    
    private fun createSegmentPath(sessionId: String, fileName: String): String {
        val documentsDir = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).firstOrNull() as? String ?: ""
        
        val dateDir = NSDateFormatter().apply {
            dateFormat = "yyyyMMdd"
        }.stringFromDate(NSDate())
        
        val segmentsDir = "$documentsDir/KMPRecording/Segments/$dateDir/$sessionId"
        
        val fileManager = NSFileManager.defaultManager
        fileManager.createDirectoryAtPath(
            segmentsDir, 
            withIntermediateDirectories = true, 
            attributes = null, 
            error = null
        )
        
        return "$segmentsDir/$fileName"
    }
    
    private fun loadExistingSegments() {
        val documentsDir = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).firstOrNull() as? String ?: ""
        
        val segmentsRoot = "$documentsDir/KMPRecording/Segments"
        val fileManager = NSFileManager.defaultManager
        
        if (!fileManager.fileExistsAtPath(segmentsRoot)) return
        
        val contents = fileManager.contentsOfDirectoryAtPath(segmentsRoot, null)
            ?: return
        
        (contents as List<Any?>).forEach { dateDir ->
            val datePath = "$segmentsRoot/$dateDir"
            val sessionContents = fileManager.contentsOfDirectoryAtPath(datePath, null)
                ?: return
            
            (sessionContents as List<Any?>).forEach { sessionId ->
                val sessionPath = "$datePath/$sessionId"
                val segmentContents = fileManager.contentsOfDirectoryAtPath(sessionPath, null)
                    ?: return
                
                (segmentContents as List<Any?>).forEach { fileName ->
                    val fileNameStr = fileName as? String ?: return
                    if (fileNameStr != null && fileNameStr.endsWith(".mp4")) {
                        val parts = fileNameStr.split("_")
                        val segmentIndex = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        val filePath = "$sessionPath/$fileNameStr"
                        
                        val attributes = fileManager.attributesOfItemAtPath(filePath, null)
                        val fileSize = (attributes?.get("NSFileSize") as? NSNumber)?.longLongValue ?: 0L
                        
                        val now = kotlinx.datetime.LocalDateTime(2025, 1, 1, 0, 0, 0)
                        val segment = VideoSegment(
                            id = "seg_${sessionId}_${segmentIndex}",
                            sessionId = sessionId as String,
                            segmentIndex = segmentIndex,
                            startTime = now,
                            endTime = now,
                            durationSeconds = segmentConfig.segmentDurationSeconds,
                            filePath = filePath,
                            fileSizeBytes = fileSize
                        )
                        allSegments.add(segment)
                    }
                }
            }
        }
    }
}

actual fun createSegmentRecorder(
    config: SegmentRecordingConfig
): SegmentRecorder {
    return IOSSegmentRecorder(config)
}
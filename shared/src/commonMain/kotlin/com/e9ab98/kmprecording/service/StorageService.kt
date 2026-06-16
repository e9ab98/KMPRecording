package com.e9ab98.kmprecording.service

import com.e9ab98.kmprecording.model.VideoRecord
import com.e9ab98.kmprecording.model.VideoSegment
import kotlinx.coroutines.flow.StateFlow

interface StorageService {
    val availableSpaceMB: StateFlow<Long>
    val totalSpaceMB: StateFlow<Long>
    val usedSpaceMB: StateFlow<Long>
    
    suspend fun getVideoRecords(): List<VideoRecord>
    suspend fun getVideoSegments(sessionId: String): List<VideoSegment>
    suspend fun deleteVideo(videoId: String): Result<Boolean>
    suspend fun deleteSession(sessionId: String): Result<Boolean>
    
    suspend fun saveVideoRecord(record: VideoRecord): Result<VideoRecord>
    suspend fun saveVideoSegment(segment: VideoSegment): Result<VideoSegment>
    
    suspend fun generateThumbnail(videoPath: String): Result<String>
    
    suspend fun cleanupOldVideos(maxStorageMB: Long): Result<Int>
    
    fun getVideoFilePath(): String
    fun getSessionFolderPath(sessionId: String): String
}
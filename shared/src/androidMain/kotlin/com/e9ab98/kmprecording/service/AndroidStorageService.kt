package com.e9ab98.kmprecording.service

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.e9ab98.kmprecording.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File

class AndroidStorageService(private val context: Context) : StorageService {
    
    private val videoDir = File(context.filesDir, "videos")
    private val thumbnailDir = File(context.filesDir, "thumbnails")
    
    private val videoRecordCache = mutableMapOf<String, CachedVideoRecord>()

    private data class CachedVideoRecord(
        val lastModified: Long,
        val fileSizeBytes: Long,
        val record: VideoRecord
    )

    init {
        if (!videoDir.exists()) videoDir.mkdirs()
        if (!thumbnailDir.exists()) thumbnailDir.mkdirs()
    }
    
    private val _availableSpaceMB = MutableStateFlow(0L)
    override val availableSpaceMB: StateFlow<Long> = _availableSpaceMB.asStateFlow()
    
    private val _totalSpaceMB = MutableStateFlow(0L)
    override val totalSpaceMB: StateFlow<Long> = _totalSpaceMB.asStateFlow()
    
    private val _usedSpaceMB = MutableStateFlow(0L)
    override val usedSpaceMB: StateFlow<Long> = _usedSpaceMB.asStateFlow()
    
    private val _availableSpaceBytes = MutableStateFlow(0L)
    val availableSpaceBytes: StateFlow<Long> = _availableSpaceBytes.asStateFlow()
    
    private val _usedSpaceBytes = MutableStateFlow(0L)
    val usedSpaceBytes: StateFlow<Long> = _usedSpaceBytes.asStateFlow()
    
    private val _videoRecords = MutableStateFlow<List<VideoRecord>>(emptyList())
    val videoRecords: StateFlow<List<VideoRecord>> = _videoRecords.asStateFlow()
    
    fun updateStorageInfo() {
        val stat = android.os.StatFs(videoDir.absolutePath)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val usedBytes = calculateUsedSpace()
        
        _availableSpaceMB.value = availableBytes / (1024 * 1024)
        _totalSpaceMB.value = totalBytes / (1024 * 1024)
        _usedSpaceMB.value = usedBytes / (1024 * 1024)
        _availableSpaceBytes.value = availableBytes
        _usedSpaceBytes.value = usedBytes
    }
    
    fun loadVideoRecords() {
        _videoRecords.value = loadVideoRecordsInternal().sortedByDescending { it.createdAt }
    }
    
    private fun calculateUsedSpace(): Long {
        return videoDir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }
    
    private fun loadVideoRecordsInternal(): List<VideoRecord> {
        val records = mutableListOf<VideoRecord>()
        val currentFileNames = mutableSetOf<String>()
        
        videoDir.walkTopDown()
            .filter { it.isFile && it.extension == "mp4" }
            .forEach { file ->
                val fileName = file.name
                currentFileNames.add(fileName)
                
                val lastModified = file.lastModified()
                val fileSizeBytes = file.length()
                
                val cached = synchronized(videoRecordCache) { videoRecordCache[fileName] }
                if (cached != null && cached.lastModified == lastModified && cached.fileSizeBytes == fileSizeBytes) {
                    records.add(cached.record)
                } else {
                    try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(file.absolutePath)
                        
                        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        val durationSeconds = (durationStr?.toIntOrNull() ?: 0) / 1000
                        
                        val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                        val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                        
                        val resolution = determineResolution(widthStr?.toIntOrNull(), heightStr?.toIntOrNull())
                        
                        retriever.release()
                        
                        val record = VideoRecord(
                            id = file.nameWithoutExtension,
                            filePath = file.absolutePath,
                            fileName = file.name,
                            durationSeconds = durationSeconds,
                            fileSizeBytes = fileSizeBytes,
                            createdAt = parseDateTimeFromFileName(file.nameWithoutExtension),
                            resolution = resolution,
                            cameraType = CameraType.BACK
                        )
                        synchronized(videoRecordCache) {
                            videoRecordCache[fileName] = CachedVideoRecord(lastModified, fileSizeBytes, record)
                        }
                        records.add(record)
                    } catch (e: Exception) {
                    }
                }
            }
            
        synchronized(videoRecordCache) {
            val cachedKeysIterator = videoRecordCache.keys.iterator()
            while (cachedKeysIterator.hasNext()) {
                val key = cachedKeysIterator.next()
                if (!currentFileNames.contains(key)) {
                    cachedKeysIterator.remove()
                }
            }
        }
        
        return records
    }
    
    override suspend fun getVideoRecords(): List<VideoRecord> {
        return _videoRecords.value
    }
    
    override suspend fun getVideoSegments(sessionId: String): List<VideoSegment> {
        return emptyList()
    }
    
    override suspend fun deleteVideo(videoId: String): Result<Boolean> {
        return try {
            val file = File(videoDir, "$videoId.mp4")
            if (file.exists()) {
                file.delete()
                
                val srtFile = File(videoDir, "$videoId.srt")
                if (srtFile.exists()) {
                    srtFile.delete()
                }
                
                val thumbnailFile = File(thumbnailDir, "${videoId}_thumb.jpg")
                if (thumbnailFile.exists()) {
                    thumbnailFile.delete()
                }
                
                updateStorageInfo()
                loadVideoRecords()
                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteSession(sessionId: String): Result<Boolean> {
        return deleteVideo(sessionId)
    }
    
    override suspend fun saveVideoRecord(record: VideoRecord): Result<VideoRecord> {
        return Result.success(record)
    }
    
    override suspend fun saveVideoSegment(segment: VideoSegment): Result<VideoSegment> {
        return Result.success(segment)
    }
    
    override suspend fun generateThumbnail(videoPath: String): Result<String> {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            
            val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            
            if (bitmap != null) {
                val fileName = File(videoPath).nameWithoutExtension
                val thumbnailFile = File(thumbnailDir, "${fileName}_thumb.jpg")
                thumbnailFile.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                bitmap.recycle()
                Result.success(thumbnailFile.absolutePath)
            } else {
                Result.failure(Exception("Failed to generate thumbnail"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun cleanupOldVideos(maxStorageMB: Long): Result<Int> {
        return try {
            var deletedCount = 0
            val maxBytes = maxStorageMB * 1024 * 1024
            
            val videos = getVideoRecords().sortedBy { it.createdAt }
            
            while (calculateUsedSpace() > maxBytes && videos.isNotEmpty()) {
                val oldestVideo = videos.first()
                deleteVideo(oldestVideo.id)
                deletedCount++
            }
            
            updateStorageInfo()
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getVideoFilePath(): String {
        val timestamp = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .toString()
            .replace(":", "-")
            .replace(".", "-")
        
        return File(videoDir, "${timestamp}.mp4").absolutePath
    }
    
    override fun getSessionFolderPath(sessionId: String): String {
        return File(videoDir, sessionId).absolutePath
    }
    
    private fun determineResolution(width: Int?, height: Int?): Resolution {
        return when {
            width == null || height == null -> Resolution.FHD_1080P
            width >= 3840 || height >= 2160 -> Resolution.UHD_4K
            width >= 1920 || height >= 1080 -> Resolution.FHD_1080P
            else -> Resolution.HD_720P
        }
    }
    
    private fun parseDateTimeFromFileName(fileName: String): LocalDateTime {
        return try {
            val parts = fileName.split("_")
            if (parts.isNotEmpty()) {
                val dateStr = parts[0].replace("-", ":")
                LocalDateTime.parse(dateStr)
            } else {
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            }
        } catch (e: Exception) {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        }
    }
}

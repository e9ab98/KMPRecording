package com.e9ab98.kmprecording.service

import com.e9ab98.kmprecording.model.CameraType
import com.e9ab98.kmprecording.model.Resolution
import com.e9ab98.kmprecording.model.VideoRecord
import com.e9ab98.kmprecording.model.VideoSegment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSNumber
import kotlinx.datetime.LocalDateTime

@OptIn(ExperimentalForeignApi::class)
class IOSStorageService : StorageService {
    private val _availableSpaceMB = MutableStateFlow(10240L)
    override val availableSpaceMB: StateFlow<Long> = _availableSpaceMB.asStateFlow()

    private val _totalSpaceMB = MutableStateFlow(102400L)
    override val totalSpaceMB: StateFlow<Long> = _totalSpaceMB.asStateFlow()

    private val _usedSpaceMB = MutableStateFlow(0L)
    override val usedSpaceMB: StateFlow<Long> = _usedSpaceMB.asStateFlow()

    private val videoDir: String by lazy {
        val documentsDir = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).firstOrNull() as? String ?: ""
        "$documentsDir/KMPRecording/Videos"
    }

    init {
        updateStorageInfo()
    }

    private fun updateStorageInfo() {
        val fileManager = NSFileManager.defaultManager

        try {
            if (videoDir.isNotEmpty() && fileManager.fileExistsAtPath(videoDir)) {
                val attrs = fileManager.attributesOfFileSystemForPath(videoDir, error = null)
                val totalSpace = (attrs?.get("NSFileSystemSize") as? NSNumber)?.longLongValue ?: 102400L
                val freeSpace = (attrs?.get("NSFileSystemFreeSize") as? NSNumber)?.longLongValue ?: 10240L

                _totalSpaceMB.value = totalSpace / (1024 * 1024)
                _availableSpaceMB.value = freeSpace / (1024 * 1024)
            }
        } catch (e: Exception) {
            println("IOSStorageService: updateStorageInfo error: ${e.message}")
        }

        var usedBytes = 0L
        try {
            if (videoDir.isNotEmpty() && fileManager.fileExistsAtPath(videoDir)) {
                val files = fileManager.contentsOfDirectoryAtPath(videoDir, error = null)
                files?.forEach { file ->
                    val filePath = "$videoDir/$file"
                    val attrs = fileManager.attributesOfItemAtPath(filePath, error = null)
                    val size = (attrs?.get("NSFileSize") as? NSNumber)?.longLongValue ?: 0L
                    usedBytes += size
                }
            }
        } catch (e: Exception) {
            println("IOSStorageService: updateStorageInfo usedSpace error: ${e.message}")
        }
        _usedSpaceMB.value = usedBytes / (1024 * 1024)
    }

    override suspend fun getVideoRecords(): List<VideoRecord> {
        val fileManager = NSFileManager.defaultManager

        if (videoDir.isEmpty() || !fileManager.fileExistsAtPath(videoDir)) {
            return emptyList()
        }

        val files = fileManager.contentsOfDirectoryAtPath(videoDir, error = null)
            ?: return emptyList()

        val records = mutableListOf<VideoRecord>()

        files.filter { (it as? String)?.endsWith(".mp4") == true }.forEach { fileObj ->
            try {
                val fileName = fileObj as String
                val filePath = "$videoDir/$fileName"

                val attrs = fileManager.attributesOfItemAtPath(filePath, error = null)
                val fileSize = (attrs?.get("NSFileSize") as? NSNumber)?.longLongValue ?: 0L

                // 文件名格式: VID_20260603_104305.mp4
                val timestampStr = fileName.removePrefix("VID_").removeSuffix(".mp4")

                val localDateTime = try {
                    // 按 "_" 分割: ["20260603", "104305"]
                    val parts = timestampStr.split("_")
                    val datePart = parts[0]  // "20260603"
                    val timePart = if (parts.size > 1) parts[1] else "000000"

                    val year = datePart.substring(0, 4).toIntOrNull() ?: 2026
                    val month = datePart.substring(4, 6).toIntOrNull() ?: 1
                    val day = datePart.substring(6, 8).toIntOrNull() ?: 1
                    val hour = timePart.substring(0, 2).toIntOrNull() ?: 0
                    val minute = timePart.substring(2, 4).toIntOrNull() ?: 0
                    val second = timePart.substring(4, 6).toIntOrNull() ?: 0

                    LocalDateTime(year, month, day, hour, minute, second)
                } catch (e: Exception) {
                    println("IOSStorageService: Failed to parse timestamp '$timestampStr': ${e.message}")
                    LocalDateTime(2026, 1, 1, 0, 0, 0)
                }

                val durationSec = estimateDuration(fileSize)

                records.add(
                    VideoRecord(
                        id = fileName.removeSuffix(".mp4"),
                        fileName = fileName,
                        filePath = filePath,
                        fileSizeBytes = fileSize,
                        createdAt = localDateTime,
                        thumbnailPath = null,
                        resolution = Resolution.FHD_1080P,
                        cameraType = CameraType.BACK,
                        isSegment = false,
                        sessionId = null,
                        durationSeconds = durationSec
                    )
                )
            } catch (e: Exception) {
                println("IOSStorageService: Failed to process file: ${e.message}")
            }
        }

        updateStorageInfo()

        return records.sortedByDescending { it.fileName }
    }

    private fun estimateDuration(fileSizeBytes: Long): Int {
        val bitrate = 5_000_000L
        return (fileSizeBytes * 8 / bitrate).toInt().coerceIn(0, 36000)
    }

    override suspend fun getVideoSegments(sessionId: String): List<VideoSegment> {
        return emptyList()
    }

    override suspend fun deleteVideo(videoId: String): Result<Boolean> {
        val fileManager = NSFileManager.defaultManager
        val filePath = "$videoDir/$videoId.mp4"
        val srtPath = "$videoDir/$videoId.srt"

        var deleted = false
        if (fileManager.fileExistsAtPath(filePath)) {
            deleted = fileManager.removeItemAtPath(filePath, error = null)
        }
        if (fileManager.fileExistsAtPath(srtPath)) {
            fileManager.removeItemAtPath(srtPath, error = null)
        }

        if (deleted) {
            updateStorageInfo()
        }
        return Result.success(deleted)
    }

    override suspend fun deleteSession(sessionId: String): Result<Boolean> {
        return Result.success(true)
    }

    override suspend fun saveVideoRecord(record: VideoRecord): Result<VideoRecord> {
        return Result.success(record)
    }

    override suspend fun saveVideoSegment(segment: VideoSegment): Result<VideoSegment> {
        return Result.success(segment)
    }

    override suspend fun generateThumbnail(videoPath: String): Result<String> {
        return Result.success("")
    }

    override suspend fun cleanupOldVideos(maxStorageMB: Long): Result<Int> {
        var deletedCount = 0
        val records = getVideoRecords().sortedBy { it.fileName }

        while (_usedSpaceMB.value > maxStorageMB && records.isNotEmpty()) {
            val oldest = records.first()
            deleteVideo(oldest.id)
            deletedCount++
            break
        }

        return Result.success(deletedCount)
    }

    override fun getVideoFilePath(): String {
        val dateFormatter = NSDateFormatter()
        dateFormatter.dateFormat = "yyyyMMdd_HHmmss"
        val timestamp = dateFormatter.stringFromDate(NSDate())
        return "$videoDir/VID_${timestamp}.mp4"
    }

    override fun getSessionFolderPath(sessionId: String): String {
        return videoDir
    }
}

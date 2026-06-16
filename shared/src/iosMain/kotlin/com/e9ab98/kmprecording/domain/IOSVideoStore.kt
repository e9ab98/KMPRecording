package com.e9ab98.kmprecording.domain

import com.e9ab98.kmprecording.model.VideoRecord
import com.e9ab98.kmprecording.service.IOSStorageService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class IOSVideoStore : VideoStore {
    private val storageService = IOSStorageService()
    private val _storageState = MutableStateFlow(StorageState())
    override val storageState: StateFlow<StorageState> = _storageState

    override suspend fun createOutput(mode: RecordingMode, segmentIndex: Int?): Result<SegmentOutput> {
        val basePath = storageService.getVideoFilePath()
        val path = if (segmentIndex == null) {
            basePath
        } else {
            basePath.removeSuffix(".mp4") + "_seg_$segmentIndex.mp4"
        }
        val sessionId = path.substringAfterLast("/").removeSuffix(".mp4").substringBefore("_seg_")
        return Result.success(SegmentOutput(sessionId, path, segmentIndex, mode))
    }

    override suspend fun saveRecordedSegment(segment: RecordedSegment): Result<VideoRecord> {
        val records = storageService.getVideoRecords()
        val record = records.firstOrNull { it.filePath == segment.filePath }
            ?: records.firstOrNull { it.filePath.substringAfterLast("/") == segment.filePath.substringAfterLast("/") }
        return if (record != null) {
            Result.success(record)
        } else {
            Result.failure(IllegalStateException("Recorded file not found: ${segment.filePath}"))
        }
    }

    override suspend fun listVideos(): Result<List<VideoRecord>> {
        return Result.success(storageService.getVideoRecords())
    }

    override suspend fun deleteVideo(videoId: String): Result<Boolean> = storageService.deleteVideo(videoId)

    override suspend fun refreshStorage(): Result<StorageState> {
        val state = StorageState(
            totalSpaceMB = storageService.totalSpaceMB.value,
            availableSpaceMB = storageService.availableSpaceMB.value,
            usedSpaceMB = storageService.usedSpaceMB.value,
            maxStorageMB = 0,
            isLowSpace = storageService.availableSpaceMB.value < 500,
            isCriticalSpace = storageService.availableSpaceMB.value < 100
        )
        _storageState.value = state
        return Result.success(state)
    }

    override suspend fun cleanupOldVideos(maxStorageMB: Long): Result<Int> {
        return storageService.cleanupOldVideos(maxStorageMB)
    }
}

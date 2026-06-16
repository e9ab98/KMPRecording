package com.e9ab98.kmprecording.domain

import com.e9ab98.kmprecording.model.CameraType
import com.e9ab98.kmprecording.model.Resolution
import com.e9ab98.kmprecording.model.VideoRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeVideoStore : VideoStore {
    private val _storageState = MutableStateFlow(
        StorageState(
            totalSpaceMB = 2048,
            availableSpaceMB = 1024,
            usedSpaceMB = 0,
            maxStorageMB = 1024
        )
    )
    override val storageState: StateFlow<StorageState> = _storageState

    val records = mutableListOf<VideoRecord>()
    var outputCount = 0
    var cleanupCalls = 0

    override suspend fun createOutput(mode: RecordingMode, segmentIndex: Int?): Result<SegmentOutput> {
        outputCount += 1
        val sessionId = "session-1"
        val suffix = segmentIndex?.let { "-seg-$it" } ?: "-normal"
        return Result.success(SegmentOutput(sessionId, "/tmp/$sessionId$suffix.mp4", segmentIndex, mode))
    }

    override suspend fun saveRecordedSegment(segment: RecordedSegment): Result<VideoRecord> {
        val record = VideoRecord(
            id = segment.filePath.substringAfterLast("/").removeSuffix(".mp4"),
            filePath = segment.filePath,
            fileName = segment.filePath.substringAfterLast("/"),
            durationSeconds = segment.durationSeconds,
            fileSizeBytes = segment.fileSizeBytes,
            createdAt = segment.startedAt,
            resolution = Resolution.FHD_1080P,
            cameraType = CameraType.BACK,
            isSegment = segment.segmentIndex != null,
            sessionId = segment.sessionId
        )
        records.add(record)
        return Result.success(record)
    }

    override suspend fun listVideos(): Result<List<VideoRecord>> = Result.success(records.toList())

    override suspend fun deleteVideo(videoId: String): Result<Boolean> {
        return Result.success(records.removeAll { it.id == videoId })
    }

    override suspend fun refreshStorage(): Result<StorageState> = Result.success(_storageState.value)

    override suspend fun cleanupOldVideos(maxStorageMB: Long): Result<Int> {
        cleanupCalls += 1
        return Result.success(0)
    }
}

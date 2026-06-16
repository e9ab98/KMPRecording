package com.e9ab98.kmprecording.domain

import com.e9ab98.kmprecording.model.VideoRecord
import kotlinx.coroutines.flow.StateFlow

interface VideoStore {
    val storageState: StateFlow<StorageState>

    suspend fun createOutput(mode: RecordingMode, segmentIndex: Int?): Result<SegmentOutput>
    suspend fun saveRecordedSegment(segment: RecordedSegment): Result<VideoRecord>
    suspend fun listVideos(): Result<List<VideoRecord>>
    suspend fun deleteVideo(videoId: String): Result<Boolean>
    suspend fun refreshStorage(): Result<StorageState>
    suspend fun cleanupOldVideos(maxStorageMB: Long): Result<Int>
}

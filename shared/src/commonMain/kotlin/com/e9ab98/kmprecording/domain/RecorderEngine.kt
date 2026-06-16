package com.e9ab98.kmprecording.domain

import com.e9ab98.kmprecording.model.CameraType
import com.e9ab98.kmprecording.model.RecordingConfig
import kotlinx.coroutines.flow.StateFlow

interface RecorderEngine {
    val previewState: StateFlow<PreviewState>
    val engineState: StateFlow<EngineRecordingState>

    suspend fun prepare(config: RecordingConfig): Result<Unit>
    suspend fun startSegment(output: SegmentOutput): Result<ActiveSegment>
    suspend fun stopSegment(): Result<RecordedSegment>
    suspend fun pause(): Result<Unit>
    suspend fun resume(): Result<Unit>
    suspend fun switchCamera(cameraType: CameraType): Result<Unit>
    fun previewHandle(): Any
    fun release()
}

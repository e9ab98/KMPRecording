package com.e9ab98.kmprecording.domain

import com.e9ab98.kmprecording.model.CameraType
import com.e9ab98.kmprecording.model.RecordingConfig
import com.e9ab98.kmprecording.model.Resolution
import com.e9ab98.kmprecording.model.VideoQuality
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val recordingConfig: StateFlow<RecordingConfig>
    val defaultMode: StateFlow<RecordingMode>
    val appLanguage: StateFlow<AppLanguage>

    suspend fun updateResolution(resolution: Resolution)
    suspend fun updateQuality(quality: VideoQuality)
    suspend fun updateAudioEnabled(enabled: Boolean)
    suspend fun updateDefaultMode(mode: RecordingMode)
    suspend fun updateLoopSegmentDuration(seconds: Int)
    suspend fun updateMaxStorageMB(maxStorageMB: Long)
    suspend fun updateAppLanguage(language: AppLanguage)
    suspend fun updateCameraType(cameraType: CameraType)
    suspend fun updateShowHud(enabled: Boolean)
    suspend fun updateGenerateSrt(enabled: Boolean)
}

package com.e9ab98.kmprecording.domain

import com.e9ab98.kmprecording.model.RecordingConfig
import com.e9ab98.kmprecording.model.Resolution
import com.e9ab98.kmprecording.model.VideoQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeSettingsRepository(
    initialConfig: RecordingConfig = RecordingConfig(),
    initialMode: RecordingMode = RecordingMode.NORMAL
) : SettingsRepository {
    private val _recordingConfig = MutableStateFlow(initialConfig)
    override val recordingConfig: StateFlow<RecordingConfig> = _recordingConfig

    private val _defaultMode = MutableStateFlow(initialMode)
    override val defaultMode: StateFlow<RecordingMode> = _defaultMode

    override suspend fun updateResolution(resolution: Resolution) {
        _recordingConfig.value = _recordingConfig.value.copy(resolution = resolution)
    }

    override suspend fun updateQuality(quality: VideoQuality) {
        _recordingConfig.value = _recordingConfig.value.copy(quality = quality)
    }

    override suspend fun updateAudioEnabled(enabled: Boolean) {
        _recordingConfig.value = _recordingConfig.value.copy(enableAudio = enabled)
    }

    override suspend fun updateDefaultMode(mode: RecordingMode) {
        _defaultMode.value = mode
    }

    override suspend fun updateLoopSegmentDuration(seconds: Int) {
        _recordingConfig.value = _recordingConfig.value.copy(segmentDurationSeconds = seconds)
    }

    override suspend fun updateMaxStorageMB(maxStorageMB: Long) {
        _recordingConfig.value = _recordingConfig.value.copy(maxStorageMB = maxStorageMB)
    }

    private val _appLanguage = MutableStateFlow(AppLanguage.ZH)
    override val appLanguage: StateFlow<AppLanguage> = _appLanguage

    override suspend fun updateAppLanguage(language: AppLanguage) {
        _appLanguage.value = language
    }

    override suspend fun updateCameraType(cameraType: com.e9ab98.kmprecording.model.CameraType) {
        _recordingConfig.value = _recordingConfig.value.copy(cameraType = cameraType)
    }

    override suspend fun updateShowHud(enabled: Boolean) {
        _recordingConfig.value = _recordingConfig.value.copy(showHud = enabled)
    }

    override suspend fun updateGenerateSrt(enabled: Boolean) {
        _recordingConfig.value = _recordingConfig.value.copy(generateSrt = enabled)
    }
}

package com.e9ab98.kmprecording.domain

import android.content.Context
import com.e9ab98.kmprecording.model.RecordingConfig
import com.e9ab98.kmprecording.model.Resolution
import com.e9ab98.kmprecording.model.VideoQuality
import com.e9ab98.kmprecording.service.SettingsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AndroidSettingsRepository(context: Context) : SettingsRepository {
    private val service = SettingsService(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _recordingConfig = MutableStateFlow(RecordingConfig())
    override val recordingConfig: StateFlow<RecordingConfig> = _recordingConfig

    private val _defaultMode = MutableStateFlow(RecordingMode.NORMAL)
    override val defaultMode: StateFlow<RecordingMode> = _defaultMode

    private val _appLanguage = MutableStateFlow(AppLanguage.ZH)
    override val appLanguage: StateFlow<AppLanguage> = _appLanguage

    init {
        scope.launch {
            service.recordingConfig.collect { config ->
                _recordingConfig.value = config
            }
        }
        scope.launch {
            service.loopModeDefault.collect { loopDefault ->
                _defaultMode.value = if (loopDefault) RecordingMode.LOOP else RecordingMode.NORMAL
            }
        }
        scope.launch {
            service.appLanguage.collect { langIndex ->
                _appLanguage.value = AppLanguage.entries.getOrElse(langIndex) { AppLanguage.ZH }
            }
        }
    }

    override suspend fun updateResolution(resolution: Resolution) = service.updateResolution(resolution)

    override suspend fun updateQuality(quality: VideoQuality) = service.updateVideoQuality(quality)

    override suspend fun updateAudioEnabled(enabled: Boolean) = service.updateEnableAudio(enabled)

    override suspend fun updateDefaultMode(mode: RecordingMode) {
        service.updateLoopModeDefault(mode == RecordingMode.LOOP)
        _defaultMode.value = mode
    }

    override suspend fun updateLoopSegmentDuration(seconds: Int) = service.updateSegmentDuration(seconds)

    override suspend fun updateMaxStorageMB(maxStorageMB: Long) = service.updateMaxStorageMB(maxStorageMB)

    override suspend fun updateAppLanguage(language: AppLanguage) {
        service.updateAppLanguage(AppLanguage.entries.indexOf(language))
        _appLanguage.value = language
    }

    override suspend fun updateCameraType(cameraType: com.e9ab98.kmprecording.model.CameraType) {
        _recordingConfig.value = _recordingConfig.value.copy(cameraType = cameraType)
    }

    override suspend fun updateShowHud(enabled: Boolean) {
        service.updateShowHud(enabled)
    }

    override suspend fun updateGenerateSrt(enabled: Boolean) {
        service.updateGenerateSrt(enabled)
    }
}

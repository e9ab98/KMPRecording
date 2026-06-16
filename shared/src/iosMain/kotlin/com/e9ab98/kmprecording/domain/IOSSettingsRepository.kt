package com.e9ab98.kmprecording.domain

import com.e9ab98.kmprecording.model.RecordingConfig
import com.e9ab98.kmprecording.model.Resolution
import com.e9ab98.kmprecording.model.VideoQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.Foundation.NSUserDefaults

class IOSSettingsRepository : SettingsRepository {
    private val defaults = NSUserDefaults.standardUserDefaults

    private val _recordingConfig = MutableStateFlow(loadConfig())
    override val recordingConfig: StateFlow<RecordingConfig> = _recordingConfig

    private val _defaultMode = MutableStateFlow(loadMode())
    override val defaultMode: StateFlow<RecordingMode> = _defaultMode

    private val _appLanguage = MutableStateFlow(loadLanguage())
    override val appLanguage: StateFlow<AppLanguage> = _appLanguage

    override suspend fun updateResolution(resolution: Resolution) {
        defaults.setInteger(Resolution.entries.indexOf(resolution).toLong(), "recording_resolution")
        _recordingConfig.value = _recordingConfig.value.copy(resolution = resolution)
    }

    override suspend fun updateQuality(quality: VideoQuality) {
        defaults.setInteger(VideoQuality.entries.indexOf(quality).toLong(), "recording_quality")
        _recordingConfig.value = _recordingConfig.value.copy(quality = quality)
    }

    override suspend fun updateAudioEnabled(enabled: Boolean) {
        defaults.setBool(enabled, "recording_audio_enabled")
        _recordingConfig.value = _recordingConfig.value.copy(enableAudio = enabled)
    }

    override suspend fun updateDefaultMode(mode: RecordingMode) {
        defaults.setInteger(RecordingMode.entries.indexOf(mode).toLong(), "recording_default_mode")
        _defaultMode.value = mode
    }

    override suspend fun updateLoopSegmentDuration(seconds: Int) {
        defaults.setInteger(seconds.toLong(), "recording_segment_duration")
        _recordingConfig.value = _recordingConfig.value.copy(segmentDurationSeconds = seconds)
    }

    override suspend fun updateMaxStorageMB(maxStorageMB: Long) {
        defaults.setInteger(maxStorageMB, "recording_max_storage_mb")
        _recordingConfig.value = _recordingConfig.value.copy(maxStorageMB = maxStorageMB)
    }

    private fun loadConfig(): RecordingConfig {
        val resolutionIndex = defaults.integerForKey("recording_resolution").toInt()
        val qualityIndex = defaults.integerForKey("recording_quality").toInt()
        val segmentDuration = defaults.integerForKey("recording_segment_duration").toInt()
        val maxStorage = defaults.integerForKey("recording_max_storage_mb")
        val hasAudioKey = defaults.objectForKey("recording_audio_enabled") != null
        val hasShowHudKey = defaults.objectForKey("recording_show_hud") != null
        val hasGenerateSrtKey = defaults.objectForKey("recording_generate_srt") != null

        return RecordingConfig(
            resolution = Resolution.entries.getOrElse(resolutionIndex) { Resolution.FHD_1080P },
            quality = VideoQuality.entries.getOrElse(qualityIndex) { VideoQuality.HIGH },
            enableAudio = if (hasAudioKey) defaults.boolForKey("recording_audio_enabled") else true,
            segmentDurationSeconds = if (segmentDuration > 0) segmentDuration else 300,
            maxStorageMB = if (maxStorage > 0) maxStorage else 1024 * 1024,
            showHud = if (hasShowHudKey) defaults.boolForKey("recording_show_hud") else true,
            generateSrt = if (hasGenerateSrtKey) defaults.boolForKey("recording_generate_srt") else true
        )
    }

    private fun loadMode(): RecordingMode {
        val modeIndex = defaults.integerForKey("recording_default_mode").toInt()
        return RecordingMode.entries.getOrElse(modeIndex) { RecordingMode.NORMAL }
    }

    override suspend fun updateAppLanguage(language: AppLanguage) {
        defaults.setInteger(AppLanguage.entries.indexOf(language).toLong(), "recording_app_language")
        _appLanguage.value = language
    }

    private fun loadLanguage(): AppLanguage {
        val langIndex = defaults.integerForKey("recording_app_language").toInt()
        return AppLanguage.entries.getOrElse(langIndex) { AppLanguage.ZH }
    }

    override suspend fun updateCameraType(cameraType: com.e9ab98.kmprecording.model.CameraType) {
        _recordingConfig.value = _recordingConfig.value.copy(cameraType = cameraType)
    }

    override suspend fun updateShowHud(enabled: Boolean) {
        defaults.setBool(enabled, "recording_show_hud")
        _recordingConfig.value = _recordingConfig.value.copy(showHud = enabled)
    }

    override suspend fun updateGenerateSrt(enabled: Boolean) {
        defaults.setBool(enabled, "recording_generate_srt")
        _recordingConfig.value = _recordingConfig.value.copy(generateSrt = enabled)
    }
}

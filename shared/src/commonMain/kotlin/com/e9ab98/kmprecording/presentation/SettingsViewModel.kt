package com.e9ab98.kmprecording.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.e9ab98.kmprecording.domain.AppLanguage
import com.e9ab98.kmprecording.domain.RecordingMode
import com.e9ab98.kmprecording.domain.SettingsRepository
import com.e9ab98.kmprecording.model.Resolution
import com.e9ab98.kmprecording.model.VideoQuality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    coroutineScope: CoroutineScope? = null,
    observeExternalState: Boolean = true
) : ViewModel() {
    private val scope = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            resolution = settingsRepository.recordingConfig.value.resolution,
            quality = settingsRepository.recordingConfig.value.quality,
            audioEnabled = settingsRepository.recordingConfig.value.enableAudio,
            segmentDurationSeconds = settingsRepository.recordingConfig.value.segmentDurationSeconds,
            maxStorageMB = settingsRepository.recordingConfig.value.maxStorageMB,
            defaultMode = settingsRepository.defaultMode.value,
            appLanguage = settingsRepository.appLanguage.value,
            showHud = settingsRepository.recordingConfig.value.showHud,
            generateSrt = settingsRepository.recordingConfig.value.generateSrt
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        if (observeExternalState) {
            scope.launch {
                settingsRepository.recordingConfig.collect { config ->
                    _uiState.value = _uiState.value.copy(
                        resolution = config.resolution,
                        quality = config.quality,
                        audioEnabled = config.enableAudio,
                        segmentDurationSeconds = config.segmentDurationSeconds,
                        maxStorageMB = config.maxStorageMB,
                        showHud = config.showHud,
                        generateSrt = config.generateSrt
                    )
                }
            }
            scope.launch {
                settingsRepository.defaultMode.collect { mode ->
                    _uiState.value = _uiState.value.copy(defaultMode = mode)
                }
            }
            scope.launch {
                settingsRepository.appLanguage.collect { language ->
                    _uiState.value = _uiState.value.copy(appLanguage = language)
                }
            }
        }
    }

    fun resolutionChanged(resolution: Resolution) {
        scope.launch {
            settingsRepository.updateResolution(resolution)
            _uiState.value = _uiState.value.copy(resolution = settingsRepository.recordingConfig.value.resolution)
        }
    }

    fun qualityChanged(quality: VideoQuality) {
        scope.launch {
            settingsRepository.updateQuality(quality)
            _uiState.value = _uiState.value.copy(quality = settingsRepository.recordingConfig.value.quality)
        }
    }

    fun audioEnabledChanged(enabled: Boolean) {
        scope.launch {
            settingsRepository.updateAudioEnabled(enabled)
            _uiState.value = _uiState.value.copy(audioEnabled = settingsRepository.recordingConfig.value.enableAudio)
        }
    }

    fun defaultModeChanged(mode: RecordingMode) {
        scope.launch {
            settingsRepository.updateDefaultMode(mode)
            _uiState.value = _uiState.value.copy(defaultMode = settingsRepository.defaultMode.value)
        }
    }

    fun segmentDurationChanged(seconds: Int) {
        scope.launch {
            settingsRepository.updateLoopSegmentDuration(seconds.coerceIn(60, 3600))
            _uiState.value = _uiState.value.copy(
                segmentDurationSeconds = settingsRepository.recordingConfig.value.segmentDurationSeconds
            )
        }
    }

    fun maxStorageGBChanged(gb: Int) {
        scope.launch {
            settingsRepository.updateMaxStorageMB(gb.coerceIn(1, 100) * 1024L)
            _uiState.value = _uiState.value.copy(maxStorageMB = settingsRepository.recordingConfig.value.maxStorageMB)
        }
    }

    fun appLanguageChanged(language: AppLanguage) {
        scope.launch {
            settingsRepository.updateAppLanguage(language)
            _uiState.value = _uiState.value.copy(appLanguage = settingsRepository.appLanguage.value)
        }
    }

    fun showHudChanged(enabled: Boolean) {
        scope.launch {
            settingsRepository.updateShowHud(enabled)
            _uiState.value = _uiState.value.copy(showHud = settingsRepository.recordingConfig.value.showHud)
        }
    }

    fun generateSrtChanged(enabled: Boolean) {
        scope.launch {
            settingsRepository.updateGenerateSrt(enabled)
            _uiState.value = _uiState.value.copy(generateSrt = settingsRepository.recordingConfig.value.generateSrt)
        }
    }
}

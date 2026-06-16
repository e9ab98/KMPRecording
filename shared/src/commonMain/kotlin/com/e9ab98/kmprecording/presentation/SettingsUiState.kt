package com.e9ab98.kmprecording.presentation

import com.e9ab98.kmprecording.domain.AppLanguage
import com.e9ab98.kmprecording.domain.RecordingMode
import com.e9ab98.kmprecording.model.Resolution
import com.e9ab98.kmprecording.model.VideoQuality

data class SettingsUiState(
    val resolution: Resolution = Resolution.FHD_1080P,
    val quality: VideoQuality = VideoQuality.HIGH,
    val audioEnabled: Boolean = true,
    val segmentDurationSeconds: Int = 300,
    val maxStorageMB: Long = 1024 * 1024,
    val defaultMode: RecordingMode = RecordingMode.NORMAL,
    val appLanguage: AppLanguage = AppLanguage.ZH,
    val showHud: Boolean = true,
    val generateSrt: Boolean = true
) {
    val maxStorageGB: Int
        get() = (maxStorageMB / 1024).toInt()
}

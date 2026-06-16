package com.e9ab98.kmprecording.presentation

import com.e9ab98.kmprecording.domain.AppLanguage
import com.e9ab98.kmprecording.domain.RecordingLifecycle
import com.e9ab98.kmprecording.domain.RecordingMode
import com.e9ab98.kmprecording.domain.StorageState
import com.e9ab98.kmprecording.model.CameraType
import com.e9ab98.kmprecording.model.Resolution
import com.e9ab98.kmprecording.model.VideoQuality

data class RecordingUiState(
    val lifecycle: RecordingLifecycle = RecordingLifecycle.Idle,
    val mode: RecordingMode = RecordingMode.NORMAL,
    val elapsedSeconds: Int = 0,
    val activeSegmentIndex: Int? = null,
    val cameraType: CameraType = CameraType.BACK,
    val resolution: Resolution = Resolution.FHD_1080P,
    val quality: VideoQuality = VideoQuality.HIGH,
    val audioEnabled: Boolean = true,
    val storage: StorageState = StorageState(),
    val previewReady: Boolean = false,
    val timestampText: String = "",
    val errorMessage: String? = null,
    val appLanguage: AppLanguage = AppLanguage.ZH,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val speedKmh: Double = 0.0,
    val showHud: Boolean = true,
    val generateSrt: Boolean = true
)

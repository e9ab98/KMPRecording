package com.e9ab98.kmprecording.presentation

import com.e9ab98.kmprecording.domain.AppLanguage
import com.e9ab98.kmprecording.model.VideoRecord

data class VideoLibraryUiState(
    val videos: List<VideoRecord> = emptyList(),
    val selectedVideo: VideoRecord? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val totalSpaceMB: Long = 0,
    val availableSpaceMB: Long = 0,
    val usedSpaceMB: Long = 0,
    val appLanguage: AppLanguage = AppLanguage.ZH
) {
    val totalDurationSeconds: Int
        get() = videos.sumOf { it.durationSeconds }
}

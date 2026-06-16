package com.e9ab98.kmprecording.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.e9ab98.kmprecording.domain.VideoStore
import com.e9ab98.kmprecording.domain.SettingsRepository
import com.e9ab98.kmprecording.domain.AppLanguage
import com.e9ab98.kmprecording.model.VideoRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoLibraryViewModel(
    private val videoStore: VideoStore,
    private val settingsRepository: SettingsRepository,
    coroutineScope: CoroutineScope? = null,
    autoLoad: Boolean = true
) : ViewModel() {
    private val scope = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(
        VideoLibraryUiState(
            appLanguage = settingsRepository.appLanguage.value
        )
    )
    val uiState: StateFlow<VideoLibraryUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            settingsRepository.appLanguage.collect { language ->
                _uiState.value = _uiState.value.copy(appLanguage = language)
            }
        }
        if (autoLoad) {
            loadVideos()
        }
    }

    fun loadVideos() {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            videoStore.refreshStorage().onSuccess { storage ->
                _uiState.value = _uiState.value.copy(
                    totalSpaceMB = storage.totalSpaceMB,
                    availableSpaceMB = storage.availableSpaceMB,
                    usedSpaceMB = storage.usedSpaceMB
                )
            }

            videoStore.listVideos()
                .onSuccess { videos ->
                    _uiState.value = _uiState.value.copy(
                        videos = videos.sortedByDescending { it.createdAt },
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "加载视频失败"
                    )
                }
        }
    }

    fun selectVideo(video: VideoRecord) {
        _uiState.value = _uiState.value.copy(selectedVideo = video)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedVideo = null)
    }

    fun deleteVideo(videoId: String) {
        scope.launch {
            videoStore.deleteVideo(videoId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        selectedVideo = _uiState.value.selectedVideo?.takeUnless { it.id == videoId }
                    )
                    loadVideos()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "删除视频失败")
                }
        }
    }

    fun dismissErrorClicked() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

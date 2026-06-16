package com.e9ab98.kmprecording.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.e9ab98.kmprecording.model.VideoRecord
import com.e9ab98.kmprecording.service.StorageService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoListViewModel(
    private val storageService: StorageService
) : ViewModel() {
    
    private val _videos = MutableStateFlow<List<VideoRecord>>(emptyList())
    val videos: StateFlow<List<VideoRecord>> = _videos.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _selectedVideo = MutableStateFlow<VideoRecord?>(null)
    val selectedVideo: StateFlow<VideoRecord?> = _selectedVideo.asStateFlow()
    
    init {
        loadVideos()
    }
    
    fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            val videos = storageService.getVideoRecords()
            _videos.value = videos.sortedByDescending { it.createdAt }
            _isLoading.value = false
        }
    }
    
    fun selectVideo(video: VideoRecord) {
        _selectedVideo.value = video
    }
    
    fun deleteVideo(videoId: String) {
        viewModelScope.launch {
            val result = storageService.deleteVideo(videoId)
            if (result.isSuccess) {
                loadVideos()
            }
        }
    }
    
    fun clearSelection() {
        _selectedVideo.value = null
    }
    
    fun getStorageInfo(): String {
        val used = storageService.usedSpaceMB.value
        val available = storageService.availableSpaceMB.value
        return "已用: ${used}MB | 可用: ${available}MB"
    }
}
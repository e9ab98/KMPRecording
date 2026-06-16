package com.e9ab98.kmprecording.presentation

import com.e9ab98.kmprecording.domain.FakeVideoStore
import com.e9ab98.kmprecording.domain.FakeSettingsRepository
import com.e9ab98.kmprecording.model.CameraType
import com.e9ab98.kmprecording.model.Resolution
import com.e9ab98.kmprecording.model.VideoRecord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class VideoLibraryViewModelTest {
    @Test
    fun loadVideosSortsNewestFirstAndRefreshesStorage() = runTest {
        val store = FakeVideoStore()
        store.records.add(video("old", createdAt = LocalDateTime(2026, 1, 1, 8, 0)))
        store.records.add(video("new", createdAt = LocalDateTime(2026, 1, 2, 8, 0)))
        val viewModel = VideoLibraryViewModel(store, FakeSettingsRepository(), coroutineScope = this, autoLoad = false)

        viewModel.loadVideos()
        advanceUntilIdle()

        assertEquals(listOf("new", "old"), viewModel.uiState.value.videos.map { it.id })
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1024, viewModel.uiState.value.availableSpaceMB)
        coroutineContext.cancelChildren()
    }

    @Test
    fun deleteSelectedVideoReloadsAndClearsSelection() = runTest {
        val store = FakeVideoStore()
        val record = video("target")
        store.records.add(record)
        val viewModel = VideoLibraryViewModel(store, FakeSettingsRepository(), coroutineScope = this, autoLoad = false)

        viewModel.selectVideo(record)
        viewModel.deleteVideo(record.id)
        advanceUntilIdle()

        assertEquals(emptyList(), viewModel.uiState.value.videos)
        assertNull(viewModel.uiState.value.selectedVideo)
        coroutineContext.cancelChildren()
    }

    private fun video(
        id: String,
        createdAt: LocalDateTime = LocalDateTime(2026, 1, 1, 8, 0)
    ): VideoRecord {
        return VideoRecord(
            id = id,
            filePath = "/tmp/$id.mp4",
            fileName = "$id.mp4",
            durationSeconds = 65,
            fileSizeBytes = 2_097_152,
            createdAt = createdAt,
            resolution = Resolution.FHD_1080P,
            cameraType = CameraType.BACK
        )
    }
}

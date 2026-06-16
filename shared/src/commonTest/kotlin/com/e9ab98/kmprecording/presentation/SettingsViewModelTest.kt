package com.e9ab98.kmprecording.presentation

import com.e9ab98.kmprecording.domain.FakeSettingsRepository
import com.e9ab98.kmprecording.domain.RecordingMode
import com.e9ab98.kmprecording.model.RecordingConfig
import com.e9ab98.kmprecording.model.Resolution
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @Test
    fun resolutionChangePersistsAndUpdatesUiState() = runTest {
        val repository = FakeSettingsRepository(
            initialConfig = RecordingConfig(resolution = Resolution.HD_720P)
        )
        val viewModel = SettingsViewModel(
            settingsRepository = repository,
            coroutineScope = this,
            observeExternalState = false
        )

        viewModel.resolutionChanged(Resolution.UHD_4K)
        advanceUntilIdle()

        assertEquals(Resolution.UHD_4K, repository.recordingConfig.value.resolution)
        assertEquals(Resolution.UHD_4K, viewModel.uiState.value.resolution)
    }

    @Test
    fun defaultModeChangePersistsAndUpdatesUiState() = runTest {
        val repository = FakeSettingsRepository(initialMode = RecordingMode.NORMAL)
        val viewModel = SettingsViewModel(
            settingsRepository = repository,
            coroutineScope = this,
            observeExternalState = false
        )

        viewModel.defaultModeChanged(RecordingMode.LOOP)
        advanceUntilIdle()

        assertEquals(RecordingMode.LOOP, repository.defaultMode.value)
        assertEquals(RecordingMode.LOOP, viewModel.uiState.value.defaultMode)
    }
}

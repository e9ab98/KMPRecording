package com.e9ab98.kmprecording.presentation

import com.e9ab98.kmprecording.domain.FakeRecorderEngine
import com.e9ab98.kmprecording.domain.FakeSettingsRepository
import com.e9ab98.kmprecording.domain.FakeVideoStore
import com.e9ab98.kmprecording.domain.RecorderController
import com.e9ab98.kmprecording.domain.FakeLocationTracker
import com.e9ab98.kmprecording.domain.RecordingLifecycle
import com.e9ab98.kmprecording.domain.RecordingMode
import com.e9ab98.kmprecording.model.RecordingConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingViewModelTest {
    @Test
    fun startClickedStartsRecordingUsingDefaultMode() = runTest {
        val engine = FakeRecorderEngine()
        val settings = FakeSettingsRepository(initialMode = RecordingMode.LOOP)
        val locationTracker = FakeLocationTracker()
        val controller = RecorderController(engine, FakeVideoStore(), locationTracker)
        val viewModel = RecordingViewModel(
            recorderController = controller,
            settingsRepository = settings,
            locationTracker = locationTracker,
            coroutineScope = this,
            observeExternalState = false,
            timeProvider = { testScheduler.currentTime }
        )

        viewModel.startClicked()
        runCurrent()

        assertEquals(RecordingMode.LOOP, viewModel.uiState.value.mode)
        assertIs<RecordingLifecycle.Recording>(viewModel.uiState.value.lifecycle)
        assertEquals(0, viewModel.uiState.value.activeSegmentIndex)

        viewModel.stopClicked()
        advanceUntilIdle()
    }

    @Test
    fun elapsedTimeAdvancesWhileRecording() = runTest {
        val engine = FakeRecorderEngine()
        val settings = FakeSettingsRepository(initialMode = RecordingMode.NORMAL)
        val locationTracker = FakeLocationTracker()
        val controller = RecorderController(engine, FakeVideoStore(), locationTracker)
        val viewModel = RecordingViewModel(
            recorderController = controller,
            settingsRepository = settings,
            locationTracker = locationTracker,
            coroutineScope = this,
            observeExternalState = false,
            timeProvider = { testScheduler.currentTime }
        )

        viewModel.startClicked()
        runCurrent()
        advanceTimeBy(2_100)
        runCurrent()

        assertEquals(2, viewModel.uiState.value.elapsedSeconds)

        viewModel.stopClicked()
        advanceUntilIdle()
    }

    @Test
    fun timestampTextRefreshesWhileRecording() = runTest {
        val engine = FakeRecorderEngine()
        val settings = FakeSettingsRepository(initialMode = RecordingMode.NORMAL)
        val locationTracker = FakeLocationTracker()
        val controller = RecorderController(engine, FakeVideoStore(), locationTracker)
        val timestamps = mutableListOf("2026-06-05 10:00:00", "2026-06-05 10:00:01")
        val viewModel = RecordingViewModel(
            recorderController = controller,
            settingsRepository = settings,
            locationTracker = locationTracker,
            coroutineScope = this,
            observeExternalState = false,
            timestampProvider = { timestamps.removeFirst() },
            timeProvider = { testScheduler.currentTime }
        )

        assertEquals("2026-06-05 10:00:00", viewModel.uiState.value.timestampText)

        viewModel.startClicked()
        runCurrent()
        advanceTimeBy(1_100)
        runCurrent()

        assertEquals("2026-06-05 10:00:01", viewModel.uiState.value.timestampText)

        viewModel.stopClicked()
        advanceUntilIdle()
    }

    @Test
    fun loopRecordingRollsOverWhenSegmentDurationElapses() = runTest {
        val engine = FakeRecorderEngine()
        val store = FakeVideoStore()
        val settings = FakeSettingsRepository(
            initialConfig = RecordingConfig(segmentDurationSeconds = 2),
            initialMode = RecordingMode.LOOP
        )
        val locationTracker = FakeLocationTracker()
        val controller = RecorderController(engine, store, locationTracker)
        val viewModel = RecordingViewModel(
            recorderController = controller,
            settingsRepository = settings,
            locationTracker = locationTracker,
            coroutineScope = this,
            observeExternalState = false,
            timestampProvider = { "2026-06-05 10:00:00" },
            timeProvider = { testScheduler.currentTime }
        )

        viewModel.startClicked()
        runCurrent()
        advanceTimeBy(2_100)
        runCurrent()

        assertEquals(1, viewModel.uiState.value.activeSegmentIndex)
        assertEquals(1, store.records.size)
        assertEquals(2, engine.startCalls)

        viewModel.stopClicked()
        advanceUntilIdle()
    }
}

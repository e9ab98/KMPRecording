package com.e9ab98.kmprecording.domain

import com.e9ab98.kmprecording.model.RecordingConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RecorderControllerTest {
    @Test
    fun normalRecordingStartsOneSegmentAndStopsIt() = runTest {
        val engine = FakeRecorderEngine()
        val store = FakeVideoStore()
        val controller = RecorderController(engine, store, FakeLocationTracker())

        controller.start(RecordingMode.NORMAL, RecordingConfig(segmentDurationSeconds = 60))
        assertEquals(1, engine.startCalls)
        assertIs<RecordingLifecycle.Recording>(controller.state.value.lifecycle)
        assertEquals(null, controller.state.value.activeSegmentIndex)

        controller.stop()
        assertEquals(1, engine.stopCalls)
        assertEquals(1, store.records.size)
        assertIs<RecordingLifecycle.Idle>(controller.state.value.lifecycle)
    }

    @Test
    fun loopRecordingStartsAtSegmentZeroAndRolloverStartsNextSegment() = runTest {
        val engine = FakeRecorderEngine()
        val store = FakeVideoStore()
        val controller = RecorderController(engine, store, FakeLocationTracker())

        controller.start(RecordingMode.LOOP, RecordingConfig(segmentDurationSeconds = 60))
        assertEquals(0, controller.state.value.activeSegmentIndex)

        controller.rolloverLoopSegment()
        assertEquals(1, engine.stopCalls)
        assertEquals(2, engine.startCalls)
        assertEquals(1, controller.state.value.activeSegmentIndex)
        assertEquals(1, store.records.size)
    }
}

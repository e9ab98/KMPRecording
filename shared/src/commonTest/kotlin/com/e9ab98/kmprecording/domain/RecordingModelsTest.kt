package com.e9ab98.kmprecording.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecordingModelsTest {
    @Test
    fun normalModeHasNoLoopSegmentIndex() {
        val state = RecordingSessionState(
            lifecycle = RecordingLifecycle.Idle,
            mode = RecordingMode.NORMAL,
            elapsedSeconds = 0,
            activeSegmentIndex = null
        )

        assertEquals(RecordingMode.NORMAL, state.mode)
        assertNull(state.activeSegmentIndex)
    }
}

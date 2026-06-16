package com.e9ab98.kmprecording.service

import com.e9ab98.kmprecording.model.SegmentRecordingConfig

expect fun createSegmentRecorder(
    config: SegmentRecordingConfig = SegmentRecordingConfig()
): SegmentRecorder
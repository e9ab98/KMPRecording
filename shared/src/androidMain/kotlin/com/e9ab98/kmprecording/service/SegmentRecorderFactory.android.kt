package com.e9ab98.kmprecording.service

import android.content.Context
import com.e9ab98.kmprecording.model.SegmentRecordingConfig

actual fun createSegmentRecorder(
    config: SegmentRecordingConfig
): SegmentRecorder {
    val context = ContextHolder.context
    return AndroidSegmentRecorder(context, config)
}

object ContextHolder {
    lateinit var context: Context
}
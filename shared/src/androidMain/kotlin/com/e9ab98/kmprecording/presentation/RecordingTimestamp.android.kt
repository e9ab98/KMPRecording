package com.e9ab98.kmprecording.presentation

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun currentRecordingTimestampText(): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
}

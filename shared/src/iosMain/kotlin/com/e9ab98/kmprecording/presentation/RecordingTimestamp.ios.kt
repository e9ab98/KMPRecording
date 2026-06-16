package com.e9ab98.kmprecording.presentation

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

actual fun currentRecordingTimestampText(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
    return formatter.stringFromDate(NSDate())
}

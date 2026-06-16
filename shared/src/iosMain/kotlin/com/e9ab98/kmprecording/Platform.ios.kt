package com.e9ab98.kmprecording

import platform.UIKit.UIDevice
import platform.Foundation.NSDate
import platform.CoreFoundation.kCFAbsoluteTimeIntervalSince1970

class IOSPlatform : Platform {
    override val name: String = "${UIDevice.currentDevice.systemName()} ${UIDevice.currentDevice.systemVersion}"
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSinceReferenceDate + kCFAbsoluteTimeIntervalSince1970).toLong() * 1000
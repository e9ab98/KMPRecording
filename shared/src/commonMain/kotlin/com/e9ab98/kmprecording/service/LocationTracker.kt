package com.e9ab98.kmprecording.service

import kotlinx.coroutines.flow.StateFlow

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val speedMps: Double,
    val timestamp: Long
) {
    val speedKmh: Double
        get() = speedMps * 3.6
}

interface LocationTracker {
    val locationState: StateFlow<LocationData?>
    fun startTracking()
    fun stopTracking()
}

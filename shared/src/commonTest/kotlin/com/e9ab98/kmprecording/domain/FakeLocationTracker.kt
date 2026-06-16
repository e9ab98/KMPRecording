package com.e9ab98.kmprecording.domain

import com.e9ab98.kmprecording.service.LocationData
import com.e9ab98.kmprecording.service.LocationTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeLocationTracker : LocationTracker {
    private val _locationState = MutableStateFlow<LocationData?>(null)
    override val locationState: StateFlow<LocationData?> = _locationState.asStateFlow()
    var startCalls = 0
    var stopCalls = 0

    override fun startTracking() {
        startCalls++
    }

    override fun stopTracking() {
        stopCalls++
    }

    fun setLocation(data: LocationData?) {
        _locationState.value = data
    }
}

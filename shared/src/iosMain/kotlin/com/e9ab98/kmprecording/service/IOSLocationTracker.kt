package com.e9ab98.kmprecording.service

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.CoreLocation.*
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class IOSLocationTracker : LocationTracker {
    private val locationManager = CLLocationManager()
    private val _locationState = MutableStateFlow<LocationData?>(null)
    override val locationState: StateFlow<LocationData?> = _locationState.asStateFlow()
    private var isTracking = false

    private val delegate = CLLocationManagerDelegateImpl(
        onLocationUpdated = { location ->
            updateWithLocation(location)
        },
        onError = { error ->
            println("IOSLocationTracker: Location updates failed with error: ${error.localizedDescription}")
        }
    )

    override fun startTracking() {
        if (isTracking) return
        locationManager.delegate = delegate
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        
        val authStatus = CLLocationManager.authorizationStatus()
        if (authStatus == kCLAuthorizationStatusNotDetermined) {
            locationManager.requestWhenInUseAuthorization()
        }
        
        locationManager.startUpdatingLocation()
        isTracking = true
        
        locationManager.location?.let {
            updateWithLocation(it)
        }
    }

    override fun stopTracking() {
        if (!isTracking) return
        locationManager.stopUpdatingLocation()
        locationManager.delegate = null
        isTracking = false
    }

    private fun updateWithLocation(location: CLLocation) {
        val lat = location.coordinate.useContents { latitude }
        val lon = location.coordinate.useContents { longitude }
        val rawSpeed = location.speed
        val speed = if (rawSpeed >= 0.0) rawSpeed else 0.0
        val timestamp = (location.timestamp.timeIntervalSince1970 * 1000).toLong()

        _locationState.value = LocationData(
            latitude = lat,
            longitude = lon,
            speedMps = speed,
            timestamp = timestamp
        )
    }
}

private class CLLocationManagerDelegateImpl(
    private val onLocationUpdated: (CLLocation) -> Unit,
    private val onError: (NSError) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {
    
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
        onLocationUpdated(location)
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        onError(didFailWithError)
    }
}

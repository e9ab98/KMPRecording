package com.e9ab98.kmprecording.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidLocationTracker(private val context: Context) : LocationTracker {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val _locationState = MutableStateFlow<LocationData?>(null)
    override val locationState: StateFlow<LocationData?> = _locationState.asStateFlow()
    private var isTracking = false
    private var lastLocation: Location? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            println("AndroidLocationTracker: onLocationChanged: lat=${location.latitude}, lon=${location.longitude}, hasSpeed=${location.hasSpeed()}, rawSpeed=${location.speed}")
            var speed = if (location.hasSpeed()) location.speed.toDouble() else 0.0
            
            val last = lastLocation
            if (speed == 0.0 && last != null) {
                val timeDiffSec = (location.time - last.time) / 1000.0
                if (timeDiffSec > 0.0) {
                    val distance = last.distanceTo(location)
                    println("AndroidLocationTracker: Calculating speed manually: distance=${distance}m, timeDiff=${timeDiffSec}s")
                    // Only compute speed if the movement is significant to filter out GPS drift noise
                    if (distance > 0.5) {
                        speed = distance / timeDiffSec
                        println("AndroidLocationTracker: Manual speed calculated: ${speed} m/s (${speed * 3.6} km/h)")
                    } else {
                        println("AndroidLocationTracker: Distance too small ($distance <= 0.5m), ignoring to prevent drift jitter")
                    }
                }
            }
            
            lastLocation = location

            _locationState.value = LocationData(
                latitude = location.latitude,
                longitude = location.longitude,
                speedMps = speed,
                timestamp = location.time
            )
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {
            println("AndroidLocationTracker: Provider enabled: $provider")
        }
        override fun onProviderDisabled(provider: String) {
            println("AndroidLocationTracker: Provider disabled: $provider")
        }
    }

    override fun startTracking() {
        println("AndroidLocationTracker: startTracking requested (isTracking=$isTracking)")
        if (isTracking) return
        
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        println("AndroidLocationTracker: Permissions check - ACCESS_FINE_LOCATION: $hasFine, ACCESS_COARSE_LOCATION: $hasCoarse")
        
        if (!hasFine && !hasCoarse) {
            println("AndroidLocationTracker: No location permission granted, aborting tracking start")
            return
        }

        var registeredAny = false

        // Request updates from GPS provider (highly accurate, speed available)
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                println("AndroidLocationTracker: GPS_PROVIDER is enabled. Registering listener...")
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    0f,
                    locationListener,
                    Looper.getMainLooper()
                )
                registeredAny = true
                println("AndroidLocationTracker: GPS_PROVIDER listener registered successfully")
            } else {
                println("AndroidLocationTracker: GPS_PROVIDER is disabled on this device")
            }
        } catch (e: Exception) {
            println("AndroidLocationTracker: Failed to register GPS provider: ${e.message}")
            e.printStackTrace()
        }

        // Also register Network provider for backup / indoor locations
        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                println("AndroidLocationTracker: NETWORK_PROVIDER is enabled. Registering listener...")
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    0f,
                    locationListener,
                    Looper.getMainLooper()
                )
                registeredAny = true
                println("AndroidLocationTracker: NETWORK_PROVIDER listener registered successfully")
            } else {
                println("AndroidLocationTracker: NETWORK_PROVIDER is disabled on this device")
            }
        } catch (e: Exception) {
            println("AndroidLocationTracker: Failed to register Network provider: ${e.message}")
            e.printStackTrace()
        }

        isTracking = registeredAny
        println("AndroidLocationTracker: startTracking finished (isTracking=$isTracking)")

        if (registeredAny) {
            // Get last known location as fallback/initial value
            var bestLast: Location? = null
            try {
                val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val lastNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                bestLast = if (lastGps != null && lastNetwork != null) {
                    if (lastGps.time > lastNetwork.time) lastGps else lastNetwork
                } else {
                    lastGps ?: lastNetwork
                }
                println("AndroidLocationTracker: lastKnownLocation - GPS=$lastGps, Network=$lastNetwork, best=$bestLast")
            } catch (e: Exception) {
                println("AndroidLocationTracker: Failed to get last known location: ${e.message}")
            }
            
            bestLast?.let {
                val speed = if (it.hasSpeed()) it.speed.toDouble() else 0.0
                _locationState.value = LocationData(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    speedMps = speed,
                    timestamp = it.time
                )
                lastLocation = it
            }
        }
    }

    override fun stopTracking() {
        println("AndroidLocationTracker: stopTracking requested (isTracking=$isTracking)")
        if (!isTracking) return
        try {
            locationManager.removeUpdates(locationListener)
            println("AndroidLocationTracker: Location listener updates removed successfully")
        } catch (e: Exception) {
            println("AndroidLocationTracker: Failed to stop tracking: ${e.message}")
            e.printStackTrace()
        } finally {
            isTracking = false
            lastLocation = null
        }
    }
}



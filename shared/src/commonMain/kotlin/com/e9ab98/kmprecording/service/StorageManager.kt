package com.e9ab98.kmprecording.service

import com.e9ab98.kmprecording.model.StorageLocation
import com.e9ab98.kmprecording.model.StorageInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface StorageManager {
    val availableLocations: StateFlow<List<StorageLocation>>
    val currentLocation: StateFlow<StorageLocation>
    val storageInfo: StateFlow<StorageInfo>
    
    fun detectAvailableStorage()
    fun selectStorageLocation(location: StorageLocation): Result<Boolean>
    fun getStoragePath(): String
    fun getAvailableSpaceMB(): Long
    fun getTotalSpaceMB(): Long
    fun isStorageAvailable(): Boolean
}

expect fun createStorageManager(): StorageManager
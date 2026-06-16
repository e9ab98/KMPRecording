package com.e9ab98.kmprecording.service

import com.e9ab98.kmprecording.model.StorageInfo
import com.e9ab98.kmprecording.model.StorageLocation
import com.e9ab98.kmprecording.model.StorageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.Foundation.*
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
class IOSStorageManager : StorageManager {
    
    private val _availableLocations = MutableStateFlow<List<StorageLocation>>(emptyList())
    private val _currentLocation = MutableStateFlow<StorageLocation>(
        StorageLocation(
            id = "documents",
            name = "App文档目录",
            path = getDocumentsPath(),
            type = StorageType.INTERNAL,
            totalSpaceMB = 0,
            availableSpaceMB = 0
        )
    )
    private val _storageInfo = MutableStateFlow<StorageInfo>(
        StorageInfo(
            totalSpaceMB = 0,
            availableSpaceMB = 0,
            usedSpaceMB = 0,
            usedPercent = 0,
            locationType = StorageType.INTERNAL,
            locationName = "App文档目录"
        )
    )
    
    override val availableLocations: StateFlow<List<StorageLocation>> = _availableLocations
    override val currentLocation: StateFlow<StorageLocation> = _currentLocation
    override val storageInfo: StateFlow<StorageInfo> = _storageInfo
    
    init {
        detectAvailableStorage()
    }
    
    @OptIn(ExperimentalForeignApi::class)
    override fun detectAvailableStorage() {
        val locations = mutableListOf<StorageLocation>()
        
        val documentsPath = getDocumentsPath()
        
        val fileManager = NSFileManager.defaultManager
        val attributes = fileManager.attributesOfFileSystemForPath(documentsPath, null)
        
        val totalSize = (attributes?.get("NSFileSystemSize") as? NSNumber)?.longLongValue ?: 0L
        val freeSize = (attributes?.get("NSFileSystemFreeSize") as? NSNumber)?.longLongValue ?: 0L
        
        val totalMB = totalSize / (1024 * 1024)
        val availableMB = freeSize / (1024 * 1024)
        
        locations.add(
            StorageLocation(
                id = "documents",
                name = "App文档目录",
                path = documentsPath,
                type = StorageType.INTERNAL,
                totalSpaceMB = totalMB,
                availableSpaceMB = availableMB,
                isRemovable = false,
                isSelected = true
            )
        )
        
        _availableLocations.value = locations
        _currentLocation.value = locations.first()
        
        updateStorageInfo(locations.first())
    }
    
    override fun selectStorageLocation(location: StorageLocation): Result<Boolean> {
        return try {
            if (!isStorageAvailableAtPath(location.path)) {
                return Result.failure(Exception("存储位置不可用"))
            }
            
            val updatedLocations = _availableLocations.value.map { loc ->
                loc.copy(isSelected = loc.id == location.id)
            }
            _availableLocations.value = updatedLocations
            
            _currentLocation.value = location
            updateStorageInfo(location)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getStoragePath(): String {
        return _currentLocation.value.path
    }
    
    override fun getAvailableSpaceMB(): Long {
        return _currentLocation.value.availableSpaceMB
    }
    
    override fun getTotalSpaceMB(): Long {
        return _currentLocation.value.totalSpaceMB
    }
    
    override fun isStorageAvailable(): Boolean {
        return isStorageAvailableAtPath(_currentLocation.value.path)
    }
    
    @OptIn(ExperimentalForeignApi::class)
    private fun updateStorageInfo(location: StorageLocation) {
        val fileManager = NSFileManager.defaultManager
        val attributes = fileManager.attributesOfFileSystemForPath(location.path, null)
        
        val totalSize = (attributes?.get("NSFileSystemSize") as? NSNumber)?.longLongValue ?: 0L
        val freeSize = (attributes?.get("NSFileSystemFreeSize") as? NSNumber)?.longLongValue ?: 0L
        
        val totalMB = totalSize / (1024 * 1024)
        val availableMB = freeSize / (1024 * 1024)
        val usedMB = totalMB - availableMB
        val usedPercent = if (totalMB > 0) ((usedMB * 100) / totalMB).toInt() else 0
        
        _storageInfo.value = StorageInfo(
            totalSpaceMB = totalMB,
            availableSpaceMB = availableMB,
            usedSpaceMB = usedMB,
            usedPercent = usedPercent,
            locationType = location.type,
            locationName = location.name,
            isLowSpace = availableMB < 500,
            isCriticalSpace = availableMB < 100
        )
    }
    
    private fun isStorageAvailableAtPath(path: String): Boolean {
        return try {
            val fileManager = NSFileManager.defaultManager
            fileManager.isWritableFileAtPath(path)
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getDocumentsPath(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true
        )
        return (paths.firstOrNull() as? String) ?: ""
    }
}

actual fun createStorageManager(): StorageManager {
    return IOSStorageManager()
}
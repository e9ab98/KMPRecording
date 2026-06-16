package com.e9ab98.kmprecording.service

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.e9ab98.kmprecording.model.StorageInfo
import com.e9ab98.kmprecording.model.StorageLocation
import com.e9ab98.kmprecording.model.StorageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AndroidStorageManager(
    private val context: Context
) : StorageManager {
    
    private val _availableLocations = MutableStateFlow<List<StorageLocation>>(emptyList())
    private val _currentLocation = MutableStateFlow<StorageLocation?>(null)
    private val _storageInfo = MutableStateFlow<StorageInfo?>(null)
    
    override val availableLocations: StateFlow<List<StorageLocation>> = _availableLocations
    override val currentLocation: StateFlow<StorageLocation> = MutableStateFlow(
        StorageLocation(
            id = "internal_default",
            name = "内部存储",
            path = getDefaultInternalPath(),
            type = StorageType.INTERNAL,
            totalSpaceMB = 0,
            availableSpaceMB = 0
        )
    )
    override val storageInfo: StateFlow<StorageInfo> = MutableStateFlow(
        StorageInfo(
            totalSpaceMB = 0,
            availableSpaceMB = 0,
            usedSpaceMB = 0,
            usedPercent = 0,
            locationType = StorageType.INTERNAL,
            locationName = "内部存储"
        )
    )
    
    private val prefs = context.getSharedPreferences("storage_prefs", Context.MODE_PRIVATE)
    
    init {
        detectAvailableStorage()
        loadSavedLocation()
    }
    
    override fun detectAvailableStorage() {
        val locations = mutableListOf<StorageLocation>()
        
        val internalPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val internalStat = StatFs(internalPath.absolutePath)
        val internalTotalMB = internalStat.totalBytes / (1024 * 1024)
        val internalAvailableMB = internalStat.availableBytes / (1024 * 1024)
        
        locations.add(
            StorageLocation(
                id = "internal",
                name = "内部存储",
                path = internalPath.absolutePath,
                type = StorageType.INTERNAL,
                totalSpaceMB = internalTotalMB,
                availableSpaceMB = internalAvailableMB,
                isRemovable = false,
                isSelected = prefs.getString("selected_storage", "internal") == "internal"
            )
        )
        
        val externalDirs = context.getExternalFilesDirs(Environment.DIRECTORY_MOVIES)
        externalDirs.forEachIndexed { index, dir ->
            if (dir != null && !dir.absolutePath.contains("emulated")) {
                val sdCardStat = StatFs(dir.absolutePath)
                val sdCardTotalMB = sdCardStat.totalBytes / (1024 * 1024)
                val sdCardAvailableMB = sdCardStat.availableBytes / (1024 * 1024)
                
                val sdCardName = detectSdCardName(dir.absolutePath)
                
                locations.add(
                    StorageLocation(
                        id = "sd_card_$index",
                        name = sdCardName,
                        path = dir.absolutePath,
                        type = StorageType.SD_CARD,
                        totalSpaceMB = sdCardTotalMB,
                        availableSpaceMB = sdCardAvailableMB,
                        isRemovable = true,
                        isSelected = prefs.getString("selected_storage", "internal") == "sd_card_$index"
                    )
                )
            }
        }
        
        _availableLocations.value = locations
        
        val selectedId = prefs.getString("selected_storage", "internal") ?: "internal"
        val selectedLocation = locations.find { it.id == selectedId } ?: locations.first()
        (currentLocation as MutableStateFlow).value = selectedLocation
        
        updateStorageInfo(selectedLocation)
    }
    
    override fun selectStorageLocation(location: StorageLocation): Result<Boolean> {
        return try {
            if (!isStorageAvailableAtPath(location.path)) {
                return Result.failure(Exception("存储位置不可用"))
            }
            
            prefs.edit().putString("selected_storage", location.id).apply()
            
            val updatedLocations = _availableLocations.value.map { loc ->
                loc.copy(isSelected = loc.id == location.id)
            }
            _availableLocations.value = updatedLocations
            
            (currentLocation as MutableStateFlow).value = location
            updateStorageInfo(location)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getStoragePath(): String {
        return currentLocation.value.path
    }
    
    override fun getAvailableSpaceMB(): Long {
        return currentLocation.value.availableSpaceMB
    }
    
    override fun getTotalSpaceMB(): Long {
        return currentLocation.value.totalSpaceMB
    }
    
    override fun isStorageAvailable(): Boolean {
        return isStorageAvailableAtPath(currentLocation.value.path)
    }
    
    private fun updateStorageInfo(location: StorageLocation) {
        val stat = StatFs(location.path)
        val totalMB = stat.totalBytes / (1024 * 1024)
        val availableMB = stat.availableBytes / (1024 * 1024)
        val usedMB = totalMB - availableMB
        val usedPercent = if (totalMB > 0) ((usedMB * 100) / totalMB).toInt() else 0
        
        (storageInfo as MutableStateFlow).value = StorageInfo(
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
            val stat = StatFs(path)
            stat.availableBytes > 0
        } catch (e: Exception) {
            false
        }
    }
    
    private fun detectSdCardName(path: String): String {
        return when {
            path.contains("sdcard") -> "SD卡"
            path.contains("external") -> "外置存储"
            path.contains("storage") -> {
                val parts = path.split("/")
                if (parts.size > 2) {
                    val storageId = parts[2]
                    when (storageId) {
                        "sdcard0" -> "SD卡"
                        "sdcard1" -> "SD卡2"
                        else -> "存储设备 ($storageId)"
                    }
                } else {
                    "外置存储"
                }
            }
            else -> "外置存储"
        }
    }
    
    private fun getDefaultInternalPath(): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath
    }
    
    private fun loadSavedLocation() {
        val savedId = prefs.getString("selected_storage", "internal") ?: "internal"
        val location = _availableLocations.value.find { it.id == savedId }
        if (location != null) {
            (currentLocation as MutableStateFlow).value = location
            updateStorageInfo(location)
        }
    }
}

actual fun createStorageManager(): StorageManager {
    return AndroidStorageManager(ContextHolder.context)
}
package com.e9ab98.kmprecording.model

data class StorageLocation(
    val id: String,
    val name: String,
    val path: String,
    val type: StorageType,
    val totalSpaceMB: Long,
    val availableSpaceMB: Long,
    val isRemovable: Boolean = false,
    val isSelected: Boolean = false,
    val isNetworkStorage: Boolean = false,
    val requiresConnection: Boolean = false
)

enum class StorageType {
    INTERNAL,
    SD_CARD,
    USB,
    WEBDAV,
    FTP,
    SFTP,
    UNKNOWN
}

data class StorageInfo(
    val totalSpaceMB: Long,
    val availableSpaceMB: Long,
    val usedSpaceMB: Long,
    val usedPercent: Int,
    val locationType: StorageType,
    val locationName: String,
    val isLowSpace: Boolean = availableSpaceMB < 500,
    val isCriticalSpace: Boolean = availableSpaceMB < 100,
    val isNetworkStorage: Boolean = false,
    val isConnected: Boolean = true
) {
    val statusText: String
        get() = when {
            !isConnected && isNetworkStorage -> "网络存储未连接"
            isCriticalSpace -> "存储空间严重不足"
            isLowSpace -> "存储空间不足"
            usedPercent > 80 -> "存储空间紧张"
            usedPercent > 50 -> "存储空间适中"
            else -> "存储空间充足"
        }
}
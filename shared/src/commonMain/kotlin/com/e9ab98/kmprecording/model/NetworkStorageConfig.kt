package com.e9ab98.kmprecording.model

data class WebDAVConfig(
    val id: String = "webdav_default",
    val name: String = "WebDAV",
    val serverUrl: String,
    val username: String,
    val password: String,
    val basePath: String = "/KMPRecording",
    val useSSL: Boolean = true,
    val port: Int = 0,
    val autoUpload: Boolean = false,
    val uploadEmergencyOnly: Boolean = true,
    val maxConcurrentUploads: Int = 2,
    val uploadTimeoutSeconds: Int = 60,
    val isEnabled: Boolean = false,
    val isConnected: Boolean = false
)

data class FTPConfig(
    val id: String = "ftp_default",
    val name: String = "FTP",
    val serverUrl: String,
    val username: String,
    val password: String,
    val basePath: String = "/KMPRecording",
    val port: Int = 21,
    val useSSL: Boolean = false,
    val autoUpload: Boolean = false,
    val uploadEmergencyOnly: Boolean = true,
    val maxConcurrentUploads: Int = 2,
    val uploadTimeoutSeconds: Int = 60,
    val isEnabled: Boolean = false,
    val isConnected: Boolean = false
)

data class SFTPConfig(
    val id: String = "sftp_default",
    val name: String = "SFTP",
    val serverUrl: String,
    val username: String,
    val password: String,
    val basePath: String = "/KMPRecording",
    val port: Int = 22,
    val privateKeyPath: String? = null,
    val autoUpload: Boolean = false,
    val uploadEmergencyOnly: Boolean = true,
    val maxConcurrentUploads: Int = 2,
    val uploadTimeoutSeconds: Int = 60,
    val isEnabled: Boolean = false,
    val isConnected: Boolean = false
)

data class NetworkStorageConfig(
    val webDAV: WebDAVConfig? = null,
    val ftp: FTPConfig? = null,
    val sftp: SFTPConfig? = null
)

data class RemoteFileInfo(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean
)

enum class UploadStatus {
    PENDING,
    UPLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class UploadTask(
    val id: String,
    val localPath: String,
    val remotePath: String,
    val storageType: StorageType,
    val status: UploadStatus,
    val progress: Int = 0,
    val errorMessage: String? = null,
    val startTime: Long = 0,
    val endTime: Long = 0
)
package com.e9ab98.kmprecording.service

import com.e9ab98.kmprecording.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

interface NetworkStorageManager {
    val webDAVConfig: StateFlow<WebDAVConfig?>
    val ftpConfig: StateFlow<FTPConfig?>
    val sftpConfig: StateFlow<SFTPConfig?>
    val uploadTasks: StateFlow<List<UploadTask>>
    val isUploading: StateFlow<Boolean>
    
    suspend fun saveWebDAVConfig(config: WebDAVConfig): Result<Boolean>
    suspend fun saveFTPConfig(config: FTPConfig): Result<Boolean>
    suspend fun saveSFTPConfig(config: SFTPConfig): Result<Boolean>
    
    suspend fun testWebDAVConnection(): Result<Boolean>
    suspend fun testFTPConnection(): Result<Boolean>
    suspend fun testSFTPConnection(): Result<Boolean>
    
    suspend fun uploadSegment(
        segment: VideoSegment,
        storageType: StorageType
    ): Result<UploadTask>
    
    suspend fun uploadEmergencySegments(
        sessionId: String
    ): Result<List<UploadTask>>
    
    suspend fun cancelUpload(taskId: String): Result<Boolean>
    suspend fun retryUpload(taskId: String): Result<Boolean>
    
    fun getUploadProgress(taskId: String): Int
    fun getActiveUploadCount(): Int
    
    suspend fun loadConfigs(): Result<NetworkStorageConfig>
    suspend fun clearConfigs(): Result<Boolean>
}

expect fun createNetworkStorageManager(): NetworkStorageManager
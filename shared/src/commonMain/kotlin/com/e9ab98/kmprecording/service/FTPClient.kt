package com.e9ab98.kmprecording.service

import com.e9ab98.kmprecording.model.FTPConfig
import com.e9ab98.kmprecording.model.RemoteFileInfo
import com.e9ab98.kmprecording.model.SFTPConfig
import com.e9ab98.kmprecording.model.StorageInfo

interface FTPClient {
    val config: FTPConfig
    val isConnected: Boolean
    
    suspend fun connect(): Result<Boolean>
    suspend fun disconnect(): Result<Boolean>
    suspend fun testConnection(): Result<Boolean>
    
    suspend fun uploadFile(
        localPath: String,
        remotePath: String,
        onProgress: ((Int) -> Unit)? = null
    ): Result<Boolean>
    
    suspend fun downloadFile(
        remotePath: String,
        localPath: String,
        onProgress: ((Int) -> Unit)? = null
    ): Result<Boolean>
    
    suspend fun deleteFile(remotePath: String): Result<Boolean>
    suspend fun createDirectory(remotePath: String): Result<Boolean>
    suspend fun listFiles(remotePath: String): Result<List<RemoteFileInfo>>
    suspend fun getStorageInfo(): Result<StorageInfo>
    suspend fun fileExists(remotePath: String): Result<Boolean>
}

interface SFTPClient {
    val config: SFTPConfig
    val isConnected: Boolean
    
    suspend fun connect(): Result<Boolean>
    suspend fun disconnect(): Result<Boolean>
    suspend fun testConnection(): Result<Boolean>
    
    suspend fun uploadFile(
        localPath: String,
        remotePath: String,
        onProgress: ((Int) -> Unit)? = null
    ): Result<Boolean>
    
    suspend fun downloadFile(
        remotePath: String,
        localPath: String,
        onProgress: ((Int) -> Unit)? = null
    ): Result<Boolean>
    
    suspend fun deleteFile(remotePath: String): Result<Boolean>
    suspend fun createDirectory(remotePath: String): Result<Boolean>
    suspend fun listFiles(remotePath: String): Result<List<RemoteFileInfo>>
    suspend fun getStorageInfo(): Result<StorageInfo>
    suspend fun fileExists(remotePath: String): Result<Boolean>
}

expect fun createFTPClient(config: FTPConfig): FTPClient
expect fun createSFTPClient(config: SFTPConfig): SFTPClient
package com.e9ab98.kmprecording.service

import android.content.Context
import com.e9ab98.kmprecording.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.random.Random

class AndroidNetworkStorageManager(
    private val context: Context
) : NetworkStorageManager {
    
    private val prefs = context.getSharedPreferences("network_storage_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _webDAVConfig = MutableStateFlow<WebDAVConfig?>(null)
    private val _ftpConfig = MutableStateFlow<FTPConfig?>(null)
    private val _sftpConfig = MutableStateFlow<SFTPConfig?>(null)
    private val _uploadTasks = MutableStateFlow<List<UploadTask>>(emptyList())
    private val _isUploading = MutableStateFlow(false)
    
    override val webDAVConfig: StateFlow<WebDAVConfig?> = _webDAVConfig
    override val ftpConfig: StateFlow<FTPConfig?> = _ftpConfig
    override val sftpConfig: StateFlow<SFTPConfig?> = _sftpConfig
    override val uploadTasks: StateFlow<List<UploadTask>> = _uploadTasks
    override val isUploading: StateFlow<Boolean> = _isUploading
    
    private val activeUploads = mutableMapOf<String, UploadTask>()
    
    init {
        scope.launch {
            loadConfigs()
        }
    }
    
    override suspend fun saveWebDAVConfig(config: WebDAVConfig): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                prefs.edit().putString("webdav_config", serializeWebDAVConfig(config)).apply()
                _webDAVConfig.value = config
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun saveFTPConfig(config: FTPConfig): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                prefs.edit().putString("ftp_config", serializeFTPConfig(config)).apply()
                _ftpConfig.value = config
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun saveSFTPConfig(config: SFTPConfig): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                prefs.edit().putString("sftp_config", serializeSFTPConfig(config)).apply()
                _sftpConfig.value = config
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun testWebDAVConnection(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val config = _webDAVConfig.value
                if (config == null || !config.isEnabled) {
                    Result.failure(Exception("WebDAV not configured"))
                } else {
                    val client = createWebDAVClient(config)
                    client.testConnection()
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun testFTPConnection(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val config = _ftpConfig.value
                if (config == null || !config.isEnabled) {
                    Result.failure(Exception("FTP not configured"))
                } else {
                    val client = createFTPClient(config)
                    client.testConnection()
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun testSFTPConnection(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val config = _sftpConfig.value
                if (config == null || !config.isEnabled) {
                    Result.failure(Exception("SFTP not configured"))
                } else {
                    val client = createSFTPClient(config)
                    client.testConnection()
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun uploadSegment(
        segment: VideoSegment,
        storageType: StorageType
    ): Result<UploadTask> {
        return withContext(Dispatchers.IO) {
            try {
                val taskId = "upload_${Random.nextLong().toString(16)}"
                val remotePath = "/${segment.sessionId}/${segment.id}.mp4"
                
                val task = UploadTask(
                    id = taskId,
                    localPath = segment.filePath,
                    remotePath = remotePath,
                    storageType = storageType,
                    status = UploadStatus.PENDING,
                    startTime = System.currentTimeMillis()
                )
                
                activeUploads[taskId] = task
                _uploadTasks.value = activeUploads.values.toList()
                
                scope.launch {
                    executeUpload(task, storageType)
                }
                
                Result.success(task)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun uploadEmergencySegments(sessionId: String): Result<List<UploadTask>> {
        return withContext(Dispatchers.IO) {
            try {
                val tasks = mutableListOf<UploadTask>()
                
                val webDAVEnabled = _webDAVConfig.value?.isEnabled == true
                val ftpEnabled = _ftpConfig.value?.isEnabled == true
                val sftpEnabled = _sftpConfig.value?.isEnabled == true
                
                if (!webDAVEnabled && !ftpEnabled && !sftpEnabled) {
                    Result.failure(Exception("No network storage configured"))
                } else {
                    val storageType = when {
                        webDAVEnabled -> StorageType.WEBDAV
                        sftpEnabled -> StorageType.SFTP
                        ftpEnabled -> StorageType.FTP
                        else -> StorageType.WEBDAV
                    }
                    
                    val segmentRecorder = createSegmentRecorder()
                    val segments = segmentRecorder.getAllSegments()
                        .filter { it.sessionId == sessionId && it.isEmergency }
                    
                    for (segment in segments) {
                        val task = uploadSegment(segment, storageType)
                        if (task.isSuccess) {
                            tasks.add(task.getOrThrow())
                        }
                    }
                    
                    Result.success(tasks)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun cancelUpload(taskId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val task = activeUploads[taskId]
                if (task != null) {
                    activeUploads.remove(taskId)
                    _uploadTasks.value = activeUploads.values.toList()
                    Result.success(true)
                } else {
                    Result.failure(Exception("Task not found"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun retryUpload(taskId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val task = activeUploads[taskId]
                if (task != null && task.status == UploadStatus.FAILED) {
                    val updatedTask = task.copy(
                        status = UploadStatus.PENDING,
                        errorMessage = null
                    )
                    activeUploads[taskId] = updatedTask
                    _uploadTasks.value = activeUploads.values.toList()
                    
                    scope.launch {
                        executeUpload(updatedTask, task.storageType)
                    }
                    
                    Result.success(true)
                } else {
                    Result.failure(Exception("Task not found or not failed"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override fun getUploadProgress(taskId: String): Int {
        return activeUploads[taskId]?.progress ?: 0
    }
    
    override fun getActiveUploadCount(): Int {
        return activeUploads.values.count { 
            it.status == UploadStatus.UPLOADING || it.status == UploadStatus.PENDING 
        }
    }
    
    override suspend fun loadConfigs(): Result<NetworkStorageConfig> {
        return withContext(Dispatchers.IO) {
            try {
                val webDAVJson = prefs.getString("webdav_config", null)
                val ftpJson = prefs.getString("ftp_config", null)
                val sftpJson = prefs.getString("sftp_config", null)
                
                val webDAV = webDAVJson?.let { deserializeWebDAVConfig(it) }
                val ftp = ftpJson?.let { deserializeFTPConfig(it) }
                val sftp = sftpJson?.let { deserializeSFTPConfig(it) }
                
                _webDAVConfig.value = webDAV
                _ftpConfig.value = ftp
                _sftpConfig.value = sftp
                
                Result.success(NetworkStorageConfig(webDAV, ftp, sftp))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun clearConfigs(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                prefs.edit().clear().apply()
                _webDAVConfig.value = null
                _ftpConfig.value = null
                _sftpConfig.value = null
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private suspend fun executeUpload(task: UploadTask, storageType: StorageType) {
        try {
            val uploadingTask = task.copy(status = UploadStatus.UPLOADING)
            activeUploads[task.id] = uploadingTask
            _uploadTasks.value = activeUploads.values.toList()
            _isUploading.value = true
            
            val result = when (storageType) {
                StorageType.WEBDAV -> {
                    val config = _webDAVConfig.value
                    if (config != null) {
                        val client = createWebDAVClient(config)
                        client.uploadFile(task.localPath, task.remotePath) { progress ->
                            updateProgress(task.id, progress)
                        }
                    } else {
                        Result.failure(Exception("WebDAV not configured"))
                    }
                }
                StorageType.FTP -> {
                    val config = _ftpConfig.value
                    if (config != null) {
                        val client = createFTPClient(config)
                        client.uploadFile(task.localPath, task.remotePath) { progress ->
                            updateProgress(task.id, progress)
                        }
                    } else {
                        Result.failure(Exception("FTP not configured"))
                    }
                }
                StorageType.SFTP -> {
                    val config = _sftpConfig.value
                    if (config != null) {
                        val client = createSFTPClient(config)
                        client.uploadFile(task.localPath, task.remotePath) { progress ->
                            updateProgress(task.id, progress)
                        }
                    } else {
                        Result.failure(Exception("SFTP not configured"))
                    }
                }
                else -> Result.failure(Exception("Unsupported storage type"))
            }
            
            if (result.isSuccess) {
                val completedTask = task.copy(
                    status = UploadStatus.COMPLETED,
                    progress = 100,
                    endTime = System.currentTimeMillis()
                )
                activeUploads[task.id] = completedTask
            } else {
                val failedTask = task.copy(
                    status = UploadStatus.FAILED,
                    errorMessage = result.exceptionOrNull()?.message,
                    endTime = System.currentTimeMillis()
                )
                activeUploads[task.id] = failedTask
            }
            
            _uploadTasks.value = activeUploads.values.toList()
            _isUploading.value = getActiveUploadCount() > 0
            
        } catch (e: Exception) {
            val failedTask = task.copy(
                status = UploadStatus.FAILED,
                errorMessage = e.message,
                endTime = System.currentTimeMillis()
            )
            activeUploads[task.id] = failedTask
            _uploadTasks.value = activeUploads.values.toList()
            _isUploading.value = getActiveUploadCount() > 0
        }
    }
    
    private fun updateProgress(taskId: String, progress: Int) {
        val task = activeUploads[taskId]
        if (task != null) {
            val updatedTask = task.copy(progress = progress)
            activeUploads[taskId] = updatedTask
            _uploadTasks.value = activeUploads.values.toList()
        }
    }
    
    private fun serializeWebDAVConfig(config: WebDAVConfig): String {
        val json = JSONObject()
        json.put("id", config.id)
        json.put("name", config.name)
        json.put("serverUrl", config.serverUrl)
        json.put("username", config.username)
        json.put("password", config.password)
        json.put("basePath", config.basePath)
        json.put("useSSL", config.useSSL)
        json.put("port", config.port)
        json.put("autoUpload", config.autoUpload)
        json.put("uploadEmergencyOnly", config.uploadEmergencyOnly)
        json.put("maxConcurrentUploads", config.maxConcurrentUploads)
        json.put("uploadTimeoutSeconds", config.uploadTimeoutSeconds)
        json.put("isEnabled", config.isEnabled)
        return json.toString()
    }
    
    private fun deserializeWebDAVConfig(json: String): WebDAVConfig {
        val obj = JSONObject(json)
        return WebDAVConfig(
            id = obj.getString("id"),
            name = obj.getString("name"),
            serverUrl = obj.getString("serverUrl"),
            username = obj.getString("username"),
            password = obj.getString("password"),
            basePath = obj.getString("basePath"),
            useSSL = obj.getBoolean("useSSL"),
            port = obj.getInt("port"),
            autoUpload = obj.getBoolean("autoUpload"),
            uploadEmergencyOnly = obj.getBoolean("uploadEmergencyOnly"),
            maxConcurrentUploads = obj.getInt("maxConcurrentUploads"),
            uploadTimeoutSeconds = obj.getInt("uploadTimeoutSeconds"),
            isEnabled = obj.getBoolean("isEnabled")
        )
    }
    
    private fun serializeFTPConfig(config: FTPConfig): String {
        val json = JSONObject()
        json.put("id", config.id)
        json.put("name", config.name)
        json.put("serverUrl", config.serverUrl)
        json.put("username", config.username)
        json.put("password", config.password)
        json.put("basePath", config.basePath)
        json.put("port", config.port)
        json.put("useSSL", config.useSSL)
        json.put("autoUpload", config.autoUpload)
        json.put("uploadEmergencyOnly", config.uploadEmergencyOnly)
        json.put("maxConcurrentUploads", config.maxConcurrentUploads)
        json.put("uploadTimeoutSeconds", config.uploadTimeoutSeconds)
        json.put("isEnabled", config.isEnabled)
        return json.toString()
    }
    
    private fun deserializeFTPConfig(json: String): FTPConfig {
        val obj = JSONObject(json)
        return FTPConfig(
            id = obj.getString("id"),
            name = obj.getString("name"),
            serverUrl = obj.getString("serverUrl"),
            username = obj.getString("username"),
            password = obj.getString("password"),
            basePath = obj.getString("basePath"),
            port = obj.getInt("port"),
            useSSL = obj.getBoolean("useSSL"),
            autoUpload = obj.getBoolean("autoUpload"),
            uploadEmergencyOnly = obj.getBoolean("uploadEmergencyOnly"),
            maxConcurrentUploads = obj.getInt("maxConcurrentUploads"),
            uploadTimeoutSeconds = obj.getInt("uploadTimeoutSeconds"),
            isEnabled = obj.getBoolean("isEnabled")
        )
    }
    
    private fun serializeSFTPConfig(config: SFTPConfig): String {
        val json = JSONObject()
        json.put("id", config.id)
        json.put("name", config.name)
        json.put("serverUrl", config.serverUrl)
        json.put("username", config.username)
        json.put("password", config.password)
        json.put("basePath", config.basePath)
        json.put("port", config.port)
        json.put("privateKeyPath", config.privateKeyPath ?: "")
        json.put("autoUpload", config.autoUpload)
        json.put("uploadEmergencyOnly", config.uploadEmergencyOnly)
        json.put("maxConcurrentUploads", config.maxConcurrentUploads)
        json.put("uploadTimeoutSeconds", config.uploadTimeoutSeconds)
        json.put("isEnabled", config.isEnabled)
        return json.toString()
    }
    
    private fun deserializeSFTPConfig(json: String): SFTPConfig {
        val obj = JSONObject(json)
        return SFTPConfig(
            id = obj.getString("id"),
            name = obj.getString("name"),
            serverUrl = obj.getString("serverUrl"),
            username = obj.getString("username"),
            password = obj.getString("password"),
            basePath = obj.getString("basePath"),
            port = obj.getInt("port"),
            privateKeyPath = obj.optString("privateKeyPath", null),
            autoUpload = obj.getBoolean("autoUpload"),
            uploadEmergencyOnly = obj.getBoolean("uploadEmergencyOnly"),
            maxConcurrentUploads = obj.getInt("maxConcurrentUploads"),
            uploadTimeoutSeconds = obj.getInt("uploadTimeoutSeconds"),
            isEnabled = obj.getBoolean("isEnabled")
        )
    }
}

actual fun createNetworkStorageManager(): NetworkStorageManager {
    return AndroidNetworkStorageManager(ContextHolder.context)
}
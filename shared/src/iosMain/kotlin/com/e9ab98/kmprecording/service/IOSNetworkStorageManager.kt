package com.e9ab98.kmprecording.service

import com.e9ab98.kmprecording.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.CoreFoundation.kCFAbsoluteTimeIntervalSince1970
import platform.Foundation.NSDate
import platform.Foundation.NSUserDefaults
import kotlin.random.Random

import kotlin.math.pow

import kotlin.math.sqrt

import kotlin.math.floor

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class IOSNetworkStorageManager : NetworkStorageManager {
    
    private val prefs = NSUserDefaults.standardUserDefaults
    private val scope = CoroutineScope(Dispatchers.Default)
    
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
    
    private fun getCurrentTimeMillis(): Long {
        return (NSDate().timeIntervalSinceReferenceDate + kCFAbsoluteTimeIntervalSince1970).toLong() * 1000
    }
    
    private fun getCurrentLocalDateTime(): LocalDateTime {
        val instant = Instant.fromEpochMilliseconds(getCurrentTimeMillis())
        return instant.toLocalDateTime(TimeZone.currentSystemDefault())
    }
    
    override suspend fun saveWebDAVConfig(config: WebDAVConfig): Result<Boolean> {
        return withContext(Dispatchers.Default) {
            try {
                prefs.setObject(serializeWebDAVConfig(config), forKey = "webdav_config")
                _webDAVConfig.value = config
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun saveFTPConfig(config: FTPConfig): Result<Boolean> {
        return withContext(Dispatchers.Default) {
            try {
                prefs.setObject(serializeFTPConfig(config), forKey = "ftp_config")
                _ftpConfig.value = config
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun saveSFTPConfig(config: SFTPConfig): Result<Boolean> {
        return withContext(Dispatchers.Default) {
            try {
                prefs.setObject(serializeSFTPConfig(config), forKey = "sftp_config")
                _sftpConfig.value = config
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun testWebDAVConnection(): Result<Boolean> {
        return withContext(Dispatchers.Default) {
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
        return withContext(Dispatchers.Default) {
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
        return withContext(Dispatchers.Default) {
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
        return withContext(Dispatchers.Default) {
            try {
                val taskId = "upload_${Random.nextLong().toString(16)}"
                val remotePath = "/${segment.sessionId}/${segment.id}.mp4"
                
                val now = getCurrentLocalDateTime()
                val task = UploadTask(
                    id = taskId,
                    localPath = segment.filePath,
                    remotePath = remotePath,
                    storageType = storageType,
                    status = UploadStatus.PENDING,
                    startTime = getCurrentTimeMillis()
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
        return withContext(Dispatchers.Default) {
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
        return withContext(Dispatchers.Default) {
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
        return withContext(Dispatchers.Default) {
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
        return withContext(Dispatchers.Default) {
            try {
                val webDAVJson = prefs.stringForKey("webdav_config")
                val ftpJson = prefs.stringForKey("ftp_config")
                val sftpJson = prefs.stringForKey("sftp_config")
                
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
        return withContext(Dispatchers.Default) {
            try {
                prefs.removeObjectForKey("webdav_config")
                prefs.removeObjectForKey("ftp_config")
                prefs.removeObjectForKey("sftp_config")
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
            
            val now = getCurrentTimeMillis()
            
            if (result.isSuccess) {
                val completedTask = task.copy(
                    status = UploadStatus.COMPLETED,
                    progress = 100,
                    endTime = now
                )
                activeUploads[task.id] = completedTask
            } else {
                val failedTask = task.copy(
                    status = UploadStatus.FAILED,
                    errorMessage = result.exceptionOrNull()?.message,
                    endTime = now
                )
                activeUploads[task.id] = failedTask
            }
            
            _uploadTasks.value = activeUploads.values.toList()
            _isUploading.value = getActiveUploadCount() > 0
            
        } catch (e: Exception) {
            val now = getCurrentTimeMillis()
            val failedTask = task.copy(
                status = UploadStatus.FAILED,
                errorMessage = e.message,
                endTime = now
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
        return buildString {
            append("{")
            append("\"id\":\"${config.id}\",")
            append("\"name\":\"${config.name}\",")
            append("\"serverUrl\":\"${config.serverUrl}\",")
            append("\"username\":\"${config.username}\",")
            append("\"password\":\"${config.password}\",")
            append("\"basePath\":\"${config.basePath}\",")
            append("\"useSSL\":${config.useSSL},")
            append("\"port\":${config.port},")
            append("\"autoUpload\":${config.autoUpload},")
            append("\"uploadEmergencyOnly\":${config.uploadEmergencyOnly},")
            append("\"maxConcurrentUploads\":${config.maxConcurrentUploads},")
            append("\"uploadTimeoutSeconds\":${config.uploadTimeoutSeconds},")
            append("\"isEnabled\":${config.isEnabled}")
            append("}")
        }
    }
    
    private fun deserializeWebDAVConfig(json: String): WebDAVConfig {
        val obj = parseJson(json)
        return WebDAVConfig(
            id = obj["id"] as String,
            name = obj["name"] as String,
            serverUrl = obj["serverUrl"] as String,
            username = obj["username"] as String,
            password = obj["password"] as String,
            basePath = obj["basePath"] as String,
            useSSL = obj["useSSL"] as Boolean,
            port = (obj["port"] as Number).toInt(),
            autoUpload = obj["autoUpload"] as Boolean,
            uploadEmergencyOnly = obj["uploadEmergencyOnly"] as Boolean,
            maxConcurrentUploads = (obj["maxConcurrentUploads"] as Number).toInt(),
            uploadTimeoutSeconds = (obj["uploadTimeoutSeconds"] as Number).toInt(),
            isEnabled = obj["isEnabled"] as Boolean
        )
    }
    
    private fun serializeFTPConfig(config: FTPConfig): String {
        return buildString {
            append("{")
            append("\"id\":\"${config.id}\",")
            append("\"name\":\"${config.name}\",")
            append("\"serverUrl\":\"${config.serverUrl}\",")
            append("\"username\":\"${config.username}\",")
            append("\"password\":\"${config.password}\",")
            append("\"basePath\":\"${config.basePath}\",")
            append("\"port\":${config.port},")
            append("\"useSSL\":${config.useSSL},")
            append("\"autoUpload\":${config.autoUpload},")
            append("\"uploadEmergencyOnly\":${config.uploadEmergencyOnly},")
            append("\"maxConcurrentUploads\":${config.maxConcurrentUploads},")
            append("\"uploadTimeoutSeconds\":${config.uploadTimeoutSeconds},")
            append("\"isEnabled\":${config.isEnabled}")
            append("}")
        }
    }
    
    private fun deserializeFTPConfig(json: String): FTPConfig {
        val obj = parseJson(json)
        return FTPConfig(
            id = obj["id"] as String,
            name = obj["name"] as String,
            serverUrl = obj["serverUrl"] as String,
            username = obj["username"] as String,
            password = obj["password"] as String,
            basePath = obj["basePath"] as String,
            port = (obj["port"] as Number).toInt(),
            useSSL = obj["useSSL"] as Boolean,
            autoUpload = obj["autoUpload"] as Boolean,
            uploadEmergencyOnly = obj["uploadEmergencyOnly"] as Boolean,
            maxConcurrentUploads = (obj["maxConcurrentUploads"] as Number).toInt(),
            uploadTimeoutSeconds = (obj["uploadTimeoutSeconds"] as Number).toInt(),
            isEnabled = obj["isEnabled"] as Boolean
        )
    }
    
    private fun serializeSFTPConfig(config: SFTPConfig): String {
        return buildString {
            append("{")
            append("\"id\":\"${config.id}\",")
            append("\"name\":\"${config.name}\",")
            append("\"serverUrl\":\"${config.serverUrl}\",")
            append("\"username\":\"${config.username}\",")
            append("\"password\":\"${config.password}\",")
            append("\"basePath\":\"${config.basePath}\",")
            append("\"port\":${config.port},")
            append("\"privateKeyPath\":\"${config.privateKeyPath ?: ""}\",")
            append("\"autoUpload\":${config.autoUpload},")
            append("\"uploadEmergencyOnly\":${config.uploadEmergencyOnly},")
            append("\"maxConcurrentUploads\":${config.maxConcurrentUploads},")
            append("\"uploadTimeoutSeconds\":${config.uploadTimeoutSeconds},")
            append("\"isEnabled\":${config.isEnabled}")
            append("}")
        }
    }
    
    private fun deserializeSFTPConfig(json: String): SFTPConfig {
        val obj = parseJson(json)
        return SFTPConfig(
            id = obj["id"] as String,
            name = obj["name"] as String,
            serverUrl = obj["serverUrl"] as String,
            username = obj["username"] as String,
            password = obj["password"] as String,
            basePath = obj["basePath"] as String,
            port = (obj["port"] as Number).toInt(),
            privateKeyPath = (obj["privateKeyPath"] as? String)?.takeIf { it.isNotEmpty() },
            autoUpload = obj["autoUpload"] as Boolean,
            uploadEmergencyOnly = obj["uploadEmergencyOnly"] as Boolean,
            maxConcurrentUploads = (obj["maxConcurrentUploads"] as Number).toInt(),
            uploadTimeoutSeconds = (obj["uploadTimeoutSeconds"] as Number).toInt(),
            isEnabled = obj["isEnabled"] as Boolean
        )
    }
    
    private fun parseJson(json: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val content = json.removeSurrounding("{", "}")
        val pairs = content.split(",")
        
        for (pair in pairs) {
            val keyValue = pair.split(":")
            if (keyValue.size == 2) {
                val key = keyValue[0].removeSurrounding("\"")
                val valueStr = keyValue[1]
                
                val value: Any = when {
                    valueStr.startsWith("\"") && valueStr.endsWith("\"") -> 
                        valueStr.removeSurrounding("\"")
                    valueStr == "true" -> true
                    valueStr == "false" -> false
                    valueStr.toIntOrNull() != null -> valueStr.toInt()
                    valueStr.toDoubleOrNull() != null -> valueStr.toDouble()
                    else -> valueStr
                }
                
                map[key] = value
            }
        }
        
        return map
    }
}

actual fun createNetworkStorageManager(): NetworkStorageManager {
    return IOSNetworkStorageManager()
}
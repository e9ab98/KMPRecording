package com.e9ab98.kmprecording.service

import com.e9ab98.kmprecording.model.RemoteFileInfo
import com.e9ab98.kmprecording.model.StorageInfo
import com.e9ab98.kmprecording.model.StorageType
import com.e9ab98.kmprecording.model.WebDAVConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class AndroidWebDAVClient(
    override val config: WebDAVConfig
) : WebDAVClient {
    
    private var _isConnected = false
    override val isConnected: Boolean = _isConnected
    
    override suspend fun connect(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val url = buildUrl(config.basePath)
                val connection = createConnection(url, "GET")
                _isConnected = connection.responseCode in 200..299
                connection.disconnect()
                Result.success(_isConnected)
            } catch (e: Exception) {
                _isConnected = false
                Result.failure(e)
            }
        }
    }
    
    override suspend fun disconnect(): Result<Boolean> {
        _isConnected = false
        return Result.success(true)
    }
    
    override suspend fun testConnection(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val url = buildUrl("/")
                val connection = createConnection(url, "GET")
                val success = connection.responseCode in 200..299
                connection.disconnect()
                Result.success(success)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun uploadFile(
        localPath: String,
        remotePath: String,
        onProgress: ((Int) -> Unit)?
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(localPath)
                if (!file.exists()) {
                    Result.failure(Exception("Local file not found"))
                } else {
                    val url = buildUrl("${config.basePath}$remotePath")
                    val connection = createConnection(url, "PUT")
                    connection.doOutput = true
                    
                    file.inputStream().use { input ->
                        connection.outputStream.use { output ->
                            val buffer = ByteArray(8192)
                            var totalRead = 0L
                            var read: Int
                            
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                totalRead += read
                                
                                onProgress?.let {
                                    val progress = (totalRead * 100 / file.length()).toInt()
                                    it(progress)
                                }
                            }
                        }
                    }
                    
                    val success = connection.responseCode in 200..299
                    connection.disconnect()
                    Result.success(success)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun downloadFile(
        remotePath: String,
        localPath: String,
        onProgress: ((Int) -> Unit)?
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val url = buildUrl("${config.basePath}$remotePath")
                val connection = createConnection(url, "GET")
                
                if (connection.responseCode !in 200..299) {
                    connection.disconnect()
                    Result.failure(Exception("Download failed: ${connection.responseCode}"))
                } else {
                    val file = File(localPath)
                    file.parentFile?.mkdirs()
                    
                    connection.inputStream.use { input ->
                        file.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var totalRead = 0L
                            var read: Int
                            val contentLength = connection.contentLengthLong
                            
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                totalRead += read
                                
                                onProgress?.let {
                                    if (contentLength > 0) {
                                        val progress = (totalRead * 100 / contentLength).toInt()
                                        it(progress)
                                    }
                                }
                            }
                        }
                    }
                    
                    connection.disconnect()
                    Result.success(true)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun deleteFile(remotePath: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val url = buildUrl("${config.basePath}$remotePath")
                val connection = createConnection(url, "DELETE")
                val success = connection.responseCode in 200..299
                connection.disconnect()
                Result.success(success)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun createDirectory(remotePath: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val url = buildUrl("${config.basePath}$remotePath")
                val connection = createConnection(url, "MKCOL")
                val success = connection.responseCode in 200..299 || connection.responseCode == 405
                connection.disconnect()
                Result.success(success)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun listFiles(remotePath: String): Result<List<RemoteFileInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = buildUrl("${config.basePath}$remotePath")
                val connection = createConnection(url, "PROPFIND")
                connection.setRequestProperty("Depth", "1")
                
                if (connection.responseCode !in 200..299) {
                    connection.disconnect()
                    Result.failure(Exception("List failed: ${connection.responseCode}"))
                } else {
                    val response = connection.inputStream.bufferedReader().readText()
                    connection.disconnect()
                    
                    val files = parseWebDAVResponse(response, remotePath)
                    Result.success(files)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun getStorageInfo(): Result<StorageInfo> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(
                    StorageInfo(
                        totalSpaceMB = 1024 * 1024,
                        availableSpaceMB = 512 * 1024,
                        usedSpaceMB = 512 * 1024,
                        usedPercent = 50,
                        locationType = StorageType.WEBDAV,
                        locationName = config.name,
                        isNetworkStorage = true,
                        isConnected = _isConnected
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun fileExists(remotePath: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val url = buildUrl("${config.basePath}$remotePath")
                val connection = createConnection(url, "HEAD")
                val success = connection.responseCode in 200..299
                connection.disconnect()
                Result.success(success)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private fun buildUrl(path: String): URL {
        val protocol = if (config.useSSL) "https" else "http"
        val portPart = if (config.port > 0) ":${config.port}" else ""
        val urlString = "$protocol://${config.serverUrl}$portPart$path"
        return URL(urlString)
    }
    
    private fun createConnection(url: URL, method: String): HttpURLConnection {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.setRequestProperty("Authorization", getAuthHeader())
        connection.connectTimeout = 30000
        connection.readTimeout = 60000
        return connection
    }
    
    private fun getAuthHeader(): String {
        val credentials = "${config.username}:${config.password}"
        val encoded = android.util.Base64.encodeToString(
            credentials.toByteArray(), 
            android.util.Base64.NO_WRAP
        )
        return "Basic $encoded"
    }
    
    private fun parseWebDAVResponse(xml: String, basePath: String): List<RemoteFileInfo> {
        val files = mutableListOf<RemoteFileInfo>()
        
        val hrefPattern = "<href>(.*?)</href>".toRegex()
        
        hrefPattern.findAll(xml).forEach { match ->
            val href = match.groupValues[1]
            val name = href.removePrefix(basePath).removeSurrounding("/")
            
            if (name.isNotEmpty()) {
                files.add(
                    RemoteFileInfo(
                        path = href,
                        name = name,
                        size = 0L,
                        lastModified = 0L,
                        isDirectory = false
                    )
                )
            }
        }
        
        return files
    }
}

actual fun createWebDAVClient(config: WebDAVConfig): WebDAVClient {
    return AndroidWebDAVClient(config)
}
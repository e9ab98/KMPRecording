@file:OptIn(ExperimentalForeignApi::class)

package com.e9ab98.kmprecording.service

import platform.Foundation.*
import kotlinx.cinterop.ExperimentalForeignApi

actual fun appendTextToFile(filePath: String, text: String) {
    val fileManager = NSFileManager.defaultManager
    if (!fileManager.fileExistsAtPath(path = filePath)) {
        writeTextToFile(filePath, text)
        return
    }
    val fileHandle = NSFileHandle.fileHandleForWritingAtPath(path = filePath)
    if (fileHandle != null) {
        try {
            fileHandle.seekToEndReturningOffset(offsetInFile = null, error = null)
            val nsString = NSString.create(string = text)
            val data = nsString.dataUsingEncoding(NSUTF8StringEncoding)
            if (data != null) {
                fileHandle.writeData(data = data, error = null)
            }
        } catch (e: Exception) {
            println("FileWriter: Failed to append to file: ${e.message}")
        } finally {
            fileHandle.closeAndReturnError(error = null)
        }
    } else {
        println("FileWriter: Could not open file handle for writing at $filePath")
    }
}

actual fun writeTextToFile(filePath: String, text: String) {
    try {
        val nsString = NSString.create(string = text)
        nsString.writeToFile(
            path = filePath,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )
    } catch (e: Exception) {
        println("FileWriter: Failed to write to file: ${e.message}")
    }
}

actual fun deleteFile(filePath: String): Boolean {
    val fileManager = NSFileManager.defaultManager
    if (fileManager.fileExistsAtPath(path = filePath)) {
        return fileManager.removeItemAtPath(path = filePath, error = null)
    }
    return false
}

actual fun readTextFromFile(filePath: String): String {
    try {
        val nsString = NSString.create(contentsOfFile = filePath, encoding = NSUTF8StringEncoding, error = null)
        return nsString?.toString() ?: ""
    } catch (e: Exception) {
        println("FileWriter: Failed to read from file: ${e.message}")
        return ""
    }
}


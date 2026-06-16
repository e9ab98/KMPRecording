package com.e9ab98.kmprecording.service

import java.io.File

actual fun appendTextToFile(filePath: String, text: String) {
    try {
        File(filePath).appendText(text)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

actual fun writeTextToFile(filePath: String, text: String) {
    try {
        File(filePath).writeText(text)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

actual fun deleteFile(filePath: String): Boolean {
    return try {
        File(filePath).delete()
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

actual fun readTextFromFile(filePath: String): String {
    return try {
        val file = File(filePath)
        if (file.exists()) file.readText() else ""
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}


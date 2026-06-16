package com.e9ab98.kmprecording.service

expect fun appendTextToFile(filePath: String, text: String)
expect fun writeTextToFile(filePath: String, text: String)
expect fun deleteFile(filePath: String): Boolean

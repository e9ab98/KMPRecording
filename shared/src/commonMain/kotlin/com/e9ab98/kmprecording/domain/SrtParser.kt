package com.e9ab98.kmprecording.domain

data class SrtCue(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val speedText: String,
    val locationText: String
)

object SrtParser {
    fun parse(content: String): List<SrtCue> {
        val cues = mutableListOf<SrtCue>()
        if (content.isEmpty()) return cues
        
        // Split by empty lines (supports both CRLF and LF, and multiple line breaks)
        val blocks = content.split(Regex("(?:\r?\n){2,}"))
        for (block in blocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size >= 3) {
                // Line 0: Index (e.g. "1")
                // Line 1: Time range (e.g. "00:00:00,000 --> 00:00:01,000")
                val timeLine = lines[1]
                if (timeLine.contains("-->")) {
                    val parts = timeLine.split("-->")
                    if (parts.size == 2) {
                        val startMs = parseSrtTime(parts[0])
                        val endMs = parseSrtTime(parts[1])
                        val speedText = lines.getOrNull(2) ?: ""
                        val locationText = lines.getOrNull(3) ?: ""
                        cues.add(SrtCue(startMs, endMs, speedText, locationText))
                    }
                }
            }
        }
        return cues
    }

    private fun parseSrtTime(timeStr: String): Long {
        return try {
            val cleanTime = timeStr.trim().replace(',', '.')
            val parts = cleanTime.split(":")
            val hours = parts[0].toLong()
            val minutes = parts[1].toLong()
            val secondsParts = parts[2].split(".")
            val seconds = secondsParts[0].toLong()
            val milliseconds = secondsParts.getOrNull(1)?.padEnd(3, '0')?.take(3)?.toLong() ?: 0L
            ((hours * 3600 + minutes * 60 + seconds) * 1000) + milliseconds
        } catch (e: Exception) {
            0L
        }
    }
}

package com.e9ab98.kmprecording.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SrtParserTest {
    @Test
    fun testParseValidSrt() {
        val srtContent = """
            1
            00:00:00,000 --> 00:00:01,000
            Speed: 35.4 km/h
            Lat: 39.904200° N, Lon: 116.407400° E
            
            2
            00:00:01,000 --> 00:00:02,000
            Speed: 36.8 km/h
            Lat: 39.904300° N, Lon: 116.407500° E
        """.trimIndent()
        
        val cues = SrtParser.parse(srtContent)
        assertEquals(2, cues.size)
        
        val first = cues[0]
        assertEquals(0L, first.startTimeMs)
        assertEquals(1000L, first.endTimeMs)
        assertEquals("Speed: 35.4 km/h", first.speedText)
        assertEquals("Lat: 39.904200° N, Lon: 116.407400° E", first.locationText)
        
        val second = cues[1]
        assertEquals(1000L, second.startTimeMs)
        assertEquals(2000L, second.endTimeMs)
        assertEquals("Speed: 36.8 km/h", second.speedText)
        assertEquals("Lat: 39.904300° N, Lon: 116.407500° E", second.locationText)
    }

    @Test
    fun testParseEmptyContent() {
        val cues = SrtParser.parse("")
        assertTrue(cues.isEmpty())
    }
}

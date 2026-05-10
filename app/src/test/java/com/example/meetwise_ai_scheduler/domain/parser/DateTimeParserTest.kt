package com.example.meetwise_ai_scheduler.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DateTimeParserTest {

    private val parser = DateTimeParser()

    @Test
    fun `parse tomorrow at 2pm returns correct date and time`() {
        val input = "Design review tomorrow at 2pm for 1 hour"
        val result = parser.parse(input)
        
        val tomorrow = LocalDate.now().plusDays(1)
        val twoPm = LocalTime.of(14, 0)
        
        assertNotNull(result)
        assertEquals(tomorrow, result?.date)
        assertEquals(twoPm, result?.time)
        assertEquals(60, result?.durationMinutes)
        assertEquals("Design review", result?.title)
    }

    @Test
    fun `parse edge cases 12pm and 12am correctly`() {
        // 12pm should be 12:00 (Noon)
        val noonResult = parser.parse("Meeting today at 12pm")
        assertEquals(12, noonResult?.time?.hour)
        
        // 12am should be 00:00 (Midnight)
        val midnightResult = parser.parse("Party tomorrow at 12am")
        assertEquals(0, midnightResult?.time?.hour)
    }

    @Test
    fun `parse duration in minutes correctly`() {
        val input = "Quick sync today at 3:30pm for 15 mins"
        val result = parser.parse(input)
        
        assertEquals(15, result?.durationMinutes)
        assertEquals(LocalTime.of(15, 30), result?.time)
    }

    @Test
    fun `return null on garbage input`() {
        val input = "Hello world what is happening"
        val result = parser.parse(input)
        
        assertNull(result)
    }
}

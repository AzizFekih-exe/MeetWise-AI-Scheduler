package com.example.meetwise_ai_scheduler.domain.engine

import com.example.meetwise_ai_scheduler.domain.model.Slot
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class SlotSuggestionEngineTest {

    private val engine = SlotSuggestionEngine()

    @Test
    fun `suggestSlots correctly splits availability by a middle conflict`() {
        /**
         * Scenario:
         * Availability: 2:00 PM to 4:00 PM (120 mins)
         * Conflict: 2:45 PM to 3:15 PM (30 mins) in the middle
         * Requested Duration: 30 mins
         * 
         * Result should be TWO slots:
         * 1. 2:00 PM - 2:45 PM (45 mins)
         * 2. 3:15 PM - 4:00 PM (45 mins)
         */
        
        val baseDate = LocalDateTime.of(2026, 5, 10, 14, 0)
        
        val availability = listOf(
            Slot(baseDate, baseDate.plusHours(2))
        )
        
        val conflicts = listOf(
            Slot(baseDate.plusMinutes(45), baseDate.plusMinutes(75))
        )
        
        val results = engine.suggestSlots(availability, conflicts, 30)
        
        // We expect 2 scored slots
        assertEquals(2, results.size)
        
        // First slot: 14:00 - 14:45
        assertEquals(baseDate, results[0].slot.startDateTime)
        assertEquals(baseDate.plusMinutes(45), results[0].slot.endDateTime)
        
        // Second slot: 15:15 - 16:00
        assertEquals(baseDate.plusMinutes(75), results[1].slot.startDateTime)
        assertEquals(baseDate.plusHours(2), results[1].slot.endDateTime)
    }

    @Test
    fun `suggestSlots excludes slots that are too short after splitting`() {
        /**
         * Scenario:
         * Availability: 2:00 PM to 3:00 PM (60 mins)
         * Conflict: 2:10 PM to 2:50 PM (40 mins)
         * Requested Duration: 30 mins
         * 
         * Remaining gaps are 10 mins each, so NO slots should be returned.
         */
        
        val baseDate = LocalDateTime.of(2026, 5, 10, 14, 0)
        
        val availability = listOf(
            Slot(baseDate, baseDate.plusHours(1))
        )
        
        val conflicts = listOf(
            Slot(baseDate.plusMinutes(10), baseDate.plusMinutes(50))
        )
        
        val results = engine.suggestSlots(availability, conflicts, 30)
        
        assertEquals(0, results.size)
    }
}

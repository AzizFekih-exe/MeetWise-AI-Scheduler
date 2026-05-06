package com.example.meetwise_ai_scheduler.domain.engine

import com.example.meetwise_ai_scheduler.domain.model.Slot
import com.example.meetwise_ai_scheduler.domain.model.ScoredSlot
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Concept: SlotSuggestionEngine (Task 9)
 * Pure Kotlin logic to calculate and score meeting slots.
 * 
 * Logic:
 * 1. Find all available gaps by subtracting conflicts from availability windows.
 * 2. Score each gap based on preferences, buffers, and history.
 * 3. Return the top 3 results.
 */
class SlotSuggestionEngine {

    fun suggestSlots(
        availableWindows: List<Slot>,
        conflicts: List<Slot>,
        durationMinutes: Int
    ): List<ScoredSlot> {
        // Phase 1: Intersect availability windows and exclude conflicts
        val possibleSlots = findPossibleSlots(availableWindows, conflicts, durationMinutes)
        
        // Phase 2: Apply the scoring formula: 0.4*pref + 0.35*buff + 0.25*hist
        return possibleSlots.map { slot ->
            calculateScore(slot)
        }.sortedByDescending { it.score }
         .take(3) 
    }

    /**
     * Logic: Subtracts conflicts from available windows and returns gaps 
     * that are long enough to fit the requested duration.
     */
    private fun findPossibleSlots(
        available: List<Slot>,
        conflicts: List<Slot>,
        durationMinutes: Int
    ): List<Slot> {
        var freeTime = available.toMutableList()

        for (conflict in conflicts) {
            val nextFreeTime = mutableListOf<Slot>()
            for (freeSlot in freeTime) {
                // Check for overlap
                val overlapStart = if (freeSlot.startDateTime > conflict.startDateTime) freeSlot.startDateTime else conflict.startDateTime
                val overlapEnd = if (freeSlot.endDateTime < conflict.endDateTime) freeSlot.endDateTime else conflict.endDateTime
                
                if (overlapStart >= overlapEnd) {
                    // No overlap: keep the slot as is
                    nextFreeTime.add(freeSlot)
                } else {
                    // Overlap exists: split the free slot
                    
                    // 1. Part before the conflict
                    if (freeSlot.startDateTime < conflict.startDateTime) {
                        nextFreeTime.add(Slot(freeSlot.startDateTime, conflict.startDateTime))
                    }
                    
                    // 2. Part after the conflict
                    if (freeSlot.endDateTime > conflict.endDateTime) {
                        nextFreeTime.add(Slot(conflict.endDateTime, freeSlot.endDateTime))
                    }
                }
            }
            freeTime = nextFreeTime
        }

        // Only return slots that meet the minimum duration requirement
        return freeTime.filter { slot ->
            Duration.between(slot.startDateTime, slot.endDateTime).toMinutes() >= durationMinutes
        }
    }

    private fun calculateScore(slot: Slot): ScoredSlot {
        // σ = 0.4·f_preferred + 0.35·f_buffer + 0.25·f_history
        val fPreferred = calculatePreferredFactor(slot)
        val fBuffer = calculateBufferFactor(slot)
        val fHistory = calculateHistoryFactor(slot)
        
        val totalScore = (0.4 * fPreferred) + (0.35 * fBuffer) + (0.25 * fHistory)
        
        return ScoredSlot(slot, totalScore, fPreferred, fBuffer, fHistory)
    }

    private fun calculatePreferredFactor(slot: Slot): Double {
        // Simple implementation: Prefer 9am to 5pm
        val startHour = slot.startDateTime.hour
        return if (startHour in 9..16) 1.0 else 0.5
    }

    private fun calculateBufferFactor(slot: Slot): Double {
        // Placeholder for proximity logic (Task 15)
        return 1.0 
    }

    private fun calculateHistoryFactor(slot: Slot): Double {
        // Placeholder for history ML scoring (Task 16)
        return 1.0
    }
}

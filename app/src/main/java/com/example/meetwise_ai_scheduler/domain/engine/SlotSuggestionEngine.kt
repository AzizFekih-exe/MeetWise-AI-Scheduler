package com.example.meetwise_ai_scheduler.domain.engine

import com.example.meetwise_ai_scheduler.domain.model.Meeting
import com.example.meetwise_ai_scheduler.domain.model.Slot
import com.example.meetwise_ai_scheduler.domain.model.ScoredSlot
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Concept: SlotSuggestionEngine (Task 15 & 16)
 * Pure Kotlin logic to calculate and score meeting slots.
 * 
 * Logic:
 * 1. Find all available gaps by subtracting conflicts from availability windows.
 * 2. Score each gap based on preferences, buffers, and history.
 * 3. σ = 0.4·f_preferred + 0.35·f_buffer + 0.25·f_history
 */
class SlotSuggestionEngine {

    fun suggestSlots(
        availableWindows: List<Slot>,
        conflicts: List<Slot>,
        existingMeetings: List<Meeting>,
        durationMinutes: Int
    ): List<ScoredSlot> {
        // Phase 1: Intersect availability windows and exclude conflicts
        val possibleSlots = findPossibleSlots(availableWindows, conflicts, durationMinutes)
        
        // Pre-calculate history: find the user's most frequent meeting start hour
        val mostFrequentHour = calculateMostFrequentHour(existingMeetings)
        
        // Phase 2: Apply the scoring formula
        return possibleSlots.map { slot ->
            calculateScore(slot, existingMeetings, mostFrequentHour)
        }.sortedByDescending { it.score }
    }

    private fun findPossibleSlots(
        available: List<Slot>,
        conflicts: List<Slot>,
        durationMinutes: Int
    ): List<Slot> {
        var freeTime = available.toMutableList()

        for (conflict in conflicts) {
            val nextFreeTime = mutableListOf<Slot>()
            for (freeSlot in freeTime) {
                val overlapStart = if (freeSlot.startDateTime > conflict.startDateTime) freeSlot.startDateTime else conflict.startDateTime
                val overlapEnd = if (freeSlot.endDateTime < conflict.endDateTime) freeSlot.endDateTime else conflict.endDateTime
                
                if (overlapStart >= overlapEnd) {
                    nextFreeTime.add(freeSlot)
                } else {
                    if (freeSlot.startDateTime < conflict.startDateTime) {
                        nextFreeTime.add(Slot(freeSlot.startDateTime, conflict.startDateTime))
                    }
                    if (freeSlot.endDateTime > conflict.endDateTime) {
                        nextFreeTime.add(Slot(conflict.endDateTime, freeSlot.endDateTime))
                    }
                }
            }
            freeTime = nextFreeTime
        }

        return freeTime.filter { slot ->
            Duration.between(slot.startDateTime, slot.endDateTime).toMinutes() >= durationMinutes
        }
    }

    private fun calculateScore(
        slot: Slot, 
        existingMeetings: List<Meeting>,
        mostFrequentHour: Int?
    ): ScoredSlot {
        val fPreferred = calculatePreferredFactor(slot)
        val fBuffer = calculateBufferFactor(slot, existingMeetings)
        val fHistory = calculateHistoryFactor(slot, mostFrequentHour)
        
        val totalScore = (0.4 * fPreferred) + (0.35 * fBuffer) + (0.25 * fHistory)
        
        return ScoredSlot(slot, totalScore, fPreferred, fBuffer, fHistory)
    }

    private fun calculatePreferredFactor(slot: Slot): Double {
        val startHour = slot.startDateTime.hour
        return if (startHour in 9..16) 1.0 else 0.5
    }

    private fun calculateBufferFactor(slot: Slot, existingMeetings: List<Meeting>): Double {
        // Task 15: Give 1.0 if there's at least a 30-min gap before and after
        val bufferThreshold = 30L
        
        for (meeting in existingMeetings) {
            val meetingStart = meeting.dateTime
            val meetingEnd = meeting.dateTime.plusMinutes(meeting.durationMinutes.toLong())
            
            // Check if meeting is too close before the slot
            if (meetingEnd > slot.startDateTime.minusMinutes(bufferThreshold) && meetingEnd <= slot.startDateTime) {
                return 0.5
            }
            // Check if meeting is too close after the slot
            if (meetingStart < slot.endDateTime.plusMinutes(bufferThreshold) && meetingStart >= slot.endDateTime) {
                return 0.5
            }
        }
        return 1.0
    }

    private fun calculateHistoryFactor(slot: Slot, mostFrequentHour: Int?): Double {
        // Task 16: Give 1.0 if the slot starts in the user's most frequent hour
        return if (mostFrequentHour != null && slot.startDateTime.hour == mostFrequentHour) {
            1.0
        } else {
            0.5
        }
    }

    private fun calculateMostFrequentHour(meetings: List<Meeting>): Int? {
        if (meetings.isEmpty()) return null
        return meetings.groupBy { it.dateTime.hour }
            .maxByOrNull { it.value.size }
            ?.key
    }
}

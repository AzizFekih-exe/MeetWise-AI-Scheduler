package com.example.meetwise_ai_scheduler.domain.engine

import com.example.meetwise_ai_scheduler.domain.model.Meeting
import com.example.meetwise_ai_scheduler.domain.model.Slot
import com.example.meetwise_ai_scheduler.domain.model.ScoredSlot
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

class SlotSuggestionEngine {

    fun suggestSlots(
        availableWindows: List<Slot>,
        conflicts: List<Slot>,
        existingMeetings: List<Meeting>,
        durationMinutes: Int
    ): List<ScoredSlot> {
        val possibleSlots = findPossibleSlots(availableWindows, conflicts, durationMinutes)
        
        // Pre-calculate history: find the user's most frequent meeting start hour
        val mostFrequentHour = calculateMostFrequentHour(existingMeetings)
        
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

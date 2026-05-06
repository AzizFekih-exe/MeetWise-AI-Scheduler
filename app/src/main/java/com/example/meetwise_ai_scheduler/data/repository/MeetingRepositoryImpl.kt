package com.example.meetwise_ai_scheduler.data.repository

import com.example.meetwise_ai_scheduler.data.network.MeetingApiService
import com.example.meetwise_ai_scheduler.data.network.model.toDomain
import com.example.meetwise_ai_scheduler.domain.engine.SlotSuggestionEngine
import com.example.meetwise_ai_scheduler.domain.model.Meeting
import com.example.meetwise_ai_scheduler.domain.model.MeetingIntent
import com.example.meetwise_ai_scheduler.domain.model.ScoredSlot
import com.example.meetwise_ai_scheduler.domain.model.Slot
import com.example.meetwise_ai_scheduler.domain.repository.MeetingRepository
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetingRepositoryImpl @Inject constructor(
    private val apiService: MeetingApiService,
    private val engine: SlotSuggestionEngine
) : MeetingRepository {

    override suspend fun getMeetings(): Result<List<Meeting>> {
        return try {
            val response = apiService.getMeetings()
            Result.success(response.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createMeeting(meeting: Meeting): Result<Unit> {
        return Result.success(Unit) 
    }

    override suspend fun getSuggestedSlots(intent: MeetingIntent): Result<List<ScoredSlot>> {
        return try {
            // 1. Fetch current meetings to calculate buffer and history factors
            val existingMeetings = apiService.getMeetings().map { it.toDomain() }
            
            // 2. Fetch raw suggestions (windows) from backend
            val rawDtos = apiService.getSuggestedSlots("TEMP_ID")
            
            // 3. Use our client-side engine to apply the full scoring formula
            val scoredSlots = engine.suggestSlots(
                availableWindows = rawDtos.map { Slot(LocalDateTime.parse(it.startTime), LocalDateTime.parse(it.endTime)) },
                conflicts = emptyList(), // Conflicts are already handled by backend windows
                existingMeetings = existingMeetings,
                durationMinutes = intent.durationMinutes
            )
            
            Result.success(scoredSlots)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun confirmMeeting(meeting: Meeting, selectedSlot: ScoredSlot): Result<Unit> {
        return try {
            // Confirm the slot with the backend
            apiService.confirmMeeting(
                meetingId = meeting.meetingId,
                slotId = selectedSlot.slot.startDateTime.toString() // Or use a real ID from SlotDto
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMeeting(meetingId: String): Result<Unit> {
        return try {
            apiService.deleteMeeting(meetingId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

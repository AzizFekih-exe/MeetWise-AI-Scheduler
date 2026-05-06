package com.example.meetwise_ai_scheduler.data.repository

import com.example.meetwise_ai_scheduler.data.network.MeetingApiService
import com.example.meetwise_ai_scheduler.data.network.model.toDomain
import com.example.meetwise_ai_scheduler.domain.model.Meeting
import com.example.meetwise_ai_scheduler.domain.model.MeetingIntent
import com.example.meetwise_ai_scheduler.domain.model.ScoredSlot
import com.example.meetwise_ai_scheduler.domain.repository.MeetingRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concept: MeetingRepository Implementation
 * This class coordinates between the Network (FastAPI) and the Domain logic.
 * It uses the Result<T> wrapper to ensure safe error propagation.
 */
@Singleton
class MeetingRepositoryImpl @Inject constructor(
    private val apiService: MeetingApiService
) : MeetingRepository {

    override suspend fun getMeetings(): Result<List<Meeting>> {
        return try {
            val response = apiService.getMeetings()
            // Map the list of DTOs to Domain models
            Result.success(response.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createMeeting(meeting: Meeting): Result<Unit> {
        // Implementation for creating a meeting via backend
        return Result.success(Unit) // Placeholder
    }

    override suspend fun getSuggestedSlots(intent: MeetingIntent): Result<List<ScoredSlot>> {
        return try {
            // In a real scenario, we'd send the intent to the backend.
            // For now, we fetch the suggested slots for a meeting ID (or simulate it)
            val meetingId = "TEMP_ID" 
            val response = apiService.getSuggestedSlots(meetingId)
            
            Result.success(response.map { it.toDomain() })
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
}

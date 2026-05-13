package com.example.meetwise_ai_scheduler.domain.repository

import com.example.meetwise_ai_scheduler.domain.model.Meeting
import com.example.meetwise_ai_scheduler.domain.model.MeetingIntent
import com.example.meetwise_ai_scheduler.domain.model.Minutes
import com.example.meetwise_ai_scheduler.domain.model.ScoredSlot
import com.example.meetwise_ai_scheduler.domain.model.TranscriptionJobStatus
import java.io.File

/**
 * Concept: MeetingRepository
 * This interface defines the contract for all meeting-related operations.
 * It will be implemented in the Data layer using Retrofit (FastAPI) and Room.
 */
interface MeetingRepository {
    
    // Fetch all meetings for the current user
    suspend fun getMeetings(): Result<List<Meeting>>
    
    // Create a new meeting (dispatch to backend/Google Calendar)
    suspend fun createMeeting(meeting: Meeting): Result<Unit>
    
    // Get the top 3 suggested slots based on the NLP-extracted intent
    suspend fun getSuggestedSlots(intent: MeetingIntent): Result<List<ScoredSlot>>
    
    // Confirm a specific slot and finalize the meeting
    suspend fun confirmMeeting(meeting: Meeting, selectedSlot: ScoredSlot): Result<Unit>

    // Delete a meeting
    suspend fun deleteMeeting(meetingId: String): Result<Unit>

    suspend fun uploadRecording(meetingId: String, audioFile: File): Result<String>

    suspend fun getTranscriptionStatus(jobId: String): Result<TranscriptionJobStatus>

    suspend fun getMinutes(meetingId: String): Result<Minutes>

    suspend fun getRecordedMinutes(): Result<List<Minutes>>
}

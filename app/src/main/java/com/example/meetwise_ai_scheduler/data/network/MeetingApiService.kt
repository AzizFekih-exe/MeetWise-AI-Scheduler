package com.example.meetwise_ai_scheduler.data.network

import com.example.meetwise_ai_scheduler.data.network.model.MeetingDto
import com.example.meetwise_ai_scheduler.data.network.model.SlotDto
import com.example.meetwise_ai_scheduler.data.network.model.TranscriptionResponse
import okhttp3.MultipartBody
import retrofit2.http.*

/**
 * Concept: MeetingApiService
 * Retrofit interface for meeting scheduling and transcription endpoints.
 */
interface MeetingApiService {

    @GET("api/v1/meetings")
    suspend fun getMeetings(): List<MeetingDto>

    @Multipart
    @POST("api/v1/meetings/transcribe")
    suspend fun transcribeAudio(@Part audio: MultipartBody.Part): TranscriptionResponse

    // Fetch suggested slots for a pending meeting
    @GET("api/v1/meetings/{id}/slots")
    suspend fun getSuggestedSlots(@Path("id") meetingId: String): List<SlotDto>

    // Confirm a specific slot for a meeting
    @POST("api/v1/meetings/{id}/confirm")
    suspend fun confirmMeeting(
        @Path("id") meetingId: String,
        @Body slotId: String // Or a confirmation object depending on backend
    )
}

package com.example.meetwise_ai_scheduler.data.network

import com.example.meetwise_ai_scheduler.data.network.model.MeetingDto
import com.example.meetwise_ai_scheduler.data.network.model.CreateMeetingRequest
import com.example.meetwise_ai_scheduler.data.network.model.SlotDto
import com.example.meetwise_ai_scheduler.data.network.model.JobStatusResponse
import com.example.meetwise_ai_scheduler.data.network.model.TranscriptionJobResponse
import com.example.meetwise_ai_scheduler.data.network.model.MinutesDto
import okhttp3.MultipartBody
import retrofit2.http.*

/**
 * Concept: MeetingApiService
 * Retrofit interface for meeting scheduling and transcription endpoints.
 */
interface MeetingApiService {

    @GET("api/v1/meetings/")
    suspend fun getMeetings(): List<MeetingDto>

    @POST("api/v1/meetings/")
    suspend fun createMeeting(@Body request: CreateMeetingRequest): MeetingDto

    @Multipart
    @POST("api/v1/meetings/{id}/transcribe")
    suspend fun transcribeAudio(
        @Path("id") meetingId: String,
        @Part audioFile: MultipartBody.Part
    ): TranscriptionJobResponse

    @GET("api/v1/jobs/{jobId}")
    suspend fun getJobStatus(@Path("jobId") jobId: String): JobStatusResponse

    @GET("api/v1/meetings/{id}/minutes")
    suspend fun getMinutes(@Path("id") meetingId: String): MinutesDto

    // Fetch suggested slots for a pending meeting
    @GET("api/v1/meetings/{id}/slots")
    suspend fun getSuggestedSlots(@Path("id") meetingId: String): List<SlotDto>

    // Confirm a specific slot for a meeting
    @POST("api/v1/meetings/{id}/confirm")
    suspend fun confirmMeeting(
        @Path("id") meetingId: String,
        @Body slotId: String 
    )

    @DELETE("api/v1/meetings/{id}")
    suspend fun deleteMeeting(@Path("id") meetingId: String)
}

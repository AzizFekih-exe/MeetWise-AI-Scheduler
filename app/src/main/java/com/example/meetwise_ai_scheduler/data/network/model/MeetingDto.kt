package com.example.meetwise_ai_scheduler.data.network.model

import com.example.meetwise_ai_scheduler.domain.model.Meeting
import com.example.meetwise_ai_scheduler.domain.model.ScoredSlot
import com.example.meetwise_ai_scheduler.domain.model.Slot
import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class MeetingDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("start_time") val startTime: String, // ISO 8601 string
    @SerializedName("duration_minutes") val durationMinutes: Int,
    @SerializedName("status") val status: String
)

fun MeetingDto.toDomain(): Meeting {
    return Meeting(
        meetingId = id,
        title = title,
        dateTime = LocalDateTime.parse(startTime, DateTimeFormatter.ISO_DATE_TIME),
        durationMinutes = durationMinutes,
        location = null,
        status = status,
        createdBy = "" // Will be filled by repository
    )
}

data class SlotDto(
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("score") val score: Double
)

fun SlotDto.toDomain(): ScoredSlot {
    val start = LocalDateTime.parse(startTime, DateTimeFormatter.ISO_DATE_TIME)
    val end = LocalDateTime.parse(endTime, DateTimeFormatter.ISO_DATE_TIME)
    return ScoredSlot(
        slot = Slot(start, end),
        score = score,
        preferredFactor = 1.0, 
        bufferFactor = 1.0,
        historyFactor = 1.0
    )
}

data class TranscriptionResponse(
    @SerializedName("text") val text: String,
    @SerializedName("intent") val intent: MeetingIntentDto?
)

data class MeetingIntentDto(
    @SerializedName("title") val title: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("time") val time: String?,
    @SerializedName("duration") val duration: Int?
)

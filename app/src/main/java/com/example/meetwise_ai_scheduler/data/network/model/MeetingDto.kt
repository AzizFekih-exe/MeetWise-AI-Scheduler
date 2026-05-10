package com.example.meetwise_ai_scheduler.data.network.model

import com.example.meetwise_ai_scheduler.domain.model.Meeting
import com.example.meetwise_ai_scheduler.domain.model.ActionItem
import com.example.meetwise_ai_scheduler.domain.model.Minutes
import com.example.meetwise_ai_scheduler.domain.model.ScoredSlot
import com.example.meetwise_ai_scheduler.domain.model.Slot
import com.example.meetwise_ai_scheduler.domain.model.TranscriptionJobStatus
import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class MeetingDto(
    @SerializedName("meetingId") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("dateTime") val startTime: String,
    @SerializedName("duration") val durationMinutes: Int,
    @SerializedName("location") val location: String?,
    @SerializedName("status") val status: String
)

data class CreateMeetingRequest(
    @SerializedName("title") val title: String,
    @SerializedName("dateTime") val dateTime: String,
    @SerializedName("duration") val duration: Int,
    @SerializedName("location") val location: String? = null,
    @SerializedName("status") val status: String = "scheduled",
    @SerializedName("participants") val participants: List<Int> = emptyList(),
    @SerializedName("participantEmails") val participantEmails: List<String> = emptyList()
)

fun MeetingDto.toDomain(): Meeting {
    return Meeting(
        meetingId = id,
        title = title,
        dateTime = LocalDateTime.parse(startTime, DateTimeFormatter.ISO_DATE_TIME),
        durationMinutes = durationMinutes,
        location = location,
        status = status,
        createdBy = "" // Will be filled by repository
    )
}

data class SlotDto(
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
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

data class TranscriptionJobResponse(
    @SerializedName("jobId") val jobId: String,
    @SerializedName("status") val status: String
)

data class JobStatusResponse(
    @SerializedName("jobId") val jobId: String,
    @SerializedName("status") val status: String,
    @SerializedName("progress") val progress: Float,
    @SerializedName("message") val message: String,
    @SerializedName("errorMessage") val errorMessage: String?
)

fun JobStatusResponse.toDomain(): TranscriptionJobStatus {
    return TranscriptionJobStatus(
        jobId = jobId,
        status = status,
        progress = progress,
        message = message,
        errorMessage = errorMessage
    )
}

data class MinutesDto(
    @SerializedName("minutesId") val minutesId: Int,
    @SerializedName("meetingId") val meetingId: Int,
    @SerializedName("summaryText") val summaryText: String,
    @SerializedName("actionItems") val actionItems: List<ActionItemDto>,
    @SerializedName("generatedAt") val generatedAt: String,
    @SerializedName("rawNotes") val rawNotes: String?
)

data class ActionItemDto(
    @SerializedName("task") val task: String,
    @SerializedName("owner") val owner: String,
    @SerializedName("deadline") val deadline: String?,
    @SerializedName("done") val done: Boolean
)

fun MinutesDto.toDomain(): Minutes {
    return Minutes(
        minutesId = minutesId,
        meetingId = meetingId,
        summary = summaryText,
        actionItems = actionItems.map {
            ActionItem(
                task = it.task,
                owner = it.owner,
                deadline = it.deadline,
                done = it.done
            )
        },
        generatedAt = generatedAt,
        rawNotes = rawNotes
    )
}

data class MeetingIntentDto(
    @SerializedName("title") val title: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("time") val time: String?,
    @SerializedName("duration") val duration: Int?
)

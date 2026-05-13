package com.example.meetwise_ai_scheduler.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class MeetingIntent(
    val title: String?,
    val date: LocalDate?,
    val endDate: LocalDate? = null,
    val time: LocalTime?,
    val timeWindow: ClosedRange<LocalTime>? = null,
    val durationMinutes: Int?,
    val attendees: List<String> = emptyList(),
    val rawInput: String
)

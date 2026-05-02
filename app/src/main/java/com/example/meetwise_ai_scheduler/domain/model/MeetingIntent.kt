package com.example.meetwise_ai_scheduler.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Concept: MeetingIntent
 * This represents the structured extraction from a natural language request.
 * e.g., "Design review with Alice tomorrow at 2pm for 1 hour"
 */
data class MeetingIntent(
    val title: String?,
    val date: LocalDate?,
    val time: LocalTime?,
    val durationMinutes: Int?,
    val attendees: List<String> = emptyList(),
    val rawInput: String
)

package com.example.meetwise_ai_scheduler.domain.model

import java.time.LocalDateTime

/**
 * Concept: Meeting Domain Model
 * This represents a scheduled meeting. Notice the use of java.time.LocalDateTime,
 * which is available since our minSdk is 26.
 */
data class Meeting(
    val meetingId: String,
    val title: String,
    val dateTime: LocalDateTime,
    val durationMinutes: Int,
    val location: String?,
    val status: String, // 'scheduled', 'confirmed', 'completed', 'cancelled'
    val createdBy: String,
    val participants: List<Participant> = emptyList(),
    val participantEmails: List<String> = emptyList()
)

data class Participant(
    val userId: String,
    val name: String,
    val email: String,
    val role: String, // 'organizer', 'attendee'
    val status: String // 'pending', 'accepted', 'declined'
)

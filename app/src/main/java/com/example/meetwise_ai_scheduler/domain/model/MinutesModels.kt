package com.example.meetwise_ai_scheduler.domain.model

data class ActionItem(
    val task: String,
    val owner: String,
    val deadline: String? = null,
    val done: Boolean = false
)

data class Minutes(
    val minutesId: Int,
    val meetingId: Int,
    val summary: String,
    val actionItems: List<ActionItem>,
    val generatedAt: String,
    val rawNotes: String? = null
)

data class AvailabilityWindow(
    val availId: Int = 0,
    val dayOfWeek: Int, // 0-6 (Mon-Sun)
    val startHour: Int, // 0-23
    val endHour: Int,   // 0-23
    val isRecurring: Boolean = true
)

package com.example.meetwise_ai_scheduler.domain.model

import java.time.LocalDateTime

data class Slot(
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime
)

data class ScoredSlot(
    val slot: Slot,
    val score: Double,
    val preferredFactor: Double, // f_preferred
    val bufferFactor: Double,    // f_buffer
    val historyFactor: Double     // f_history
)

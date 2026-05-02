package com.example.meetwise_ai_scheduler.domain.model

import java.time.LocalDateTime

/**
 * Concept: Slot and ScoredSlot
 * A Slot is a simple time window. 
 * A ScoredSlot includes the score σ calculated by our suggestion engine:
 * σ = 0.4·f_preferred + 0.35·f_buffer + 0.25·f_history
 */
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

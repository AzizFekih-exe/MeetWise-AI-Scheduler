package com.example.meetwise_ai_scheduler.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "minutes")
data class MinutesEntity(
    @PrimaryKey val minutesId: Int,
    val meetingId: Int,
    val summary: String,
    val actionItemsJson: String,
    val generatedAt: String,
    val rawNotes: String? = null
)

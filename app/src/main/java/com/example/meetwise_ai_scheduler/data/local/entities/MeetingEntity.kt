package com.example.meetwise_ai_scheduler.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meetings")
data class MeetingEntity(
    @PrimaryKey val meetingId: Int,
    val title: String,
    val dateTime: String,
    val createdBy: Int
)

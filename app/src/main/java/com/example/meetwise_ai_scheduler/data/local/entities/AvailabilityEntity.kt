package com.example.meetwise_ai_scheduler.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "availability")
data class AvailabilityEntity(
    @PrimaryKey(autoGenerate = true) val availId: Int = 0,
    val userId: Int,
    val dayOfWeek: Int, // 0-6
    val startHour: Int,
    val endHour: Int,
    val isRecurring: Boolean = true
)

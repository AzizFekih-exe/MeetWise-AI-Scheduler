package com.example.meetwise_ai_scheduler.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "availability",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AvailabilityEntity(
    @PrimaryKey(autoGenerate = true) val availId: Int = 0,
    val userId: Int,
    val dayOfWeek: Int, // 0-6
    val startHour: Int,
    val endHour: Int,
    val isRecurring: Boolean = true
)

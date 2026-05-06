package com.example.meetwise_ai_scheduler.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: Int,
    val name: String,
    val email: String
)

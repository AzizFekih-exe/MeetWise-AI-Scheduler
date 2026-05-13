package com.example.meetwise_ai_scheduler.domain.repository

import com.example.meetwise_ai_scheduler.domain.model.User

interface UserRepository {
    
    // Fetch a user's profile details
    suspend fun getUserProfile(userId: String): Result<User>
    
    // Update the current user's profile (name, timezone, etc.)
    suspend fun updateUserProfile(user: User): Result<Unit>
    
    // Placeholder for fetching user's availability/busy slots from calendar
    suspend fun getUserAvailability(userId: String): Result<List<String>>
}

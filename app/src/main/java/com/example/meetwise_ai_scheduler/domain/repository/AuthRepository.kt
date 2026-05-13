package com.example.meetwise_ai_scheduler.domain.repository

import com.example.meetwise_ai_scheduler.domain.model.User

interface AuthRepository {
    
    // For email/password registration
    suspend fun registerWithEmail(email: String, password: String, name: String): Result<User>
    
    // For email/password login
    suspend fun loginWithEmail(email: String, password: String): Result<User>
    
    // For Google Sign-In (takes the ID token provided by Google Play Services)
    suspend fun loginWithGoogle(idToken: String): Result<User>
    
    // Clear local session & Firebase Auth
    suspend fun logout(): Result<Unit>
    
    // Check if the user is currently authenticated
    fun isUserLoggedIn(): Boolean
    
    // Get the current cached user
    fun getCurrentUser(): User?
}

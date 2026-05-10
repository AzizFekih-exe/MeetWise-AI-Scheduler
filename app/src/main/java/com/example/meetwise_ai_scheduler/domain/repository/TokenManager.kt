package com.example.meetwise_ai_scheduler.domain.repository

/**
 * Concept: Token Management Interface
 * Again, this lives in the Domain layer so that the Domain and Presentation layers
 * can easily check if a token exists without knowing HOW it's stored.
 */
interface TokenManager {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
}

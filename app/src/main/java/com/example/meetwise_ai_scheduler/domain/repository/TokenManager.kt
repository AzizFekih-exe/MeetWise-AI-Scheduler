package com.example.meetwise_ai_scheduler.domain.repository

interface TokenManager {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
}

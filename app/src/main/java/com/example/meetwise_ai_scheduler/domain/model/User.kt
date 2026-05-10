package com.example.meetwise_ai_scheduler.domain.model

/**
 * Concept: Domain Model
 * This is a pure Kotlin data class. Notice there are NO Android or framework imports here.
 * It represents the core business concept of a User in our application, 
 * completely independent of how it's stored in Room or fetched from FastAPI.
 */
data class User(
    val userId: String,
    val name: String,
    val email: String,
    val timezone: String
)

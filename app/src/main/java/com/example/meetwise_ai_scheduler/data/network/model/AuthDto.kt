package com.example.meetwise_ai_scheduler.data.network.model

import com.google.gson.annotations.SerializedName

/**
 * Concept: Data Transfer Objects (DTOs)
 * These represent the raw JSON structure from the FastAPI backend.
 * We use @SerializedName to map JSON snake_case to Kotlin camelCase.
 */

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String
)

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("name") val name: String,
    @SerializedName("password") val password: String,
    @SerializedName("timezone") val timezone: String = "UTC"
)

data class UserDto(
    @SerializedName("userId") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("timezone") val timezone: String
)

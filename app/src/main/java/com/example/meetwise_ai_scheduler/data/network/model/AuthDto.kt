package com.example.meetwise_ai_scheduler.data.network.model

import com.google.gson.annotations.SerializedName


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

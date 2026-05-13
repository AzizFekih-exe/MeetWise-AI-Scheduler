package com.example.meetwise_ai_scheduler.data.network

import com.example.meetwise_ai_scheduler.data.network.model.LoginRequest
import com.example.meetwise_ai_scheduler.data.network.model.LoginResponse
import com.example.meetwise_ai_scheduler.data.network.model.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest)

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}

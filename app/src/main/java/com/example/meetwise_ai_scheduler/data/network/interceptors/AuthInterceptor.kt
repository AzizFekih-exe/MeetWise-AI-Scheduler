package com.example.meetwise_ai_scheduler.data.network.interceptors

import com.example.meetwise_ai_scheduler.domain.repository.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Fetch the actual token from EncryptedSharedPreferences
        val token = tokenManager.getToken()

        val requestBuilder = originalRequest.newBuilder()
        
        // If we have a token, attach it using the "Bearer" schema
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val newRequest = requestBuilder.build()
        return chain.proceed(newRequest)
    }
}

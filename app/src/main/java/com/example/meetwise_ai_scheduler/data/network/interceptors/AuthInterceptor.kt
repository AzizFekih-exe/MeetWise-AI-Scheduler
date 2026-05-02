package com.example.meetwise_ai_scheduler.data.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Concept:
 * Interceptors sit between the app and the network.
 * They allow us to inspect or modify outgoing requests and incoming responses.
 * 
 * Here, we intercept every outgoing HTTP request to the FastAPI backend
 * and automatically attach the JWT token to the headers.
 */
class AuthInterceptor @Inject constructor(
    // We will later inject a TokenManager (Task 4) here.
    // For now, we simulate it.
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Simulating fetching a token. In Task 4, this comes from EncryptedSharedPreferences.
        val token = "YOUR_JWT_TOKEN_PLACEHOLDER"

        val requestBuilder = originalRequest.newBuilder()
        
        // If we have a token, attach it using the "Bearer" schema
        if (token.isNotEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val newRequest = requestBuilder.build()
        
        // Proceed to execute the network call with the modified request
        val response = chain.proceed(newRequest)
        
        // Concept preview for Task 3: 
        // If response.code == 401 (Unauthorized), the token expired.
        // We will detect that here and trigger a re-route to the Login screen.
        
        return response
    }
}

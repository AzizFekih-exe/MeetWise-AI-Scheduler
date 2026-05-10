package com.example.meetwise_ai_scheduler.data.repository

import com.example.meetwise_ai_scheduler.data.network.AuthApiService
import com.example.meetwise_ai_scheduler.data.network.model.LoginRequest
import com.example.meetwise_ai_scheduler.data.network.model.RegisterRequest
import com.example.meetwise_ai_scheduler.domain.model.User
import com.example.meetwise_ai_scheduler.domain.repository.AuthRepository
import com.example.meetwise_ai_scheduler.domain.repository.TokenManager
import javax.inject.Inject

/**
 * Concept: Repository Implementation
 * This class implements the domain's AuthRepository interface.
 * Here we use the real FirebaseAuth SDK and our TokenManager.
 * 
 * Notice that we convert Firebase Tasks to Coroutines using `.await()`,
 * and we wrap everything in `Result.success` or `Result.failure`.
 */
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun registerWithEmail(email: String, password: String, name: String): Result<User> {
        return try {
            authApiService.register(RegisterRequest(email = email, name = name, password = password))
            loginWithEmail(email, password).getOrThrow()
            Result.success(User(userId = "", name = name, email = email, timezone = "UTC"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginWithEmail(email: String, password: String): Result<User> {
        return try {
            val response = authApiService.login(LoginRequest(email = email, password = password))
            tokenManager.saveToken(response.accessToken)
            Result.success(
                User(
                    userId = "",
                    name = email.substringBefore("@").ifBlank { "User" },
                    email = email,
                    timezone = "UTC"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginWithGoogle(idToken: String): Result<User> {
        // We will implement Google Sign-In in the next step, as it requires a bit more setup.
        TODO("Google Sign-In will be implemented next")
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            tokenManager.clearToken()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isUserLoggedIn(): Boolean {
        return false
    }

    override fun getCurrentUser(): User? {
        return null
    }
}

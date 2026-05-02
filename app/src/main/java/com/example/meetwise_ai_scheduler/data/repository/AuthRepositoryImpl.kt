package com.example.meetwise_ai_scheduler.data.repository

import com.example.meetwise_ai_scheduler.domain.model.User
import com.example.meetwise_ai_scheduler.domain.repository.AuthRepository
import com.example.meetwise_ai_scheduler.domain.repository.TokenManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
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
    private val firebaseAuth: FirebaseAuth,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun registerWithEmail(email: String, password: String, name: String): Result<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("User creation failed")
            
            // Note: We might want to send the display name to Firebase or backend here.
            // For now, we return our domain User model.
            Result.success(
                User(
                    userId = firebaseUser.uid,
                    name = name,
                    email = firebaseUser.email ?: email,
                    timezone = "UTC" // We will get the real timezone later
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginWithEmail(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Login failed")
            
            // Get the JWT ID token from Firebase
            val tokenResult = firebaseUser.getIdToken(true).await()
            val token = tokenResult.token ?: throw Exception("Failed to get token")
            
            // Store it securely (Task 4)
            tokenManager.saveToken(token)
            
            Result.success(
                User(
                    userId = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "User",
                    email = firebaseUser.email ?: email,
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
            firebaseAuth.signOut()
            tokenManager.clearToken()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isUserLoggedIn(): Boolean {
        // We consider the user logged in if Firebase has a user AND we have a token
        return firebaseAuth.currentUser != null && tokenManager.getToken() != null
    }

    override fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser ?: return null
        return User(
            userId = firebaseUser.uid,
            name = firebaseUser.displayName ?: "User",
            email = firebaseUser.email ?: "",
            timezone = "UTC"
        )
    }
}

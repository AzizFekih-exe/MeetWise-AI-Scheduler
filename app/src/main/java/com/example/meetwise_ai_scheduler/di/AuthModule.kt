package com.example.meetwise_ai_scheduler.di

import com.example.meetwise_ai_scheduler.data.local.EncryptedTokenManager
import com.example.meetwise_ai_scheduler.data.repository.AuthRepositoryImpl
import com.example.meetwise_ai_scheduler.domain.repository.AuthRepository
import com.example.meetwise_ai_scheduler.domain.repository.TokenManager
import com.google.firebase.auth.FirebaseAuth
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    /**
     * Concept: Binds vs Provides
     * When we have an interface (TokenManager) and an implementation (EncryptedTokenManager)
     * that we already know how to inject, we use @Binds. It's more efficient than @Provides.
     */
    @Binds
    @Singleton
    abstract fun bindTokenManager(
        encryptedTokenManager: EncryptedTokenManager
    ): TokenManager

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
}

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
}

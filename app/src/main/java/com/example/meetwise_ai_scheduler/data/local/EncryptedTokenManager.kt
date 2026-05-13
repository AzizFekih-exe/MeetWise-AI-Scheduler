package com.example.meetwise_ai_scheduler.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.meetwise_ai_scheduler.domain.repository.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedTokenManager @Inject constructor(
    @ApplicationContext context: Context
) : TokenManager {

    companion object {
        private const val PREFS_NAME = "meetwise_secure_prefs"
        private const val KEY_TOKEN = "jwt_token"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
    }

    override fun getToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    override fun clearToken() {
        sharedPreferences.edit().remove(KEY_TOKEN).apply()
    }
}

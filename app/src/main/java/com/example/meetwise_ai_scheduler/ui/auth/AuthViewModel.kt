package com.example.meetwise_ai_scheduler.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meetwise_ai_scheduler.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(isLoggedIn = authRepository.isUserLoggedIn()))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            authRepository.loginWithEmail(email.trim(), password).fold(
                onSuccess = { _uiState.value = AuthUiState(isLoggedIn = true) },
                onFailure = { error ->
                    _uiState.value = AuthUiState(errorMessage = error.message ?: "Login failed")
                }
            )
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            authRepository.registerWithEmail(email.trim(), password, name.trim()).fold(
                onSuccess = { _uiState.value = AuthUiState(isLoggedIn = true) },
                onFailure = { error ->
                    _uiState.value = AuthUiState(errorMessage = error.message ?: "Registration failed")
                }
            )
        }
    }
}

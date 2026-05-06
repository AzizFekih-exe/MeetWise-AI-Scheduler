package com.example.meetwise_ai_scheduler.ui.screens.minutes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meetwise_ai_scheduler.domain.model.Minutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MinutesUiState {
    object Idle : MinutesUiState()
    object Loading : MinutesUiState()
    data class Transcribing(val jobId: String, val progressStep: Int) : MinutesUiState()
    data class MinutesReady(val minutes: Minutes) : MinutesUiState()
    data class Error(val message: String) : MinutesUiState()
}

class MinutesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<MinutesUiState>(MinutesUiState.Idle)
    val uiState: StateFlow<MinutesUiState> = _uiState.asStateFlow()

    fun startTranscription(jobId: String) {
        _uiState.value = MinutesUiState.Transcribing(jobId, 1)
        startPolling(jobId)
    }

    private fun startPolling(jobId: String) {
        viewModelScope.launch {
            var isProcessing = true
            while (isProcessing) {
                // TODO: Call Repository.getJobStatus(jobId)
                // For demonstration, we'll simulate progress
                delay(3000)
                
                val currentStatus = simulateJobStatus(jobId)
                
                when (currentStatus) {
                    "done" -> {
                        isProcessing = false
                        fetchMinutes(101) // Simulate meeting ID
                    }
                    "failed" -> {
                        isProcessing = false
                        _uiState.value = MinutesUiState.Error("Transcription failed on server.")
                    }
                    else -> {
                        // Update progress step visually
                        val current = _uiState.value as? MinutesUiState.Transcribing
                        if (current != null) {
                            val nextStep = if (current.progressStep < 3) current.progressStep + 1 else 3
                            _uiState.value = MinutesUiState.Transcribing(jobId, nextStep)
                        }
                    }
                }
            }
        }
    }

    private fun fetchMinutes(meetingId: Int) {
        viewModelScope.launch {
            _uiState.value = MinutesUiState.Loading
            // TODO: Call Repository.getMinutes(meetingId)
            delay(1000)
            // Mock result
            val mockMinutes = Minutes(1, meetingId, "The team discussed Phase 3.", emptyList(), "2026-05-06")
            _uiState.value = MinutesUiState.MinutesReady(mockMinutes)
        }
    }

    private fun simulateJobStatus(jobId: String): String {
        // Logic to simulate server response cycles
        return "processing" // This would be the real API call
    }
}

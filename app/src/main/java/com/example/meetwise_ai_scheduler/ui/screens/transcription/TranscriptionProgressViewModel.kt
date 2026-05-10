package com.example.meetwise_ai_scheduler.ui.screens.transcription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meetwise_ai_scheduler.domain.model.Minutes
import com.example.meetwise_ai_scheduler.domain.repository.MeetingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TranscriptionProgressUiState(
    val status: String = "pending",
    val progress: Float = 0.30f,
    val message: String = "Audio uploaded. Waiting to transcribe.",
    val minutes: Minutes? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class TranscriptionProgressViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranscriptionProgressUiState())
    val uiState: StateFlow<TranscriptionProgressUiState> = _uiState.asStateFlow()

    fun pollJob(meetingId: String, jobId: String) {
        viewModelScope.launch {
            repeat(60) {
                meetingRepository.getTranscriptionStatus(jobId).fold(
                    onSuccess = { jobStatus ->
                        _uiState.value = TranscriptionProgressUiState(
                            status = jobStatus.status,
                            progress = jobStatus.progress,
                            message = jobStatus.message,
                            errorMessage = jobStatus.errorMessage
                        )
                        if (jobStatus.status == "done") {
                            fetchMinutes(meetingId)
                            return@launch
                        }
                        if (jobStatus.status == "failed") {
                            return@launch
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = TranscriptionProgressUiState(
                        status = "failed",
                        progress = _uiState.value.progress,
                        message = "Failed to check transcription status",
                        errorMessage = error.message ?: "Failed to check transcription status"
                    )
                        return@launch
                    }
                )
                delay(2_000)
            }
        }
    }

    private suspend fun fetchMinutes(meetingId: String) {
        repeat(5) {
            meetingRepository.getMinutes(meetingId).fold(
                onSuccess = { minutes ->
                    _uiState.value = TranscriptionProgressUiState(
                        status = "done",
                        progress = 1f,
                        message = "Minutes ready",
                        minutes = minutes
                    )
                    return
                },
                onFailure = { error ->
                    _uiState.value = TranscriptionProgressUiState(
                        status = "done",
                        progress = 1f,
                        message = "Minutes generated, but could not load them",
                        errorMessage = error.message ?: "Minutes are not ready yet"
                    )
                }
            )
            delay(1_000)
        }
    }
}

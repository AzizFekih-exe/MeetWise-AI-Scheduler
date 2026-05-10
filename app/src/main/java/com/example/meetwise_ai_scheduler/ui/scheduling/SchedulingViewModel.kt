package com.example.meetwise_ai_scheduler.ui.scheduling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meetwise_ai_scheduler.domain.model.Meeting
import com.example.meetwise_ai_scheduler.domain.model.MeetingIntent
import com.example.meetwise_ai_scheduler.domain.model.ScoredSlot
import com.example.meetwise_ai_scheduler.domain.parser.DateTimeParser
import com.example.meetwise_ai_scheduler.domain.repository.MeetingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject

/**
 * Concept: SchedulingUiState
 * Represents the different visual states of the Scheduling screen.
 */
sealed class SchedulingUiState {
    object Idle : SchedulingUiState()
    object Loading : SchedulingUiState()
    data class Success(
        val intent: MeetingIntent,
        val suggestedSlots: List<ScoredSlot>,
        val isConfirming: Boolean = false,
        val confirmationMessage: String? = null
    ) : SchedulingUiState()
    data class Error(val message: String) : SchedulingUiState()
}

/**
 * Concept: SchedulingViewModel (Task 12)
 * Coordinates the scheduling flow:
 * 1. Takes natural language input from the UI.
 * 2. Uses DateTimeParser to extract structured intent.
 * 3. Calls MeetingRepository to fetch recommendations.
 */
@HiltViewModel
class SchedulingViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository,
    private val dateTimeParser: DateTimeParser
) : ViewModel() {

    private val _uiState = MutableStateFlow<SchedulingUiState>(SchedulingUiState.Idle)
    val uiState: StateFlow<SchedulingUiState> = _uiState.asStateFlow()

    /**
     * Called whenever the user types in the search bar.
     */
    fun onQueryChanged(query: String) {
        if (query.isBlank()) {
            _uiState.value = SchedulingUiState.Idle
            return
        }

        // Phase 1: Parse the natural language locally
        val intent = try {
            dateTimeParser.parse(query)
        } catch (e: Exception) {
            _uiState.value = SchedulingUiState.Error(
                e.message ?: "Could not understand this scheduling request"
            )
            return
        }
        
        if (intent != null) {
            // Phase 2: If we have a valid intent, fetch real slots from the repository
            fetchSuggestedSlots(intent)
        } else {
            // If parsing fails, stay in Idle (UI might show "Keep typing...")
            _uiState.value = SchedulingUiState.Idle
        }
    }

    private fun fetchSuggestedSlots(intent: MeetingIntent) {
        viewModelScope.launch {
            _uiState.value = SchedulingUiState.Loading
            
            meetingRepository.getSuggestedSlots(intent).fold(
                onSuccess = { slots ->
                    _uiState.value = SchedulingUiState.Success(intent, slots)
                },
                onFailure = { error ->
                    _uiState.value = SchedulingUiState.Error(
                        error.message ?: "Failed to fetch suggestions"
                    )
                }
            )
        }
    }

    fun confirmSlot(scoredSlot: ScoredSlot, inviteeEmailsText: String) {
        val currentState = _uiState.value as? SchedulingUiState.Success ?: return
        val inviteeEmails = inviteeEmailsText
            .split(",", ";", " ", "\n")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()

        viewModelScope.launch {
            _uiState.value = currentState.copy(isConfirming = true, confirmationMessage = null)

            val meeting = Meeting(
                meetingId = "",
                title = currentState.intent.title ?: "Meeting",
                dateTime = scoredSlot.slot.startDateTime,
                durationMinutes = Duration.between(
                    scoredSlot.slot.startDateTime,
                    scoredSlot.slot.endDateTime
                ).toMinutes().toInt(),
                location = null,
                status = "scheduled",
                createdBy = "",
                participantEmails = inviteeEmails
            )

            meetingRepository.createMeeting(meeting).fold(
                onSuccess = {
                    _uiState.value = currentState.copy(
                        isConfirming = false,
                        confirmationMessage = "Meeting scheduled"
                    )
                },
                onFailure = { error ->
                    _uiState.value = currentState.copy(
                        isConfirming = false,
                        confirmationMessage = error.message ?: "Failed to schedule meeting"
                    )
                }
            )
        }
    }
}

package com.example.meetwise_ai_scheduler.ui.meetings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meetwise_ai_scheduler.domain.model.Meeting
import com.example.meetwise_ai_scheduler.domain.model.Minutes
import com.example.meetwise_ai_scheduler.domain.repository.MeetingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MeetingListUiState {
    object Loading : MeetingListUiState()
    data class Success(
        val meetings: List<Meeting>,
        val recordedMinutes: List<Minutes> = emptyList(),
        val showingRecorded: Boolean = false,
        val isOpeningMinutes: Boolean = false,
        val message: String? = null
    ) : MeetingListUiState()
    data class Error(val message: String) : MeetingListUiState()
}

@HiltViewModel
class MeetingListViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MeetingListUiState>(MeetingListUiState.Loading)
    val uiState: StateFlow<MeetingListUiState> = _uiState.asStateFlow()
    private val _minutesEvents = MutableSharedFlow<Minutes>()
    val minutesEvents: SharedFlow<Minutes> = _minutesEvents.asSharedFlow()

    init {
        loadMeetings()
    }

    fun loadMeetings() {
        viewModelScope.launch {
            _uiState.value = MeetingListUiState.Loading
            meetingRepository.getMeetings().fold(
                onSuccess = { meetings ->
                    _uiState.value = MeetingListUiState.Success(meetings)
                },
                onFailure = { error ->
                    _uiState.value = MeetingListUiState.Error(error.message ?: "Failed to load meetings")
                }
            )
        }
    }

    fun deleteMeeting(meetingId: String) {
        viewModelScope.launch {
            val currentState = _uiState.value as? MeetingListUiState.Success
            if (currentState != null) {
                _uiState.value = currentState.copy(
                    meetings = currentState.meetings.map { meeting ->
                        if (meeting.meetingId == meetingId) {
                            meeting.copy(status = "cancelled")
                        } else {
                            meeting
                        }
                    }
                )
            }

            meetingRepository.deleteMeeting(meetingId).fold(
                onSuccess = {
                    delay(3000)
                    val latestState = _uiState.value as? MeetingListUiState.Success
                    if (latestState != null) {
                        _uiState.value = latestState.copy(
                            meetings = latestState.meetings.filterNot { it.meetingId == meetingId }
                        )
                    } else {
                        loadMeetings()
                    }
                },
                onFailure = { error ->
                    _uiState.value = MeetingListUiState.Error(error.message ?: "Failed to cancel meeting")
                    loadMeetings()
                }
            )
        }
    }

    fun openMeetingMinutes(meetingId: String) {
        viewModelScope.launch {
            val currentState = _uiState.value as? MeetingListUiState.Success ?: return@launch
            _uiState.value = currentState.copy(isOpeningMinutes = true, message = null)
            meetingRepository.getMinutes(meetingId).fold(
                onSuccess = { minutes ->
                    _uiState.value = currentState.copy(isOpeningMinutes = false)
                    _minutesEvents.emit(minutes)
                },
                onFailure = {
                    _uiState.value = currentState.copy(
                        isOpeningMinutes = false,
                        message = "No recorded minutes saved for this meeting yet"
                    )
                }
            )
        }
    }

    fun showRecordedMeetings() {
        viewModelScope.launch {
            val currentState = _uiState.value as? MeetingListUiState.Success ?: return@launch
            _uiState.value = currentState.copy(
                showingRecorded = true,
                isOpeningMinutes = true,
                message = null
            )
            meetingRepository.getRecordedMinutes().fold(
                onSuccess = { minutes ->
                    _uiState.value = currentState.copy(
                        showingRecorded = true,
                        recordedMinutes = minutes,
                        isOpeningMinutes = false,
                        message = if (minutes.isEmpty()) "No recorded meetings saved yet" else null
                    )
                },
                onFailure = { error ->
                    _uiState.value = currentState.copy(
                        showingRecorded = true,
                        isOpeningMinutes = false,
                        message = error.message ?: "Could not load recorded meetings"
                    )
                }
            )
        }
    }

    fun showMeetings() {
        val currentState = _uiState.value as? MeetingListUiState.Success ?: return
        _uiState.value = currentState.copy(showingRecorded = false, message = null)
    }
}

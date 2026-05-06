package com.example.meetwise_ai_scheduler.ui.meetings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meetwise_ai_scheduler.domain.model.Meeting
import com.example.meetwise_ai_scheduler.domain.repository.MeetingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MeetingListUiState {
    object Loading : MeetingListUiState()
    data class Success(val meetings: List<Meeting>) : MeetingListUiState()
    data class Error(val message: String) : MeetingListUiState()
}

@HiltViewModel
class MeetingListViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MeetingListUiState>(MeetingListUiState.Loading)
    val uiState: StateFlow<MeetingListUiState> = _uiState.asStateFlow()

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
            meetingRepository.deleteMeeting(meetingId).fold(
                onSuccess = {
                    loadMeetings() // Refresh the list
                },
                onFailure = {
                    // Handle error (e.g. show toast)
                }
            )
        }
    }
}

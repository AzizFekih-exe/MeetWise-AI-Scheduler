package com.example.meetwise_ai_scheduler.ui.screens.recording

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meetwise_ai_scheduler.domain.repository.MeetingRepository
import com.example.meetwise_ai_scheduler.util.AudioRecorderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class RecordingUiState(
    val isRecording: Boolean = false,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val errorMessage: String? = null
)

@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val meetingRepository: MeetingRepository
) : ViewModel() {

    private val recorder = AudioRecorderManager(context)
    private var outputFile: File? = null

    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    fun startRecording() {
        if (_uiState.value.isRecording) return
        val recordingsRoot = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
        val recordingsDir = File(recordingsRoot, "MeetWise Recordings").apply { mkdirs() }
        val file = File(recordingsDir, "meetwise_recording_${System.currentTimeMillis()}.m4a")
        outputFile = file
        recorder.startRecording(file)
        _uiState.value = RecordingUiState(isRecording = true)
    }

    fun stopAndUpload(meetingId: String, onUploaded: (jobId: String, audioFilePath: String) -> Unit) {
        val file = outputFile
        recorder.stopRecording()

        if (file == null || !file.exists()) {
            _uiState.value = RecordingUiState(errorMessage = "Recording file was not created")
            return
        }

        viewModelScope.launch {
            _uiState.value = RecordingUiState(isUploading = true, uploadProgress = 0.05f)
            val progressJob = launch {
                var progress = 0.05f
                while (_uiState.value.isUploading) {
                    delay(150)
                    progress = (progress + 0.025f).coerceAtMost(0.30f)
                    _uiState.value = _uiState.value.copy(uploadProgress = progress)
                }
            }
            meetingRepository.uploadRecording(meetingId, file).fold(
                onSuccess = { jobId ->
                    progressJob.cancel()
                    _uiState.value = RecordingUiState(isUploading = true, uploadProgress = 0.30f)
                    delay(250)
                    _uiState.value = RecordingUiState()
                    onUploaded(jobId, file.absolutePath)
                },
                onFailure = { error ->
                    progressJob.cancel()
                    _uiState.value = RecordingUiState(
                        errorMessage = error.message ?: "Failed to upload recording"
                    )
                }
            )
        }
    }

    override fun onCleared() {
        recorder.release()
    }
}

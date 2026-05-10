package com.example.meetwise_ai_scheduler.domain.model

data class TranscriptionJobStatus(
    val jobId: String,
    val status: String,
    val progress: Float,
    val message: String,
    val errorMessage: String? = null
)

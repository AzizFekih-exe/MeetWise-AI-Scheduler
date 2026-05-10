package com.example.meetwise_ai_scheduler.ui.screens.transcription

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.meetwise_ai_scheduler.domain.model.Minutes

@Composable
fun TranscriptionProgressScreen(
    meetingId: String,
    jobId: String,
    onCancel: () -> Unit,
    onViewMinutes: (Minutes) -> Unit,
    viewModel: TranscriptionProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentStep = when (uiState.status) {
        "pending" -> 1
        "transcribing", "processing" -> 2
        "generating" -> 3
        "done" -> 3
        "failed" -> if (uiState.progress < 0.70f) 2 else 3
        else -> 1
    }
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress.coerceIn(0f, 1f),
        label = "transcriptionProgress"
    )
    val steps = listOf("Exporting Audio", "Transcribing Content", "Generating Minutes")

    LaunchedEffect(jobId) {
        viewModel.pollJob(meetingId, jobId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (uiState.status == "done") {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        } else {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(72.dp),
                strokeWidth = 7.dp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = when (uiState.status) {
                "done" -> "Minutes Ready"
                "failed" -> "Processing Failed"
                else -> "Processing Meeting"
            },
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Step Indicators
        steps.forEachIndexed { index, stepName ->
            val stepNumber = index + 1
            val isCompleted = stepNumber < currentStep
            val isCurrent = stepNumber == currentStep

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isCurrent || isCompleted,
                    onClick = null,
                    enabled = isCurrent,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                )
                Text(
                    text = stepName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isCurrent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = when (uiState.status) {
                "done" -> "Your transcription job finished successfully."
                "failed" -> uiState.errorMessage ?: "The transcription job failed."
                else -> uiState.message
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(64.dp))

        AnimatedVisibility(visible = uiState.status == "done" && uiState.minutes != null) {
            Button(
                onClick = { uiState.minutes?.let(onViewMinutes) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Minutes")
            }
        }

        AnimatedVisibility(visible = uiState.status == "done" && uiState.minutes == null) {
            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Meetings")
            }
        }

        AnimatedVisibility(visible = uiState.status != "done") {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.status == "failed") "Back to Meetings" else "Cancel Process")
            }
        }
    }
}

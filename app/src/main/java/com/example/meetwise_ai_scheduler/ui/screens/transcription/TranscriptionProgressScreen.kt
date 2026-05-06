package com.example.meetwise_ai_scheduler.ui.screens.transcription

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun TranscriptionProgressScreen(
    currentStep: Int, // 1, 2, or 3
    onCancel: () -> Unit
) {
    val steps = listOf("Uploading Audio", "Transcribing Content", "Generating Minutes")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 6.dp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Processing Meeting",
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
            text = "You can safely close this screen.\nWe'll notify you when your minutes are ready.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(64.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel Process")
        }
    }
}

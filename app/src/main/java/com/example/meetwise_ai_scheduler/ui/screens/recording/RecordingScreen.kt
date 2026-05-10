package com.example.meetwise_ai_scheduler.ui.screens.recording

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

@Composable
fun RecordingScreen(
    meetingId: String,
    onUploaded: (jobId: String, audioFilePath: String) -> Unit,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var secondsElapsed by remember { mutableStateOf(0) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            viewModel.startRecording()
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.startRecording()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Timer Logic
    LaunchedEffect(uiState.isRecording) {
        while (uiState.isRecording) {
            delay(1000)
            secondsElapsed++
        }
    }

    // Pulsing Animation for LIVE indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val animatedUploadProgress by animateFloatAsState(
        targetValue = uiState.uploadProgress,
        label = "uploadProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (uiState.isUploading) {
            CircularProgressIndicator(
                progress = { animatedUploadProgress.coerceIn(0f, 0.30f) / 0.30f },
                modifier = Modifier.size(72.dp),
                strokeWidth = 7.dp
            )
            Spacer(modifier = Modifier.height(24.dp))
            LinearProgressIndicator(
                progress = { animatedUploadProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${(animatedUploadProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(32.dp))
        } else {
            // LIVE Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LIVE",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Timer Text
            Text(
                text = formatTime(secondsElapsed),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Thin
            )

            Spacer(modifier = Modifier.height(64.dp))
        }

        if (!uiState.isUploading) {
            // STOP Button
            Button(
                onClick = { viewModel.stopAndUpload(meetingId, onUploaded) },
                enabled = uiState.isRecording,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.size(80.dp),
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when {
                uiState.isUploading -> "Exporting Audio"
                uiState.isRecording -> "Stop Recording"
                else -> "Preparing Recorder"
            },
            style = MaterialTheme.typography.labelLarge
        )

        uiState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}

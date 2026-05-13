package com.example.meetwise_ai_scheduler.ui.navigation

import android.content.Intent
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.meetwise_ai_scheduler.domain.model.Minutes
import com.example.meetwise_ai_scheduler.ui.auth.AuthScreen
import com.example.meetwise_ai_scheduler.ui.meetings.MeetingListScreen
import com.example.meetwise_ai_scheduler.ui.screens.minutes.MinutesScreen
import com.example.meetwise_ai_scheduler.ui.screens.recording.RecordingScreen
import com.example.meetwise_ai_scheduler.ui.screens.transcription.TranscriptionProgressScreen
import com.example.meetwise_ai_scheduler.ui.scheduling.SchedulingScreen
import com.example.meetwise_ai_scheduler.util.PdfExportManager
import java.io.File

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object MeetingList : Screen("meeting_list")
    object Scheduling : Screen("scheduling")
    object Recording : Screen("recording/{meetingId}") {
        fun createRoute(meetingId: String) = "recording/$meetingId"
    }
    object Transcription : Screen("transcription/{meetingId}/{jobId}") {
        fun createRoute(meetingId: String, jobId: String) = "transcription/$meetingId/$jobId"
    }
    object Minutes : Screen("minutes")
}

@Composable
fun NavGraph(
    isDarkTheme: Boolean,
    largeText: Boolean,
    reduceMotion: Boolean,
    onToggleTheme: () -> Unit,
    onToggleLargeText: () -> Unit,
    onToggleReduceMotion: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var selectedMinutes by remember { mutableStateOf<Minutes?>(null) }
    var selectedAudioFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val enterDuration = if (reduceMotion) 90 else 520
    val exitDuration = if (reduceMotion) 70 else 320
    fun shareFile(file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            data = uri
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, file.name)
            clipData = android.content.ClipData.newUri(context.contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }

    fun exportMinutes(minutes: Minutes) {
        val file = PdfExportManager(context).exportMinutesToPdf(
            minutes = minutes,
            fileName = "meetwise_minutes_${minutes.meetingId}_${System.currentTimeMillis()}"
        )
        if (file == null) {
            Toast.makeText(context, "Could not export PDF", Toast.LENGTH_SHORT).show()
            return
        }

        shareFile(file, "application/pdf", "Export meeting minutes")
    }

    fun exportAudio(audioFilePath: String?) {
        val file = audioFilePath?.let(::File)
        if (file == null || !file.exists()) {
            Toast.makeText(context, "No recording audio found for this meeting", Toast.LENGTH_SHORT).show()
            return
        }

        val extension = file.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension)
            ?: "audio/mp4"
        shareFile(file, mimeType, "Export meeting recording")
    }

    if (showSettings) {
        MeetWiseSettingsDialog(
            isDarkTheme = isDarkTheme,
            largeText = largeText,
            reduceMotion = reduceMotion,
            onToggleTheme = onToggleTheme,
            onToggleLargeText = onToggleLargeText,
            onToggleReduceMotion = onToggleReduceMotion,
            onDismiss = { showSettings = false }
        )
    }
    
    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route,
        enterTransition = {
            fadeIn(animationSpec = tween(enterDuration, easing = FastOutSlowInEasing)) +
            scaleIn(
                initialScale = if (reduceMotion) 0.99f else 0.94f,
                animationSpec = tween(enterDuration, easing = FastOutSlowInEasing)
            ) +
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(enterDuration, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(exitDuration, easing = FastOutSlowInEasing)) +
            scaleOut(
                targetScale = if (reduceMotion) 0.99f else 0.97f,
                animationSpec = tween(exitDuration, easing = FastOutSlowInEasing)
            ) +
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(exitDuration, easing = FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(enterDuration, easing = FastOutSlowInEasing)) +
            scaleIn(
                initialScale = if (reduceMotion) 0.99f else 0.94f,
                animationSpec = tween(enterDuration, easing = FastOutSlowInEasing)
            ) +
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(enterDuration, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(exitDuration, easing = FastOutSlowInEasing)) +
            scaleOut(
                targetScale = if (reduceMotion) 0.99f else 0.97f,
                animationSpec = tween(exitDuration, easing = FastOutSlowInEasing)
            ) +
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(exitDuration, easing = FastOutSlowInEasing)
            )
        }
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthenticated = {
                    navController.navigate(Screen.MeetingList.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.MeetingList.route) {
            MeetingListScreen(
                onNavigateToScheduling = {
                    navController.navigate(Screen.Scheduling.route)
                },
                onNavigateToRecording = { meetingId ->
                    navController.navigate(Screen.Recording.createRoute(meetingId))
                },
                onOpenMinutes = { minutes ->
                    selectedMinutes = minutes
                    selectedAudioFilePath = null
                    navController.navigate(Screen.Minutes.route)
                },
                onLogout = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.MeetingList.route) { inclusive = true }
                    }
                },
                onContactSupport = {
                    Toast.makeText(context, "Support: meetwise.help@gmail.com", Toast.LENGTH_LONG).show()
                },
                isDarkTheme = isDarkTheme,
                onOpenSettings = { showSettings = true }
            )
        }
        composable(Screen.Scheduling.route) {
            SchedulingScreen(
                onNavigateHome = {
                    val popped = navController.popBackStack(Screen.MeetingList.route, inclusive = false)
                    if (!popped) {
                        navController.navigate(Screen.MeetingList.route)
                    }
                },
                isDarkTheme = isDarkTheme,
                reduceMotion = reduceMotion,
                onOpenSettings = { showSettings = true }
            )
        }
        composable(
            route = Screen.Recording.route,
            arguments = listOf(navArgument("meetingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getString("meetingId").orEmpty()
            RecordingScreen(
                meetingId = meetingId,
                onUploaded = { jobId, audioFilePath ->
                    selectedAudioFilePath = audioFilePath
                    navController.navigate(Screen.Transcription.createRoute(meetingId, jobId)) {
                        popUpTo(Screen.Recording.route) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.Transcription.route,
            arguments = listOf(
                navArgument("meetingId") { type = NavType.StringType },
                navArgument("jobId") { type = NavType.StringType }
            )
        ) {
            val meetingId = it.arguments?.getString("meetingId").orEmpty()
            val jobId = it.arguments?.getString("jobId").orEmpty()
            TranscriptionProgressScreen(
                meetingId = meetingId,
                jobId = jobId,
                onCancel = { navController.popBackStack(Screen.MeetingList.route, inclusive = false) },
                onViewMinutes = { minutes ->
                    selectedMinutes = minutes
                    navController.navigate(Screen.Minutes.route)
                }
            )
        }
        composable(Screen.Minutes.route) {
            selectedMinutes?.let { minutes ->
                MinutesScreen(
                    minutes = minutes,
                    onBackToMeetings = {
                        navController.navigate(Screen.MeetingList.route) {
                            popUpTo(Screen.MeetingList.route) { inclusive = true }
                        }
                    },
                    onExportPdf = { exportMinutes(minutes) },
                    onExportAudio = { exportAudio(selectedAudioFilePath) },
                    onShare = { exportMinutes(minutes) },
                    canExportAudio = selectedAudioFilePath?.let { File(it).exists() } == true,
                    onToggleActionItem = { }
                )
            }
        }
    }
}

@Composable
private fun MeetWiseSettingsDialog(
    isDarkTheme: Boolean,
    largeText: Boolean,
    reduceMotion: Boolean,
    onToggleTheme: () -> Unit,
    onToggleLargeText: () -> Unit,
    onToggleReduceMotion: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SettingsToggleRow(
                    title = "Dark theme",
                    description = "Use the darker MeetWise interface",
                    checked = isDarkTheme,
                    onCheckedChange = { onToggleTheme() }
                )
                HorizontalDivider()
                SettingsToggleRow(
                    title = "Larger text",
                    description = "Increase text size across the app",
                    checked = largeText,
                    onCheckedChange = { onToggleLargeText() }
                )
                HorizontalDivider()
                SettingsToggleRow(
                    title = "Reduced motion",
                    description = "Use shorter screen transitions",
                    checked = reduceMotion,
                    onCheckedChange = { onToggleReduceMotion() }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

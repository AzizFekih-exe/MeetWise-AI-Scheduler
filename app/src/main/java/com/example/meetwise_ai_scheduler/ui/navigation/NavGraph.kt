package com.example.meetwise_ai_scheduler.ui.navigation

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
    onToggleTheme: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var selectedMinutes by remember { mutableStateOf<Minutes?>(null) }
    var selectedAudioFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    fun exportMinutes(minutes: Minutes) {
        val file = PdfExportManager(context).exportMinutesToPdf(
            minutes = minutes,
            fileName = "meetwise_minutes_${minutes.meetingId}_${System.currentTimeMillis()}"
        )
        if (file == null) {
            Toast.makeText(context, "Could not export PDF", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export meeting minutes"))
    }

    fun exportAudio(audioFilePath: String?) {
        val file = audioFilePath?.let(::File)
        if (file == null || !file.exists()) {
            Toast.makeText(context, "No recording audio found for this meeting", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export meeting recording"))
    }
    
    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route,
        enterTransition = {
            fadeIn(animationSpec = tween(220)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(220)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(160)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(160)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(220)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(220)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(160)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(160)
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
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )
        }
        composable(Screen.Scheduling.route) {
            SchedulingScreen(
                onNavigateHome = {
                    navController.navigate(Screen.MeetingList.route) {
                        popUpTo(Screen.MeetingList.route) { inclusive = true }
                    }
                },
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
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

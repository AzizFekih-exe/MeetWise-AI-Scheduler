package com.example.meetwise_ai_scheduler.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.meetwise_ai_scheduler.ui.meetings.MeetingListScreen
import com.example.meetwise_ai_scheduler.ui.scheduling.SchedulingScreen

sealed class Screen(val route: String) {
    object MeetingList : Screen("meeting_list")
    object Scheduling : Screen("scheduling")
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Screen.MeetingList.route
    ) {
        composable(Screen.MeetingList.route) {
            MeetingListScreen(
                onNavigateToScheduling = {
                    navController.navigate(Screen.Scheduling.route)
                }
            )
        }
        composable(Screen.Scheduling.route) {
            SchedulingScreen()
        }
    }
}

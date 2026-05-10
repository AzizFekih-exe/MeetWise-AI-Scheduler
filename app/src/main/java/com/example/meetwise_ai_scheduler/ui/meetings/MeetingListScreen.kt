package com.example.meetwise_ai_scheduler.ui.meetings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.meetwise_ai_scheduler.domain.model.Meeting
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingListScreen(
    onNavigateToScheduling: () -> Unit,
    onNavigateToRecording: (String) -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    viewModel: MeetingListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadMeetings()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Meetings", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = onToggleTheme) {
                        Text(if (isDarkTheme) "Light" else "Dark")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToScheduling,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = { Text("Schedule") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToScheduling) {
                Icon(Icons.Default.Add, contentDescription = "Add Meeting")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is MeetingListUiState.Loading -> {
                    if (state == MeetingListUiState.Loading) {
                         CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
                is MeetingListUiState.Success -> {
                    AnimatedVisibility(visible = state.meetings.isEmpty()) {
                        Text(
                            "No meetings scheduled yet.",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AnimatedVisibility(visible = state.meetings.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.meetings) { meeting ->
                                MeetingItem(
                                    meeting = meeting,
                                    onRecord = { onNavigateToRecording(meeting.meetingId) },
                                    onDelete = { viewModel.deleteMeeting(meeting.meetingId) }
                                )
                            }
                        }
                    }
                }
                is MeetingListUiState.Error -> {
                    Text(
                        state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun MeetingItem(
    meeting: Meeting,
    onRecord: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .animateContentSize()
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meeting.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${meeting.dateTime.format(dateFormatter)} at ${meeting.dateTime.format(timeFormatter)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Status Chip
                SuggestionChip(
                    onClick = { },
                    label = { Text(meeting.status) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = when(meeting.status.lowercase()) {
                            "scheduled", "confirmed", "upcoming" -> MaterialTheme.colorScheme.primaryContainer
                            "completed" -> Color.LightGray
                            "cancelled" -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.errorContainer
                        }
                    )
                )
            }

            TextButton(
                onClick = onRecord,
                enabled = meeting.status.lowercase() != "cancelled"
            ) {
                Text("Record")
            }

            IconButton(
                onClick = onDelete,
                enabled = meeting.status.lowercase() != "cancelled"
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

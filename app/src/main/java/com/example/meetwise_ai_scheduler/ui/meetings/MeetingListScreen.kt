package com.example.meetwise_ai_scheduler.ui.meetings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.meetwise_ai_scheduler.domain.model.Meeting
import com.example.meetwise_ai_scheduler.domain.model.Minutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.min

private enum class HomeTab {
    Meetings,
    Calendar
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingListScreen(
    onNavigateToScheduling: () -> Unit,
    onNavigateToRecording: (String) -> Unit,
    onOpenMinutes: (Minutes) -> Unit,
    onLogout: () -> Unit,
    onContactSupport: () -> Unit,
    isDarkTheme: Boolean,
    onOpenSettings: () -> Unit,
    viewModel: MeetingListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var horizontalDrag by remember { mutableFloatStateOf(0f) }
    var isDraggingContent by remember { androidx.compose.runtime.mutableStateOf(false) }
    var contentVisible by remember { androidx.compose.runtime.mutableStateOf(false) }
    var homeTab by remember { androidx.compose.runtime.mutableStateOf(HomeTab.Meetings) }
    var searchOpen by remember { androidx.compose.runtime.mutableStateOf(false) }
    var searchQuery by remember { androidx.compose.runtime.mutableStateOf("") }
    val screenWidthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }
    val animatedDragOffset by animateFloatAsState(
        targetValue = horizontalDrag,
        animationSpec = tween(
            durationMillis = if (isDraggingContent) 0 else 240,
            easing = FastOutSlowInEasing
        ),
        label = "meeting-swipe-offset"
    )

    LaunchedEffect(Unit) {
        contentVisible = true
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadMeetings()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.minutesEvents.collect { minutes -> onOpenMinutes(minutes) }
    }

    val showingRecorded = (uiState as? MeetingListUiState.Success)?.showingRecorded == true

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "MeetWise",
                    modifier = Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Workspace",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                NavigationDrawerItem(
                    label = { Text("My meetings") },
                    selected = !showingRecorded,
                    onClick = {
                        viewModel.showMeetings()
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Recorded meetings") },
                    selected = showingRecorded,
                    onClick = {
                        viewModel.showRecordedMeetings()
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Check, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                NavigationDrawerItem(
                    label = { Text("Contact support") },
                    selected = false,
                    onClick = {
                        onContactSupport()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Log out") },
                    selected = false,
                    onClick = onLogout,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("MeetWise", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Home, contentDescription = "Open menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { searchOpen = !searchOpen }) {
                            Icon(Icons.Default.Search, contentDescription = "Search meetings")
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        selected = !showingRecorded && homeTab == HomeTab.Meetings,
                        onClick = {
                            homeTab = HomeTab.Meetings
                            viewModel.showMeetings()
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Meetings") }
                    )
                    NavigationBarItem(
                        selected = !showingRecorded && homeTab == HomeTab.Calendar,
                        onClick = {
                            homeTab = HomeTab.Calendar
                            viewModel.showMeetings()
                        },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        label = { Text("Calendar") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = onNavigateToScheduling,
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        label = { Text("Schedule") }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .clipToBounds()
            ) {
                ScheduleSwipePreview(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = screenWidthPx + animatedDragOffset
                            alpha = min(abs(animatedDragOffset) / (screenWidthPx * 0.45f), 1f)
                        }
                )
                DrawerSwipePreview(
                    modifier = Modifier
                        .width(292.dp)
                        .fillMaxSize()
                        .graphicsLayer {
                            val progress = min(animatedDragOffset / (screenWidthPx * 0.35f), 1f)
                            translationX = -292.dp.toPx() + (292.dp.toPx() * progress)
                            alpha = progress
                        }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                    .graphicsLayer {
                        translationX = animatedDragOffset
                        val distance = min(abs(animatedDragOffset) / screenWidthPx, 1f)
                        alpha = 1f - (distance * 0.18f)
                        scaleX = 1f - (distance * 0.025f)
                        scaleY = 1f - (distance * 0.025f)
                    }
                    .pointerInput(screenWidthPx) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                horizontalDrag = 0f
                                isDraggingContent = true
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                horizontalDrag = (horizontalDrag + dragAmount)
                                    .coerceIn(-screenWidthPx, screenWidthPx * 0.35f)
                            },
                            onDragEnd = {
                                isDraggingContent = false
                                if (horizontalDrag < -90f) {
                                    horizontalDrag = -screenWidthPx
                                    scope.launch {
                                        delay(140)
                                        onNavigateToScheduling()
                                        horizontalDrag = 0f
                                    }
                                } else if (horizontalDrag > 90f) {
                                    horizontalDrag = 0f
                                    scope.launch { drawerState.open() }
                                } else {
                                    horizontalDrag = 0f
                                }
                            }
                        )
                    }
                ) {
                when (val state = uiState) {
                    is MeetingListUiState.Loading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                    is MeetingListUiState.Success -> {
                        if (state.isOpeningMinutes) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        AnimatedVisibility(
                            visible = contentVisible,
                            enter = fadeIn(animationSpec = tween(360)) +
                                slideInVertically(animationSpec = tween(420), initialOffsetY = { it / 5 }) +
                                expandVertically(animationSpec = tween(420))
                        ) {
                            if (state.showingRecorded) {
                                RecordedMinutesContent(
                                    minutes = state.recordedMinutes,
                                    onOpenMinutes = onOpenMinutes
                                )
                            } else if (homeTab == HomeTab.Calendar) {
                                CalendarMeetingsContent(
                                    meetings = state.meetings,
                                    searchOpen = searchOpen,
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { searchQuery = it },
                                    onOpenMinutes = { viewModel.openMeetingMinutes(it) },
                                    onRecord = onNavigateToRecording,
                                    onDelete = { viewModel.deleteMeeting(it) }
                                )
                            } else {
                                MeetingsContent(
                                    meetings = state.meetings,
                                    searchOpen = searchOpen,
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { searchQuery = it },
                                    onOpenMinutes = { viewModel.openMeetingMinutes(it) },
                                    onRecord = onNavigateToRecording,
                                    onDelete = { viewModel.deleteMeeting(it) }
                                )
                            }
                        }
                        state.message?.let { message ->
                            Snackbar(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp)
                            ) {
                                Text(message)
                            }
                        }
                    }
                    is MeetingListUiState.Error -> Text(
                        state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
}

@Composable
private fun ScheduleSwipePreview(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Schedule",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Release to create a meeting",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DrawerSwipePreview(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "MeetWise",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("My meetings", color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Recorded meetings", color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Contact support", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Log out", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MeetingsContent(
    meetings: List<Meeting>,
    searchOpen: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenMinutes: (String) -> Unit,
    onRecord: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val filteredMeetings = remember(meetings, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            meetings
        } else {
            meetings.filter { meeting ->
                meeting.title.contains(query, ignoreCase = true) ||
                    meeting.status.contains(query, ignoreCase = true) ||
                    meeting.dateTime.format(DateTimeFormatter.ofPattern("EEE, MMM d h:mm a"))
                        .contains(query, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MeetingSearchBar(
            visible = searchOpen,
            query = searchQuery,
            onQueryChange = onSearchQueryChange
        )

    if (filteredMeetings.isEmpty()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (meetings.isEmpty()) "No meetings scheduled yet." else "No meetings found.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (meetings.isEmpty()) "Use Schedule to create your next meeting." else "Try another meeting name.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredMeetings) { meeting ->
                AnimatedMeetingRow(meeting.meetingId) {
                    MeetingItem(
                        meeting = meeting,
                        onClick = { onOpenMinutes(meeting.meetingId) },
                        onRecord = { onRecord(meeting.meetingId) },
                        onDelete = { onDelete(meeting.meetingId) }
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun CalendarMeetingsContent(
    meetings: List<Meeting>,
    searchOpen: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenMinutes: (String) -> Unit,
    onRecord: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val filteredMeetings = remember(meetings, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            meetings
        } else {
            meetings.filter { meeting ->
                meeting.title.contains(query, ignoreCase = true) ||
                    meeting.dateTime.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
                        .contains(query, ignoreCase = true)
            }
        }
    }
    val groupedMeetings = filteredMeetings
        .sortedBy { it.dateTime }
        .groupBy { it.dateTime.toLocalDate() }
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")

    Column(modifier = Modifier.fillMaxSize()) {
        MeetingSearchBar(
            visible = searchOpen,
            query = searchQuery,
            onQueryChange = onSearchQueryChange
        )

        if (groupedMeetings.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No calendar days yet.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Scheduled meetings will appear by day.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                groupedMeetings.forEach { (day, dayMeetings) ->
                    item(key = day.toString()) {
                        Column {
                            Text(
                                day.format(dayFormatter),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                dayMeetings.forEach { meeting ->
                                    AnimatedMeetingRow("calendar-${meeting.meetingId}") {
                                        MeetingItem(
                                            meeting = meeting,
                                            onClick = { onOpenMinutes(meeting.meetingId) },
                                            onRecord = { onRecord(meeting.meetingId) },
                                            onDelete = { onDelete(meeting.meetingId) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MeetingSearchBar(
    visible: Boolean,
    query: String,
    onQueryChange: (String) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(240)) +
            slideInVertically(animationSpec = tween(280), initialOffsetY = { -it / 2 }) +
            expandVertically(animationSpec = tween(280))
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Search by meeting name or day") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )
    }
}

@Composable
private fun AnimatedMeetingRow(
    key: String,
    content: @Composable () -> Unit
) {
    var visible by remember(key) { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(key) {
        visible = false
        delay(40)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(260)) +
            slideInHorizontally(
                animationSpec = tween(320, easing = FastOutSlowInEasing),
                initialOffsetX = { it / 4 }
            ) +
            slideInVertically(
                animationSpec = tween(320, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 6 }
            )
    ) {
        content()
    }
}

@Composable
private fun RecordedMinutesContent(
    minutes: List<Minutes>,
    onOpenMinutes: (Minutes) -> Unit
) {
    if (minutes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "No recorded meetings yet.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Generated minutes will appear here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(minutes) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenMinutes(item) },
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Meeting #${item.meetingId}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            item.summary,
                            maxLines = 3,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (item.actionItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "${item.actionItems.size} action item${if (item.actionItems.size == 1) "" else "s"}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MeetingItem(
    meeting: Meeting,
    onClick: () -> Unit,
    onRecord: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val elevation by animateDpAsState(targetValue = 3.dp, label = "meeting-card-elevation")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
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
                SuggestionChip(
                    onClick = { },
                    label = { Text(meeting.status) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = when (meeting.status.lowercase()) {
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
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
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

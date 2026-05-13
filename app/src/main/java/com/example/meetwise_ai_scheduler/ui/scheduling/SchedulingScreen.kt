package com.example.meetwise_ai_scheduler.ui.scheduling

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.meetwise_ai_scheduler.domain.model.ScoredSlot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulingScreen(
    onNavigateHome: () -> Unit,
    onNavigateCalendar: () -> Unit,
    isDarkTheme: Boolean,
    reduceMotion: Boolean,
    onOpenSettings: () -> Unit,
    viewModel: SchedulingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    var inviteeEmails by remember { mutableStateOf("") }
    var horizontalDrag by remember { mutableFloatStateOf(0f) }
    var isDraggingContent by remember { mutableStateOf(false) }
    var screenVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val screenWidthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }
    val animatedDragOffset by animateFloatAsState(
        targetValue = horizontalDrag,
        animationSpec = tween(
            durationMillis = if (isDraggingContent) 0 else if (reduceMotion) 90 else 240,
            easing = FastOutSlowInEasing
        ),
        label = "schedule-swipe-offset"
    )

    LaunchedEffect(Unit) {
        screenVisible = true
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Schedule", fontWeight = FontWeight.Bold) },
                actions = {
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
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateHome,
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateCalendar,
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    label = { Text("Calendar") }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { },
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
            CalendarSwipePreview(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = -screenWidthPx + animatedDragOffset
                        alpha = min(animatedDragOffset / (screenWidthPx * 0.45f), 1f)
                    }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
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
                                .coerceIn(-screenWidthPx * 0.25f, screenWidthPx)
                        },
                        onDragEnd = {
                            isDraggingContent = false
                            if (horizontalDrag > 90f) {
                                horizontalDrag = screenWidthPx
                                scope.launch {
                                    delay(if (reduceMotion) 60 else 140)
                                    onNavigateCalendar()
                                    horizontalDrag = 0f
                                }
                            } else {
                                horizontalDrag = 0f
                            }
                        }
                    )
                }
            ) {
            AnimatedVisibility(
                visible = screenVisible,
                enter = fadeIn(animationSpec = tween(if (reduceMotion) 90 else 360)) +
                    slideInVertically(
                        animationSpec = tween(if (reduceMotion) 90 else 420),
                        initialOffsetY = { -it / 4 }
                    )
            ) {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            viewModel.onQueryChanged(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Design review tomorrow at 2pm") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = MaterialTheme.shapes.medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inviteeEmails,
                        onValueChange = { inviteeEmails = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Invite emails, separated by commas") },
                        supportingText = { Text("Example: one@gmail.com, two@gmail.com") },
                        singleLine = false,
                        maxLines = 3,
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Results Section
            when (val state = uiState) {
                is SchedulingUiState.Idle -> {
                    Text(
                        "Start typing to see suggestions...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                is SchedulingUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is SchedulingUiState.Success -> {
                    AnimatedVisibility(visible = true) {
                        Text(
                            "All Suggestions",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    state.confirmationMessage?.let { message ->
                        Text(
                            text = message,
                            color = if (message == "Meeting scheduled") {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    if (state.suggestedSlots.isEmpty()) {
                        Text(
                            "No future suggestions match this request.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(state.suggestedSlots) { index, scoredSlot ->
                                AnimatedSuggestionCard(
                                    index = index,
                                    reduceMotion = reduceMotion,
                                    scoredSlot = scoredSlot,
                                    isConfirming = state.isConfirming,
                                    onConfirm = { viewModel.confirmSlot(scoredSlot, inviteeEmails) }
                                )
                            }
                        }
                    }
                }
                is SchedulingUiState.Error -> {
                    Text(
                        state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun CalendarSwipePreview(modifier: Modifier = Modifier) {
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
                Icons.Default.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Calendar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Release to view meeting days",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnimatedSuggestionCard(
    index: Int,
    reduceMotion: Boolean,
    scoredSlot: ScoredSlot,
    isConfirming: Boolean,
    onConfirm: () -> Unit
) {
    var visible by remember(scoredSlot.slot.startDateTime) { mutableStateOf(false) }
    LaunchedEffect(scoredSlot.slot.startDateTime, reduceMotion) {
        kotlinx.coroutines.delay(if (reduceMotion) 20L else index * 80L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(if (reduceMotion) 80 else 260)) +
            slideInHorizontally(
                animationSpec = tween(if (reduceMotion) 80 else 360, easing = FastOutSlowInEasing),
                initialOffsetX = { if (index % 2 == 0) it / 2 else -it / 2 }
            ) +
            slideInVertically(
                animationSpec = tween(if (reduceMotion) 80 else 360, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 5 }
            ) +
            expandVertically(animationSpec = tween(if (reduceMotion) 80 else 360, easing = FastOutSlowInEasing))
    ) {
        SlotCard(
            scoredSlot = scoredSlot,
            isConfirming = isConfirming,
            onConfirm = onConfirm
        )
    }
}

@Composable
fun SlotCard(
    scoredSlot: ScoredSlot,
    isConfirming: Boolean,
    onConfirm: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .animateContentSize()
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scoredSlot.slot.startDateTime.format(dateFormatter),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${scoredSlot.slot.startDateTime.format(timeFormatter)} - ${scoredSlot.slot.endDateTime.format(timeFormatter)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Score Badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Score: ${(scoredSlot.score * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Button(
                onClick = onConfirm,
                enabled = !isConfirming,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm")
            }
        }
    }
}

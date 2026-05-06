package com.example.meetwise_ai_scheduler.ui.screens.availability

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.meetwise_ai_scheduler.domain.model.AvailabilityWindow

@Composable
fun AvailabilityScreen(
    currentWindows: List<AvailabilityWindow>,
    onSave: (List<AvailabilityWindow>) -> Unit,
    onAddException: () -> Unit
) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
    val times = listOf("Morning", "Afternoon")
    
    // Local state to track which cells are selected
    // For simplicity, we'll use a set of "DayIndex-TimeIndex" strings
    var selectedCells by remember { mutableStateOf(setOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Weekly Availability",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // Grid Header (Days)
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1f)) // Offset for time labels
            days.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid Content (Times x Days)
        times.forEachIndexed { timeIndex, timeLabel ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time Label
                Text(
                    text = timeLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall
                )

                // Day Cells
                days.forEachIndexed { dayIndex, _ ->
                    val cellKey = "$dayIndex-$timeIndex"
                    val isSelected = selectedCells.contains(cellKey)
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(2.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                else Color.Transparent
                            )
                            .clickable {
                                selectedCells = if (isSelected) {
                                    selectedCells - cellKey
                                } else {
                                    selectedCells + cellKey
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Text("✓", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onAddException,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Exception Date")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { /* TODO: Map selectedCells back to model list and call onSave */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Changes")
        }
    }
}

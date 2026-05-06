package com.example.meetwise_ai_scheduler.ui.screens.minutes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.meetwise_ai_scheduler.domain.model.ActionItem
import com.example.meetwise_ai_scheduler.domain.model.Minutes

@OptIn(Material3Api::class)
@Composable
fun MinutesScreen(
    minutes: Minutes,
    onExportPdf: () -> Unit,
    onShare: () -> Unit,
    onToggleActionItem: (ActionItem) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meeting Minutes") },
                actions = {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Summary Section
            item {
                Text(
                    text = "Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = minutes.summary,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Action Items Section
            item {
                Text(
                    text = "Action Items",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(minutes.actionItems) { item ->
                ActionItemRow(
                    actionItem = item,
                    onToggle = { onToggleActionItem(item) }
                )
            }

            // Export Button
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onExportPdf,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export as PDF")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ActionItemRow(
    actionItem: ActionItem,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = actionItem.done,
            onCheckedChange = { onToggle() }
        )
        Column {
            Text(
                text = actionItem.task,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Owner: ${actionItem.owner}${if (actionItem.deadline != null) " | Due: ${actionItem.deadline}" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

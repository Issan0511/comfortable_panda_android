package com.example.pandaapp.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pandaapp.data.model.Assignment
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToLogin: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold {
        innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> CircularProgressIndicator()
                    state.error != null -> Text(text = state.error ?: "", color = MaterialTheme.colorScheme.error)
                    state.assignments.isEmpty() -> Text(text = "No assignments found")
                    else -> AssignmentList(assignments = state.assignments)
                }
            }

            LastUpdatedLabel(lastUpdatedEpochSeconds = state.lastUpdatedEpochSeconds)

            Button(
                onClick = viewModel::fetchAssignments,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Log in & Fetch assignments")
            }

            Button(
                onClick = {
                    viewModel.clearCredentialsAndNavigateToLogin()
                    onNavigateToLogin()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Change Password")
            }
        }
    }
}

@Composable
private fun AssignmentList(assignments: List<Assignment>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(assignments) { assignment ->
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(text = assignment.courseName, style = MaterialTheme.typography.titleMedium)
                Text(text = assignment.title, style = MaterialTheme.typography.bodyLarge)
                assignment.dueTimeSeconds?.let {
                    val formattedDate = formatEpochSecondsToJst(it)
                    Text(text = "Due: $formattedDate", style = MaterialTheme.typography.bodySmall)
                }
                assignment.status?.let {
                    Text(text = "Status: $it", style = MaterialTheme.typography.bodySmall)
                }
                Divider(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun LastUpdatedLabel(lastUpdatedEpochSeconds: Long?) {
    val label = lastUpdatedEpochSeconds?.let { timestamp ->
        "最終更新: ${formatEpochSecondsToJst(timestamp)}"
    } ?: "最終更新: -"

    Text(text = label, style = MaterialTheme.typography.bodySmall)
}

private fun formatEpochSecondsToJst(epochSeconds: Long): String {
    val instant = Instant.ofEpochSecond(epochSeconds)
    val zoneId = ZoneId.of("Asia/Tokyo")
    val zonedDateTime = instant.atZone(zoneId)
    val formatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
    return formatter.format(zonedDateTime)
}

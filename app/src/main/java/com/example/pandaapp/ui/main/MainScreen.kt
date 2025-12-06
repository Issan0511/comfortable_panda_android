package com.example.pandaapp.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pandaapp.data.model.Assignment

@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaddingValues(16.dp))
    ) {
        Button(
            onClick = viewModel::fetchAssignments,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Log in & Fetch assignments")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            state.isLoading -> CircularProgressIndicator()
            state.error != null -> Text(text = state.error ?: "", color = MaterialTheme.colorScheme.error)
            state.assignments.isEmpty() -> Text(text = "No assignments found")
            else -> AssignmentList(assignments = state.assignments)
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
                    Text(text = "Due (epoch sec): $it", style = MaterialTheme.typography.bodySmall)
                }
                assignment.status?.let {
                    Text(text = "Status: $it", style = MaterialTheme.typography.bodySmall)
                }
                Divider(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

package com.example.pandaapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pandaapp.data.model.Assignment
import com.example.pandaapp.data.repository.PandaRepository
import com.example.pandaapp.util.AssignmentStore
import com.example.pandaapp.util.CredentialsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainScreenState(
    val isLoading: Boolean = false,
    val assignments: List<Assignment> = emptyList(),
    val error: String? = null
)

class MainViewModel(
    private val repository: PandaRepository,
    private val credentialsStore: CredentialsStore,
    private val assignmentStore: AssignmentStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenState())
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()

    fun fetchAssignments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val credentials = credentialsStore.load()
            if (credentials == null) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Credentials not found.")
                }
                return@launch
            }

            try {
                val fetchedAssignments = repository.fetchAssignments(credentials.username, credentials.password)
                val sortedAssignments = sortAssignments(fetchedAssignments)
                assignmentStore.save(sortedAssignments)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        assignments = sortedAssignments,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    private fun sortAssignments(assignments: List<Assignment>): List<Assignment> {
        val now = System.currentTimeMillis() / 1000

        val (futureAssignments, pastAssignments) = assignments.partition {
            it.dueTimeSeconds != null && it.dueTimeSeconds > now
        }

        val sortedFuture = futureAssignments.sortedBy { it.dueTimeSeconds }
        val sortedPast = pastAssignments.sortedByDescending { it.dueTimeSeconds }

        return sortedFuture + sortedPast
    }

    companion object {
        fun provideFactory(
            repository: PandaRepository,
            credentialsStore: CredentialsStore,
            assignmentStore: AssignmentStore
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repository, credentialsStore, assignmentStore) as T
            }
        }
    }
}

package com.example.pandaapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pandaapp.data.model.Assignment
import com.example.pandaapp.data.repository.PandaRepository
import com.example.pandaapp.util.AssignmentStore
import com.example.pandaapp.util.CredentialsStore
import com.example.pandaapp.util.NewAssignmentNotifier
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
    private val assignmentStore: AssignmentStore,
    private val newAssignmentNotifier: NewAssignmentNotifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenState())
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()

    fun fetchAssignments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val credentials = credentialsStore.load()
            if (credentials == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Credentials not found."
                )
                return@launch
            }

            runCatching {
                repository.loginAndFetchAssignments(credentials.username, credentials.password)
            }.onSuccess { assignments ->
                val savedAssignments = assignmentStore.load()
                val savedIds = savedAssignments.map { it.id }.toSet()
                val freshAssignments = assignments.distinctBy { it.id }
                val newAssignments = freshAssignments.filterNot { it.id in savedIds }

                assignmentStore.save(freshAssignments)
                if (newAssignments.isNotEmpty()) {
                    newAssignmentNotifier.notify(newAssignments)
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    assignments = freshAssignments
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = throwable.message)
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
            assignmentStore: AssignmentStore,
            newAssignmentNotifier: NewAssignmentNotifier
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return MainViewModel(
                        repository,
                        credentialsStore,
                        assignmentStore,
                        newAssignmentNotifier
                    ) as T
                }
            }
    }
}

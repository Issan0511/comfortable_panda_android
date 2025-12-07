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
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: PandaRepository,
    private val credentialsStore: CredentialsStore,
    private val assignmentStore: AssignmentStore,
    private val newAssignmentNotifier: NewAssignmentNotifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    init {
        loadSavedAssignments()
    }

    fun fetchAssignments() {
        val credentials = credentialsStore.load()
        if (credentials == null) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Credentials missing")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
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

    private fun loadSavedAssignments() {
        val saved = assignmentStore.load()
        if (saved.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(assignments = saved)
        }
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

data class MainUiState(
    val assignments: List<Assignment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

package com.example.pandaapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pandaapp.data.model.Assignment
import com.example.pandaapp.data.repository.PandaRepository
import com.example.pandaapp.util.CredentialsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: PandaRepository,
    private val credentialsStore: CredentialsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

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
                _uiState.value = _uiState.value.copy(isLoading = false, assignments = assignments)
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = throwable.message)
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: PandaRepository,
            credentialsStore: CredentialsStore
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return MainViewModel(repository, credentialsStore) as T
                }
            }
    }
}

data class MainUiState(
    val assignments: List<Assignment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

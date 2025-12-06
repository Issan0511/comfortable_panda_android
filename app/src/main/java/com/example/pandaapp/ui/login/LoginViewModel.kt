package com.example.pandaapp.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pandaapp.util.Credentials
import com.example.pandaapp.util.CredentialsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val credentialsStore: CredentialsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }

    fun saveCredentials(onSaved: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                credentialsStore.save(
                    Credentials(
                        username = _uiState.value.username,
                        password = _uiState.value.password
                    )
                )
                _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
                onSaved()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }

    companion object {
        fun provideFactory(credentialsStore: CredentialsStore): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return LoginViewModel(credentialsStore) as T
                }
            }
    }
}

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

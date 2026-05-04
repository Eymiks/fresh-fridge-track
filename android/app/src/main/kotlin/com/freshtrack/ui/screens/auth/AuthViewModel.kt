package com.freshtrack.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshtrack.data.auth.AuthRepository
import com.freshtrack.data.auth.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    private val _ui = MutableStateFlow(AuthUiState())
    val ui = _ui.asStateFlow()

    fun signIn(email: String, password: String) = viewModelScope.launch {
        _ui.update { it.copy(isLoading = true, error = null) }
        runCatching { authRepository.signIn(email, password) }
            .onFailure { e -> _ui.update { it.copy(isLoading = false, error = e.message) } }
            .onSuccess { _ui.update { it.copy(isLoading = false) } }
    }

    fun signUp(email: String, password: String, displayName: String) = viewModelScope.launch {
        _ui.update { it.copy(isLoading = true, error = null) }
        runCatching { authRepository.signUp(email, password, displayName) }
            .onFailure { e -> _ui.update { it.copy(isLoading = false, error = e.message) } }
            .onSuccess { _ui.update { it.copy(isLoading = false) } }
    }

    fun continueAsGuest() = viewModelScope.launch { authRepository.setGuestMode() }

    fun leaveGuestMode() = viewModelScope.launch { authRepository.disableGuestMode() }

    fun signOut() = viewModelScope.launch { authRepository.signOut() }

    fun clearError() = _ui.update { it.copy(error = null) }
}

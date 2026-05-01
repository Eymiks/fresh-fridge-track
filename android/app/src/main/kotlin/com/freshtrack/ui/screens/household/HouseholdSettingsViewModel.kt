package com.freshtrack.ui.screens.household

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshtrack.data.auth.AuthRepository
import com.freshtrack.data.auth.AuthState
import com.freshtrack.data.household.HouseholdRepository
import com.freshtrack.domain.model.Household
import com.freshtrack.domain.model.Member
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HouseholdSettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class HouseholdSettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val householdRepository: HouseholdRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(HouseholdSettingsUiState())
    val ui = _ui.asStateFlow()

    private val authState = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    val household = authState.map { (it as? AuthState.Authenticated)?.household }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val members = authState.map { (it as? AuthState.Authenticated)?.members ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val currentUserId get() = authRepository.currentUserId()

    fun updateDisplayName(memberId: String, name: String) = viewModelScope.launch {
        _ui.update { it.copy(isLoading = true) }
        runCatching { householdRepository.updateDisplayName(memberId, name) }
            .onSuccess { _ui.update { it.copy(isLoading = false, successMessage = "Nom mis à jour") } }
            .onFailure { e -> _ui.update { it.copy(isLoading = false, error = e.message) } }
    }

    fun updateHouseholdName(householdId: String, name: String) = viewModelScope.launch {
        _ui.update { it.copy(isLoading = true) }
        runCatching { householdRepository.updateHouseholdName(householdId, name) }
            .onSuccess { _ui.update { it.copy(isLoading = false, successMessage = "Foyer mis à jour") } }
            .onFailure { e -> _ui.update { it.copy(isLoading = false, error = e.message) } }
    }

    fun removeMember(memberId: String) = viewModelScope.launch {
        runCatching { householdRepository.removeMember(memberId) }
            .onFailure { e -> _ui.update { it.copy(error = e.message) } }
    }

    fun signOut() = viewModelScope.launch { authRepository.signOut() }

    fun clearMessages() = _ui.update { it.copy(error = null, successMessage = null) }
}

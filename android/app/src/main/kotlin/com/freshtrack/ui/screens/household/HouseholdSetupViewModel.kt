package com.freshtrack.ui.screens.household

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshtrack.data.auth.AuthRepository
import com.freshtrack.data.household.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HouseholdSetupUiState(val isLoading: Boolean = false, val error: String? = null)

@HiltViewModel
class HouseholdSetupViewModel @Inject constructor(
    private val householdRepository: HouseholdRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(HouseholdSetupUiState())
    val ui = _ui.asStateFlow()

    fun createHousehold(name: String, displayName: String) = viewModelScope.launch {
        _ui.update { it.copy(isLoading = true, error = null) }
        val userId = authRepository.currentUserId() ?: run {
            _ui.update { it.copy(isLoading = false, error = "Non connecté") }
            return@launch
        }
        runCatching { householdRepository.createHousehold(name, userId, displayName.ifBlank { "Moi" }) }
            .onFailure { e -> _ui.update { it.copy(isLoading = false, error = e.message) } }
            .onSuccess { _ui.update { it.copy(isLoading = false) } }
    }

    fun joinByCode(code: String, displayName: String) = viewModelScope.launch {
        _ui.update { it.copy(isLoading = true, error = null) }
        runCatching { householdRepository.joinByCode(code, displayName.ifBlank { "Moi" }) }
            .onFailure { e -> _ui.update { it.copy(isLoading = false, error = e.message) } }
            .onSuccess { _ui.update { it.copy(isLoading = false) } }
    }
}

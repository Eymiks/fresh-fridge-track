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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HouseholdSettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val avatarUrlOverride: String? = null
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

    private var cachedUserId: String? = null
    private var cachedHouseholdId: String? = null

    val currentUserId get() = authRepository.currentUserId()

    init {
        viewModelScope.launch {
            authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    cachedUserId = state.userId
                    state.household?.id?.let { cachedHouseholdId = it }
                }
            }
        }
    }

    fun updateDisplayName(memberId: String, name: String) = viewModelScope.launch {
        _ui.update { it.copy(isLoading = true) }
        runCatching { householdRepository.updateDisplayName(memberId, name) }
            .onSuccess {
                authRepository.refreshHousehold()
                _ui.update { it.copy(isLoading = false, successMessage = "Nom mis à jour") }
            }
            .onFailure { e -> _ui.update { it.copy(isLoading = false, error = e.message) } }
    }

    fun updateHouseholdName(householdId: String, name: String) = viewModelScope.launch {
        _ui.update { it.copy(isLoading = true) }
        runCatching { householdRepository.updateHouseholdName(householdId, name) }
            .onSuccess {
                authRepository.refreshHousehold()
                _ui.update { it.copy(isLoading = false, successMessage = "Foyer mis à jour") }
            }
            .onFailure { e -> _ui.update { it.copy(isLoading = false, error = e.message) } }
    }

    fun removeMember(memberId: String) = viewModelScope.launch {
        _ui.update { it.copy(isLoading = true) }
        runCatching { householdRepository.removeMember(memberId) }
            .onSuccess {
                authRepository.refreshHousehold()
                _ui.update { it.copy(isLoading = false, successMessage = "Membre retiré") }
            }
            .onFailure { e -> _ui.update { it.copy(isLoading = false, error = e.message) } }
    }

    fun uploadAvatar(householdId: String, userId: String, bytes: ByteArray, ext: String) = viewModelScope.launch {
        _ui.update { it.copy(isLoading = true) }
        runCatching { householdRepository.uploadAvatar(householdId, userId, bytes, ext) }
            .onSuccess { member ->
                authRepository.refreshHousehold()
                _ui.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Avatar mis à jour",
                        avatarUrlOverride = member.avatarUrl
                    )
                }
            }
            .onFailure { e -> _ui.update { it.copy(isLoading = false, error = e.message) } }
    }

    fun uploadCurrentUserAvatar(bytes: ByteArray, ext: String) = viewModelScope.launch {
        _ui.update { it.copy(isLoading = true, error = null, successMessage = null) }
        runCatching {
            val (householdId, userId) = resolveAvatarTarget()
            householdRepository.uploadAvatar(householdId, userId, bytes, ext)
        }
            .onSuccess { member ->
                cachedUserId = member.userId
                cachedHouseholdId = member.householdId
                authRepository.refreshHousehold()
                _ui.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Avatar mis à jour",
                        avatarUrlOverride = member.avatarUrl
                    )
                }
            }
            .onFailure { e ->
                _ui.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Impossible d'envoyer l'avatar."
                    )
                }
            }
    }

    private suspend fun resolveAvatarTarget(): Pair<String, String> {
        val state = authState.value as? AuthState.Authenticated
        val userId = state?.userId
            ?: cachedUserId
            ?: authRepository.currentUserId()
            ?: error("Impossible d'envoyer l'avatar : utilisateur introuvable.")
        val householdId = state?.household?.id
            ?: cachedHouseholdId
            ?: authRepository.refreshHousehold().first?.id
            ?: error("Impossible d'envoyer l'avatar : foyer introuvable.")
        return householdId to userId
    }

    fun signOut() = viewModelScope.launch { authRepository.signOut() }

    fun reportError(message: String) = _ui.update { it.copy(isLoading = false, error = message) }

    fun clearMessages() = _ui.update { it.copy(error = null, successMessage = null) }
}

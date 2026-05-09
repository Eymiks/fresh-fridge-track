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
        validateCredentials(email, password, isSignUp = false)?.let { message ->
            _ui.update { it.copy(isLoading = false, error = message) }
            return@launch
        }

        _ui.update { it.copy(isLoading = true, error = null) }
        runCatching { authRepository.signIn(email, password) }
            .onFailure { e -> _ui.update { it.copy(isLoading = false, error = e.toFriendlyAuthMessage(AuthAction.SignIn)) } }
            .onSuccess { _ui.update { it.copy(isLoading = false) } }
    }

    fun signUp(email: String, password: String, displayName: String) = viewModelScope.launch {
        validateCredentials(email, password, isSignUp = true)?.let { message ->
            _ui.update { it.copy(isLoading = false, error = message) }
            return@launch
        }

        _ui.update { it.copy(isLoading = true, error = null) }
        runCatching { authRepository.signUp(email, password, displayName) }
            .onFailure { e -> _ui.update { it.copy(isLoading = false, error = e.toFriendlyAuthMessage(AuthAction.SignUp)) } }
            .onSuccess { _ui.update { it.copy(isLoading = false) } }
    }

    fun continueAsGuest() = viewModelScope.launch { authRepository.setGuestMode() }

    fun leaveGuestMode() = viewModelScope.launch { authRepository.disableGuestMode() }

    fun signOut() = viewModelScope.launch { authRepository.signOut() }

    fun clearError() = _ui.update { it.copy(error = null) }

    private fun validateCredentials(email: String, password: String, isSignUp: Boolean): String? {
        val trimmedEmail = email.trim()
        if (!EMAIL_REGEX.matches(trimmedEmail)) return "Adresse email invalide."
        if (isSignUp && password.length < MIN_PASSWORD_LENGTH) {
            return "Le mot de passe doit contenir au moins $MIN_PASSWORD_LENGTH caractères."
        }
        return null
    }

    private fun Throwable.toFriendlyAuthMessage(action: AuthAction): String {
        val raw = listOfNotNull(message, cause?.message).joinToString(" ").lowercase()
        return when {
            raw.contains("invalid login") ||
                raw.contains("invalid credentials") ||
                raw.contains("email not confirmed") ||
                raw.contains("invalid_grant") -> "Email ou mot de passe incorrect."
            raw.contains("already registered") ||
                raw.contains("already been registered") ||
                raw.contains("user already exists") -> "Un compte existe déjà avec cette adresse email."
            raw.contains("invalid email") ||
                raw.contains("email address is invalid") -> "Adresse email invalide."
            raw.contains("password") && (
                raw.contains("short") ||
                    raw.contains("least") ||
                    raw.contains("weak") ||
                    raw.contains("strength")
                ) -> "Le mot de passe est trop faible. Utilisez au moins $MIN_PASSWORD_LENGTH caractères."
            raw.contains("network") ||
                raw.contains("timeout") ||
                raw.contains("failed to connect") ||
                raw.contains("unable to resolve host") -> "Connexion impossible. Vérifiez votre réseau puis réessayez."
            action == AuthAction.SignIn -> "Email ou mot de passe incorrect."
            else -> "Impossible de créer le compte pour le moment. Réessayez dans quelques instants."
        }
    }

    private enum class AuthAction { SignIn, SignUp }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
        private val EMAIL_REGEX = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
    }
}

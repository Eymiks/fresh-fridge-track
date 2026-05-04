package com.freshtrack.ui.navigation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshtrack.data.auth.AuthRepository
import com.freshtrack.data.auth.AuthState
import com.freshtrack.data.products.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GuestImportUiState(
    val showDialog: Boolean = false,
    val guestCount: Int = 0,
    val isImporting: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class GuestImportViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val dismissed = MutableStateFlow(false)
    private val importing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    private val authState = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    val ui = combine(
        authState,
        productRepository.observeGuestProducts(),
        dismissed,
        importing,
        error
    ) { auth, guestProducts, isDismissed, isImporting, message ->
        val canImport = auth is AuthState.Authenticated && auth.household != null
        GuestImportUiState(
            showDialog = canImport && guestProducts.isNotEmpty() && !isDismissed,
            guestCount = guestProducts.size,
            isImporting = isImporting,
            error = message
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, GuestImportUiState())

    fun importProducts() = viewModelScope.launch {
        val auth = authState.value as? AuthState.Authenticated ?: return@launch
        val householdId = auth.household?.id ?: return@launch
        importing.value = true
        error.value = null
        runCatching {
            productRepository.importGuestProductsToHousehold(householdId, auth.userId)
        }.onSuccess {
            importing.value = false
            dismissed.value = true
        }.onFailure { throwable ->
            importing.value = false
            error.value = throwable.message ?: "Impossible d'importer les produits invites"
        }
    }

    fun discardProducts() = viewModelScope.launch {
        importing.value = true
        runCatching { productRepository.clearGuestProducts() }
            .onSuccess {
                importing.value = false
                dismissed.value = true
                error.value = null
            }
            .onFailure { throwable ->
                importing.value = false
                error.value = throwable.message ?: "Impossible de supprimer les produits invites"
            }
    }

    fun dismissForNow() {
        dismissed.update { true }
    }
}

@Composable
fun GuestImportPrompt(
    vm: GuestImportViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    if (!ui.showDialog) return

    AlertDialog(
        onDismissRequest = {
            if (!ui.isImporting) vm.dismissForNow()
        },
        title = { Text("Importer le frigo invité ?") },
        text = {
            Text(
                buildString {
                    append(ui.guestCount)
                    append(" produit")
                    if (ui.guestCount > 1) append("s")
                    append(" créé")
                    if (ui.guestCount > 1) append("s")
                    append(" en mode invité peuvent être ajoutés à votre foyer.")
                    ui.error?.let { append("\n\n").append(it) }
                }
            )
        },
        confirmButton = {
            Button(
                onClick = vm::importProducts,
                enabled = !ui.isImporting
            ) {
                if (ui.isImporting) CircularProgressIndicator()
                else Text("Importer")
            }
        },
        dismissButton = {
            TextButton(
                onClick = vm::dismissForNow,
                enabled = !ui.isImporting
            ) {
                Text("Plus tard")
            }
            TextButton(
                onClick = vm::discardProducts,
                enabled = !ui.isImporting
            ) {
                Text("Ignorer")
            }
        }
    )
}

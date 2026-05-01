package com.freshtrack.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshtrack.data.auth.AuthRepository
import com.freshtrack.data.auth.AuthState
import com.freshtrack.data.products.ProductRepository
import com.freshtrack.domain.catalog.getFreezeDuration
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.ProductStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val product: Product? = null,
    val notFound: Boolean = false,
    val authMessage: String? = null,
    val error: String? = null,
    val showOpeningDialog: Boolean = false
)

private data class DetailTransientState(
    val showOpeningDialog: Boolean = false,
    val actionError: String? = null
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val productId: String = checkNotNull(savedStateHandle["productId"])

    private val authState = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    private val transient = MutableStateFlow(DetailTransientState())

    private val productState = authState
        .flatMapLatest { state -> state.toProductState() }
        .catch { e ->
            emit(DetailUiState(isLoading = false, error = e.message ?: "Erreur de chargement du produit."))
        }

    val ui = combine(productState, transient) { state, local ->
        state.copy(
            showOpeningDialog = local.showOpeningDialog,
            error = local.actionError ?: state.error
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DetailUiState())

    private fun AuthState.toProductState(): Flow<DetailUiState> = when (this) {
        AuthState.Loading -> flowOf(DetailUiState(isLoading = true))
        AuthState.NotAuthenticated -> flowOf(
            DetailUiState(
                isLoading = false,
                authMessage = "Connectez-vous pour afficher ce produit."
            )
        )
        AuthState.Guest -> productRepository.observeGuestProduct(productId).map { product ->
            DetailUiState(
                isLoading = false,
                product = product,
                notFound = product == null
            )
        }
        is AuthState.Authenticated -> {
            val hId = household?.id
            if (hId == null) {
                flowOf(
                    DetailUiState(
                        isLoading = false,
                        authMessage = "Aucun foyer n'est associé à ce compte."
                    )
                )
            } else {
                flow {
                    emit(DetailUiState(isLoading = true))
                    val refreshError = runCatching { productRepository.fetchAndCache(hId) }
                        .exceptionOrNull()
                    emitAll(
                        productRepository.observeProduct(hId, productId, members).map { product ->
                            DetailUiState(
                                isLoading = false,
                                product = product,
                                notFound = product == null,
                                error = refreshError?.message
                            )
                        }
                    )
                }
            }
        }
    }

    fun getProduct(): Product? = ui.value.product

    fun setStatus(status: ProductStatus) = viewModelScope.launch {
        val p = getProduct() ?: return@launch
        val result = when (val state = authState.value) {
            is AuthState.Guest -> runCatching { productRepository.setGuestStatus(p, status) }
            is AuthState.Authenticated -> {
                val hId = state.household?.id ?: return@launch
                runCatching { productRepository.setStatus(p, hId, status) }
            }
            else -> return@launch
        }
        result
            .onSuccess { transient.update { it.copy(actionError = null) } }
            .onFailure { e -> transient.update { it.copy(actionError = e.message) } }
    }

    fun openProduct(daysAfterOpening: Int) = viewModelScope.launch {
        val p = getProduct() ?: return@launch
        val updated = p.copy(
            status = ProductStatus.OPENED,
            openedAt = kotlinx.datetime.Clock.System.now(),
            daysAfterOpening = daysAfterOpening
        )
        val result = when (val state = authState.value) {
            is AuthState.Guest -> runCatching { productRepository.updateGuestProduct(updated) }
            is AuthState.Authenticated -> {
                val hId = state.household?.id ?: return@launch
                runCatching { productRepository.updateProduct(updated, hId) }
            }
            else -> return@launch
        }
        result
            .onSuccess { transient.update { it.copy(actionError = null) } }
            .onFailure { e -> transient.update { it.copy(actionError = e.message) } }
        transient.update { it.copy(showOpeningDialog = false) }
    }

    fun updateNotes(notes: String) = viewModelScope.launch {
        val p = getProduct() ?: return@launch
        val updated = p.copy(notes = notes)
        saveProduct(updated)
            .onSuccess { transient.update { it.copy(actionError = null) } }
            .onFailure { e -> transient.update { it.copy(actionError = e.message) } }
    }

    fun freezeProduct() = viewModelScope.launch {
        val p = getProduct() ?: return@launch
        val until = kotlinx.datetime.Clock.System.todayIn(TimeZone.currentSystemDefault())
            .plus(getFreezeDuration(p.category), DateTimeUnit.MONTH)
        saveProduct(p.copy(frozenUntil = until))
            .onSuccess { transient.update { it.copy(actionError = null) } }
            .onFailure { e -> transient.update { it.copy(actionError = e.message) } }
    }

    fun unfreezeProduct() = viewModelScope.launch {
        val p = getProduct() ?: return@launch
        saveProduct(p.copy(frozenUntil = null))
            .onSuccess { transient.update { it.copy(actionError = null) } }
            .onFailure { e -> transient.update { it.copy(actionError = e.message) } }
    }

    private suspend fun saveProduct(product: Product): Result<Unit> = when (val state = authState.value) {
        is AuthState.Guest -> runCatching { productRepository.updateGuestProduct(product) }
        is AuthState.Authenticated -> {
            val hId = state.household?.id ?: return Result.failure(IllegalStateException("Pas de foyer"))
            runCatching { productRepository.updateProduct(product, hId) }
        }
        else -> Result.failure(IllegalStateException("Non connecté"))
    }

    fun deleteProduct(onDeleted: () -> Unit) = viewModelScope.launch {
        val p = getProduct() ?: return@launch
        val result = when (authState.value) {
            is AuthState.Guest -> runCatching { productRepository.removeGuestProduct(p.id) }
            is AuthState.Authenticated -> runCatching { productRepository.removeProduct(p.id) }
            else -> return@launch
        }
        result
            .onSuccess { onDeleted() }
            .onFailure { e -> transient.update { it.copy(actionError = e.message) } }
    }

    fun showOpeningDialog() = transient.update { it.copy(showOpeningDialog = true) }
    fun dismissOpeningDialog() = transient.update { it.copy(showOpeningDialog = false) }
}

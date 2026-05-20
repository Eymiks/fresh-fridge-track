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
import androidx.compose.runtime.Immutable
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

@Immutable
data class DetailUiState(
    val isLoading: Boolean = true,
    val isMutating: Boolean = false,
    val product: Product? = null,
    val notFound: Boolean = false,
    val authMessage: String? = null,
    val error: String? = null,
    val showOpeningDialog: Boolean = false,
    val feedbackMessage: String? = null,
    val feedbackId: Long = 0
)

private data class DetailTransientState(
    val showOpeningDialog: Boolean = false,
    val isMutating: Boolean = false,
    val feedbackMessage: String? = null,
    val feedbackId: Long = 0
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
            isMutating = local.isMutating,
            showOpeningDialog = local.showOpeningDialog,
            feedbackMessage = local.feedbackMessage,
            feedbackId = local.feedbackId
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
        mutate("Statut mis à jour") {
            when (val state = authState.value) {
                is AuthState.Guest -> productRepository.setGuestStatus(p, status)
                is AuthState.Authenticated -> {
                    val hId = state.household?.id ?: error("Pas de foyer")
                    productRepository.setStatus(p, hId, status)
                }
                else -> error("Non connecté")
            }
        }
    }

    fun openProduct(daysAfterOpening: Int) = viewModelScope.launch {
        val p = getProduct() ?: return@launch
        val now = kotlinx.datetime.Clock.System.now()
        val updated = p.copy(
            status = ProductStatus.OPENED,
            openedAt = now,
            statusChangedAt = now,
            daysAfterOpening = daysAfterOpening
        )
        mutate("Produit marqué comme ouvert") {
            when (val state = authState.value) {
                is AuthState.Guest -> productRepository.updateGuestProduct(updated)
                is AuthState.Authenticated -> {
                    val hId = state.household?.id ?: error("Pas de foyer")
                    productRepository.updateProduct(updated, hId)
                }
                else -> error("Non connecté")
            }
        }
        transient.update { it.copy(showOpeningDialog = false) }
    }

    fun updateNotes(notes: String) = viewModelScope.launch {
        val p = getProduct() ?: return@launch
        mutate("Note sauvegardée") {
            saveProduct(p.copy(notes = notes.takeIf { it.isNotBlank() })).getOrThrow()
        }
    }

    fun freezeProduct() = viewModelScope.launch {
        val p = getProduct() ?: return@launch
        val until = kotlinx.datetime.Clock.System.todayIn(TimeZone.currentSystemDefault())
            .plus(getFreezeDuration(p.category), DateTimeUnit.MONTH)
        mutate("Produit mis au congélateur") {
            saveProduct(p.copy(frozenUntil = until)).getOrThrow()
        }
    }

    fun unfreezeProduct() = viewModelScope.launch {
        val p = getProduct() ?: return@launch
        mutate("Produit retiré du congélateur") {
            saveProduct(p.copy(frozenUntil = null)).getOrThrow()
        }
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
        mutate(successMessage = null) {
            when (authState.value) {
                is AuthState.Guest -> productRepository.removeGuestProduct(p.id)
                is AuthState.Authenticated -> productRepository.removeProduct(p.id)
                else -> error("Non connecté")
            }
            onDeleted()
        }
    }

    fun showOpeningDialog() = transient.update { it.copy(showOpeningDialog = true) }
    fun dismissOpeningDialog() = transient.update { it.copy(showOpeningDialog = false) }

    fun clearFeedback() = transient.update { it.copy(feedbackMessage = null) }

    private suspend fun mutate(successMessage: String?, block: suspend () -> Unit) {
        if (transient.value.isMutating) return
        transient.update { it.copy(isMutating = true, feedbackMessage = null) }
        runCatching { block() }
            .onSuccess {
                successMessage?.let { message ->
                    transient.update { state ->
                        state.copy(feedbackMessage = message, feedbackId = state.feedbackId + 1)
                    }
                }
            }
            .onFailure { e ->
                transient.update { state ->
                    state.copy(
                        feedbackMessage = e.message ?: "Action impossible pour le moment.",
                        feedbackId = state.feedbackId + 1
                    )
                }
            }
        transient.update { it.copy(isMutating = false) }
    }
}

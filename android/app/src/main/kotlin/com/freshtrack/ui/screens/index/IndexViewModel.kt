package com.freshtrack.ui.screens.index

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshtrack.data.auth.AuthRepository
import com.freshtrack.data.auth.AuthState
import com.freshtrack.data.products.ProductRepository
import com.freshtrack.domain.model.ExpirationStatus
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.ProductStatus
import com.freshtrack.domain.model.getEffectiveExpirationDate
import com.freshtrack.domain.model.getExpirationStatus
import com.freshtrack.domain.model.isActive
import com.freshtrack.domain.usecase.ExpirationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOrder { EXPIRATION, NAME, ADDED_DATE }
enum class StatusFilter { ALL, EXPIRED, SOON, FRESH }

sealed class SwipeUiEvent {
    data class ShowUndo(
        val message: String,
        val product: Product,
        val previousStatus: ProductStatus
    ) : SwipeUiEvent()
}

@Immutable
data class IndexUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedCategory: String = "all",
    val sortOrder: SortOrder = SortOrder.EXPIRATION,
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val error: String? = null
)

@Immutable
data class ProductGroups(
    val expired: List<Product> = emptyList(),
    val soon: List<Product> = emptyList(),
    val fresh: List<Product> = emptyList()
)

@Immutable
data class TotalCounts(val expired: Int = 0, val soon: Int = 0, val fresh: Int = 0)

@HiltViewModel
class IndexViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val productRepository: ProductRepository,
    private val expirationUseCase: ExpirationUseCase
) : ViewModel() {

    val swipeEvents = Channel<SwipeUiEvent>(Channel.BUFFERED)

    private val _ui = MutableStateFlow(IndexUiState())
    val ui = _ui.asStateFlow()

    private val authState = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    val isGuest: StateFlow<Boolean> = authState
        .map { it is AuthState.Guest }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val products = authState.flatMapLatest { state ->
        when (state) {
            is AuthState.Authenticated -> {
                val hId = state.household?.id ?: return@flatMapLatest flowOf(emptyList())
                productRepository.observeProducts(hId, state.members)
            }
            is AuthState.Guest -> productRepository.observeGuestProducts()
            else -> flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val derived = combine(products, _ui) { prods, uiState ->
        val sorted = sorted(baseFiltered(prods, uiState), uiState.sortOrder)
        val expired = sorted.filter { it.getExpirationStatus() == ExpirationStatus.EXPIRED }
        val soon = sorted.filter { it.getExpirationStatus() == ExpirationStatus.SOON }
        val fresh = sorted.filter { it.getExpirationStatus() == ExpirationStatus.FRESH }
        val counts = TotalCounts(expired.size, soon.size, fresh.size)
        val groups = when (uiState.statusFilter) {
            StatusFilter.ALL -> ProductGroups(expired, soon, fresh)
            StatusFilter.EXPIRED -> ProductGroups(expired = expired)
            StatusFilter.SOON -> ProductGroups(soon = soon)
            StatusFilter.FRESH -> ProductGroups(fresh = fresh)
        }
        counts to groups
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    val totalCounts = derived.map { it.first }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, TotalCounts())

    val groups = derived.map { it.second }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProductGroups())

    init {
        viewModelScope.launch {
            authState.flatMapLatest { state ->
                when {
                    state is AuthState.Loading -> flow {
                        _ui.update { it.copy(isLoading = true) }
                    }
                    state is AuthState.Authenticated && state.household != null -> flow {
                        _ui.update { it.copy(isLoading = true, error = null) }
                        val result = runCatching { productRepository.fetchAndCache(state.household.id) }
                        _ui.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
                        emitAll(productRepository.subscribeToRealtime(state.household.id))
                    }
                    else -> flow {
                        _ui.update { it.copy(isLoading = false) }
                    }
                }
            }.collect {}
        }
    }

    fun setSearch(query: String) = _ui.update { it.copy(searchQuery = query) }
    fun setCategory(key: String) = _ui.update { it.copy(selectedCategory = key) }
    fun setSortOrder(order: SortOrder) = _ui.update { it.copy(sortOrder = order) }
    fun setStatusFilter(filter: StatusFilter) = _ui.update { it.copy(statusFilter = filter) }

    fun enterSelectionMode(productId: String) = _ui.update {
        it.copy(isSelectionMode = true, selectedIds = setOf(productId))
    }
    fun toggleSelection(productId: String) = _ui.update {
        val updated = if (productId in it.selectedIds) it.selectedIds - productId
        else it.selectedIds + productId
        it.copy(isSelectionMode = updated.isNotEmpty(), selectedIds = updated)
    }
    fun exitSelectionMode() = _ui.update { it.copy(isSelectionMode = false, selectedIds = emptySet()) }

    fun quickSetStatus(productId: String, status: ProductStatus) = viewModelScope.launch {
        val product = products.value.find { it.id == productId } ?: return@launch
        val previousStatus = product.status
        val state = authState.value
        when (state) {
            is AuthState.Authenticated -> {
                val hId = state.household?.id ?: return@launch
                runCatching { productRepository.setStatus(product, hId, status) }
            }
            is AuthState.Guest -> runCatching { productRepository.setGuestStatus(product, status) }
            else -> {}
        }
        val message = if (status == ProductStatus.CONSUMED) "Marqué comme consommé" else "Marqué comme jeté"
        swipeEvents.send(SwipeUiEvent.ShowUndo(message, product, previousStatus))
    }

    fun undoSwipe(product: Product, previousStatus: ProductStatus) = viewModelScope.launch {
        val state = authState.value
        when (state) {
            is AuthState.Authenticated -> {
                val hId = state.household?.id ?: return@launch
                runCatching { productRepository.setStatus(product, hId, previousStatus) }
            }
            is AuthState.Guest -> runCatching { productRepository.setGuestStatus(product, previousStatus) }
            else -> {}
        }
    }

    fun setStatusForSelected(status: ProductStatus) = viewModelScope.launch {
        val state = authState.value
        val ids = _ui.value.selectedIds.toList()
        exitSelectionMode()
        ids.forEach { id ->
            products.value.find { it.id == id }?.let { product ->
                when (state) {
                    is AuthState.Authenticated -> {
                        val hId = state.household?.id ?: return@let
                        runCatching { productRepository.setStatus(product, hId, status) }
                    }
                    is AuthState.Guest -> runCatching { productRepository.setGuestStatus(product, status) }
                    else -> {}
                }
            }
        }
    }

    fun deleteSelected() = viewModelScope.launch {
        val ids = _ui.value.selectedIds.toList()
        exitSelectionMode()
        val state = authState.value
        ids.forEach { id ->
            when (state) {
                is AuthState.Guest -> runCatching { productRepository.removeGuestProduct(id) }
                is AuthState.Authenticated -> runCatching { productRepository.removeProduct(id) }
                else -> {}
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        val state = authState.value as? AuthState.Authenticated ?: return@launch
        val hId = state.household?.id ?: return@launch
        _ui.update { it.copy(isLoading = true) }
        runCatching { productRepository.fetchAndCache(hId) }
        _ui.update { it.copy(isLoading = false) }
    }

    fun updateProductDate(productId: String, newDate: LocalDate) = viewModelScope.launch {
        val product = products.value.find { it.id == productId } ?: return@launch
        if (product.expirationDate == newDate) return@launch
        val updated = product.copy(expirationDate = newDate)
        when (val state = authState.value) {
            is AuthState.Authenticated -> {
                val hId = state.household?.id ?: return@launch
                runCatching { productRepository.updateProduct(updated, hId) }
            }
            is AuthState.Guest -> runCatching { productRepository.updateGuestProduct(updated) }
            else -> {}
        }
    }

    private fun baseFiltered(prods: List<Product>, uiState: IndexUiState): List<Product> {
        val active = prods.filter { it.isActive() }
        val searched = expirationUseCase.search(active, uiState.searchQuery)
        return expirationUseCase.filterByCategory(searched, uiState.selectedCategory)
    }

    private fun sorted(prods: List<Product>, order: SortOrder) = when (order) {
        SortOrder.EXPIRATION -> expirationUseCase.sortByExpiration(prods)
        SortOrder.NAME -> prods.sortedBy { it.name.lowercase() }
        SortOrder.ADDED_DATE -> prods.sortedByDescending { it.addedAt }
    }
}

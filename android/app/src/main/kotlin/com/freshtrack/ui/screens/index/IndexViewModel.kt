package com.freshtrack.ui.screens.index

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshtrack.data.auth.AuthRepository
import com.freshtrack.data.auth.AuthState
import com.freshtrack.data.products.ProductRepository
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.ProductStatus
import com.freshtrack.domain.model.getEffectiveExpirationDate
import com.freshtrack.domain.model.getExpirationStatus
import com.freshtrack.domain.model.ExpirationStatus
import com.freshtrack.domain.usecase.ExpirationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IndexUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedCategory: String = "all",
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val error: String? = null
)

data class ProductGroups(
    val expired: List<Product> = emptyList(),
    val soon: List<Product> = emptyList(),
    val fresh: List<Product> = emptyList()
)

@HiltViewModel
class IndexViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val productRepository: ProductRepository,
    private val expirationUseCase: ExpirationUseCase
) : ViewModel() {

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

    val groups = combine(products, _ui) { prods, uiState ->
        val active = prods.filter { it.status == ProductStatus.ACTIVE || it.status == ProductStatus.OPENED }
        val searched = expirationUseCase.search(active, uiState.searchQuery)
        val filtered = expirationUseCase.filterByCategory(searched, uiState.selectedCategory)
        val sorted = filtered.sortedBy { it.getEffectiveExpirationDate() }
        ProductGroups(
            expired = sorted.filter { it.getExpirationStatus() == ExpirationStatus.EXPIRED },
            soon = sorted.filter { it.getExpirationStatus() == ExpirationStatus.SOON },
            fresh = sorted.filter { it.getExpirationStatus() == ExpirationStatus.FRESH }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProductGroups())

    init {
        // Initial fetch + realtime subscription
        viewModelScope.launch {
            authState.flatMapLatest { state ->
                if (state is AuthState.Authenticated && state.household != null) {
                    flow {
                        val result = runCatching { productRepository.fetchAndCache(state.household.id) }
                        _ui.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
                        emitAll(productRepository.subscribeToRealtime(state.household.id))
                    }
                } else {
                    flow {
                        _ui.update { it.copy(isLoading = false) }
                    }
                }
            }.collect {}
        }
    }

    fun setSearch(query: String) = _ui.update { it.copy(searchQuery = query) }
    fun setCategory(key: String) = _ui.update { it.copy(selectedCategory = key) }

    fun enterSelectionMode(productId: String) = _ui.update {
        it.copy(isSelectionMode = true, selectedIds = setOf(productId))
    }
    fun toggleSelection(productId: String) = _ui.update {
        val updated = if (productId in it.selectedIds) it.selectedIds - productId
        else it.selectedIds + productId
        it.copy(isSelectionMode = updated.isNotEmpty(), selectedIds = updated)
    }
    fun exitSelectionMode() = _ui.update { it.copy(isSelectionMode = false, selectedIds = emptySet()) }

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
                    is AuthState.Guest -> {
                        runCatching { productRepository.setGuestStatus(product, status) }
                    }
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
}

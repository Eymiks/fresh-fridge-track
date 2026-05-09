package com.freshtrack.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshtrack.data.auth.AuthRepository
import com.freshtrack.data.auth.AuthState
import com.freshtrack.data.products.ProductRepository
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.ProductStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val authState = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    val historyProducts = authState.flatMapLatest { state ->
        when {
            state is AuthState.Authenticated && state.household != null ->
                productRepository.observeProducts(state.household.id, state.members)
                    .map { filterAndSort(it) }
            state is AuthState.Guest ->
                productRepository.observeGuestProducts()
                    .map { filterAndSort(it) }
            else -> flowOf(emptyList())
        }
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<Product>())

    private fun filterAndSort(products: List<Product>): List<Product> =
        products.filter {
            it.status == ProductStatus.CONSUMED ||
            it.status == ProductStatus.THROWN ||
            it.status == ProductStatus.OPENED
        }.sortedByDescending { it.statusChangedAt }

    fun restoreProduct(product: Product) = viewModelScope.launch {
        when (val state = authState.value) {
            is AuthState.Guest -> runCatching {
                productRepository.setGuestStatus(product, ProductStatus.ACTIVE)
            }.onFailure { _error.value = "Impossible de restaurer le produit" }
            is AuthState.Authenticated -> {
                val householdId = state.household?.id ?: return@launch
                runCatching {
                    productRepository.setStatus(product, householdId, ProductStatus.ACTIVE)
                }.onFailure { _error.value = "Impossible de restaurer le produit" }
            }
            else -> {}
        }
    }

    fun clearError() { _error.value = null }
}

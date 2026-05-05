package com.freshtrack.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshtrack.data.auth.AuthRepository
import com.freshtrack.data.auth.AuthState
import com.freshtrack.data.products.ProductRepository
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.usecase.StatsResult
import com.freshtrack.domain.usecase.StatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel @Inject constructor(
    authRepository: AuthRepository,
    productRepository: ProductRepository,
    private val statsUseCase: StatsUseCase
) : ViewModel() {

    private val authState = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    val products = authState.flatMapLatest { state ->
        when {
            state is AuthState.Authenticated && state.household != null ->
                productRepository.observeProducts(state.household.id, state.members)
            state is AuthState.Guest -> productRepository.observeGuestProducts()
            else -> flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<Product>())

    val stats = products
        .map { products -> statsUseCase.compute(products) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

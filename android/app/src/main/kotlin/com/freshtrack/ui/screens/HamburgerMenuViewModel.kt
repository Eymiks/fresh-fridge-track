@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.freshtrack.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshtrack.data.auth.AuthRepository
import com.freshtrack.data.auth.AuthState
import com.freshtrack.data.products.ProductRepository
import com.freshtrack.domain.model.ExpirationStatus
import com.freshtrack.domain.model.ProductStatus
import com.freshtrack.domain.model.getExpirationStatus
import com.freshtrack.domain.model.isActive
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HamburgerMenuUiState(
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val householdName: String? = null,
    val memberCount: Int = 0,
    val totalProducts: Int = 0,
    val alertCount: Int = 0,
    val historyCount: Int = 0
)

@HiltViewModel
class HamburgerMenuViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val authState = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    private val allProducts = authState.flatMapLatest { state ->
        when (state) {
            is AuthState.Authenticated -> {
                val hId = state.household?.id ?: return@flatMapLatest flowOf(emptyList())
                productRepository.observeProducts(hId, state.members)
            }
            is AuthState.Guest -> productRepository.observeGuestProducts()
            else -> flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<HamburgerMenuUiState> = combine(authState, allProducts) { auth, products ->
        val userId = (auth as? AuthState.Authenticated)?.userId
        val members = (auth as? AuthState.Authenticated)?.members ?: emptyList()
        val currentMember = members.find { it.userId == userId }

        val activeProducts = products.filter { it.isActive() }
        val alertCount = activeProducts.count {
            val s = it.getExpirationStatus()
            s == ExpirationStatus.EXPIRED || s == ExpirationStatus.SOON
        }
        val historyCount = products.count {
            it.status == ProductStatus.CONSUMED || it.status == ProductStatus.THROWN
        }

        HamburgerMenuUiState(
            displayName = currentMember?.displayName
                ?: (auth as? AuthState.Authenticated)?.email?.substringBefore('@'),
            avatarUrl = currentMember?.avatarUrl,
            householdName = (auth as? AuthState.Authenticated)?.household?.name,
            memberCount = members.size,
            totalProducts = activeProducts.size,
            alertCount = alertCount,
            historyCount = historyCount
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HamburgerMenuUiState())

    fun signOut() = viewModelScope.launch {
        authRepository.signOut()
    }
}

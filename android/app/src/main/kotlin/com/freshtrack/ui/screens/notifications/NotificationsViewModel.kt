package com.freshtrack.ui.screens.notifications

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshtrack.data.auth.AuthRepository
import com.freshtrack.data.auth.AuthState
import com.freshtrack.data.prefs.AppPreferences
import com.freshtrack.data.products.ProductRepository
import com.freshtrack.domain.model.ExpirationStatus
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.getExpirationStatus
import com.freshtrack.domain.model.isActive
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import javax.inject.Inject

@Immutable
data class NotificationSettings(
    val enabled: Boolean = true,
    val days: Int = 3
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val prefs: AppPreferences,
    authRepository: AuthRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val authState = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    val settings = combine(prefs.notifEnabled, prefs.notifDays) { enabled, days ->
        NotificationSettings(enabled, days)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, NotificationSettings())

    val permissionRequested = prefs.notifPermissionRequested
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val products = authState.flatMapLatest { state ->
        when (state) {
            is AuthState.Authenticated -> {
                val householdId = state.household?.id ?: return@flatMapLatest flowOf(emptyList())
                productRepository.observeProducts(householdId, state.members)
            }
            is AuthState.Guest -> productRepository.observeGuestProducts()
            else -> flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private data class SplitProducts(val expired: List<Product>, val soon: List<Product>)

    private val splitProducts = products.map { list ->
        val active = list.filter { it.isActive() }
        SplitProducts(
            expired = active.filter { it.getExpirationStatus() == ExpirationStatus.EXPIRED },
            soon = active.filter { it.getExpirationStatus() == ExpirationStatus.SOON }
        )
    }
        .flowOn(Dispatchers.Default)
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    val expiredProducts = splitProducts.map { it.expired }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<Product>())

    val soonProducts = splitProducts.map { it.soon }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<Product>())

    fun setEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setNotifEnabled(enabled) }
    fun setDays(days: Int) = viewModelScope.launch { prefs.setNotifDays(days) }
    fun markPermissionRequested() = viewModelScope.launch {
        prefs.setNotifPermissionRequested(true)
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
}

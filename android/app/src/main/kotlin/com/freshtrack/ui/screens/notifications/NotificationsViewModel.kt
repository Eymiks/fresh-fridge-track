package com.freshtrack.ui.screens.notifications

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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationSettings(
    val enabled: Boolean = true,
    val days: Int = 3
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val prefs: AppPreferences,
    authRepository: AuthRepository,
    productRepository: ProductRepository
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

    val expiredProducts = products.map { list ->
        list.filter { it.isActive() && it.getExpirationStatus() == ExpirationStatus.EXPIRED }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<Product>())

    val soonProducts = products.map { list ->
        list.filter { it.isActive() && it.getExpirationStatus() == ExpirationStatus.SOON }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<Product>())

    fun setEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setNotifEnabled(enabled) }
    fun setDays(days: Int) = viewModelScope.launch { prefs.setNotifDays(days) }
    fun markPermissionRequested() = viewModelScope.launch {
        prefs.setNotifPermissionRequested(true)
    }
}

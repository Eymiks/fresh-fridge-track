package com.freshtrack.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshtrack.data.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val prefs: AppPreferences
) : ViewModel() {

    val appearance = combine(
        combine(prefs.themeMode, prefs.accentColor, prefs.density, prefs.reduceMotion) { theme, accent, density, reduce ->
            AppearanceState(
                themeMode = ThemeMode.entries.firstOrNull { it.name.lowercase() == theme } ?: ThemeMode.SYSTEM,
                accentColor = AccentColor.fromKey(accent),
                density = Density.fromKey(density),
                reduceMotion = reduce
            )
        },
        prefs.hamburgerSide
    ) { state, hSide ->
        state.copy(hamburgerSide = HamburgerSide.fromKey(hSide))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceState())

    val notifEnabled = prefs.notifEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val notifDays = prefs.notifDays
        .stateIn(viewModelScope, SharingStarted.Eagerly, 3)
    val notifPermissionRequested = prefs.notifPermissionRequested
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        prefs.setThemeMode(mode.name.lowercase())
    }
    fun setAccentColor(color: AccentColor) = viewModelScope.launch {
        prefs.setAccentColor(color.key)
    }
    fun setDensity(density: Density) = viewModelScope.launch {
        prefs.setDensity(density.key)
    }
    fun setReduceMotion(reduce: Boolean) = viewModelScope.launch {
        prefs.setReduceMotion(reduce)
    }
    fun setHamburgerSide(side: HamburgerSide) = viewModelScope.launch {
        prefs.setHamburgerSide(side.key)
    }
    fun setNotifEnabled(value: Boolean) = viewModelScope.launch {
        prefs.setNotifEnabled(value)
    }
    fun setNotifDays(value: Int) = viewModelScope.launch {
        prefs.setNotifDays(value)
    }
    fun markNotifPermissionRequested() = viewModelScope.launch {
        prefs.setNotifPermissionRequested(true)
    }
}

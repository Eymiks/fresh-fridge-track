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
        prefs.themeMode,
        prefs.accentColor,
        prefs.density,
        prefs.reduceMotion
    ) { theme, accent, density, reduce ->
        AppearanceState(
            themeMode = ThemeMode.entries.firstOrNull { it.name.lowercase() == theme } ?: ThemeMode.SYSTEM,
            accentColor = AccentColor.fromKey(accent),
            density = Density.fromKey(density),
            reduceMotion = reduce
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceState())

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
}

package com.freshtrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshtrack.ui.theme.AppearanceViewModel

private fun lightSchemeForAccent(accent: AccentColor) = when (accent) {
    AccentColor.GREEN -> lightColorScheme(
        primary = GreenPrimary, onPrimary = GreenOnPrimary,
        primaryContainer = GreenPrimaryContainer, onPrimaryContainer = GreenOnPrimaryContainer
    )
    AccentColor.BLUE -> lightColorScheme(
        primary = BluePrimary, onPrimary = BlueOnPrimary,
        primaryContainer = BluePrimaryContainer, onPrimaryContainer = BlueOnPrimaryContainer
    )
    AccentColor.VIOLET -> lightColorScheme(
        primary = VioletPrimary, onPrimary = VioletOnPrimary,
        primaryContainer = VioletPrimaryContainer, onPrimaryContainer = VioletOnPrimaryContainer
    )
    AccentColor.ORANGE -> lightColorScheme(
        primary = OrangePrimary, onPrimary = OrangeOnPrimary,
        primaryContainer = OrangePrimaryContainer, onPrimaryContainer = OrangeOnPrimaryContainer
    )
    AccentColor.ROSE -> lightColorScheme(
        primary = RosePrimary, onPrimary = RoseOnPrimary,
        primaryContainer = RosePrimaryContainer, onPrimaryContainer = RoseOnPrimaryContainer
    )
    AccentColor.CYAN -> lightColorScheme(
        primary = CyanPrimary, onPrimary = CyanOnPrimary,
        primaryContainer = CyanPrimaryContainer, onPrimaryContainer = CyanOnPrimaryContainer
    )
}

private fun darkSchemeForAccent(accent: AccentColor) = when (accent) {
    AccentColor.GREEN -> darkColorScheme(
        primary = GreenPrimaryContainer, onPrimary = GreenOnPrimaryContainer,
        primaryContainer = GreenOnPrimaryContainer, onPrimaryContainer = GreenPrimaryContainer
    )
    AccentColor.BLUE -> darkColorScheme(
        primary = BluePrimaryContainer, onPrimary = BlueOnPrimaryContainer,
        primaryContainer = BlueOnPrimaryContainer, onPrimaryContainer = BluePrimaryContainer
    )
    AccentColor.VIOLET -> darkColorScheme(
        primary = VioletPrimaryContainer, onPrimary = VioletOnPrimaryContainer,
        primaryContainer = VioletOnPrimaryContainer, onPrimaryContainer = VioletPrimaryContainer
    )
    AccentColor.ORANGE -> darkColorScheme(
        primary = OrangePrimaryContainer, onPrimary = OrangeOnPrimaryContainer,
        primaryContainer = OrangeOnPrimaryContainer, onPrimaryContainer = OrangePrimaryContainer
    )
    AccentColor.ROSE -> darkColorScheme(
        primary = RosePrimaryContainer, onPrimary = RoseOnPrimaryContainer,
        primaryContainer = RoseOnPrimaryContainer, onPrimaryContainer = RosePrimaryContainer
    )
    AccentColor.CYAN -> darkColorScheme(
        primary = CyanPrimaryContainer, onPrimary = CyanOnPrimaryContainer,
        primaryContainer = CyanOnPrimaryContainer, onPrimaryContainer = CyanPrimaryContainer
    )
}

@Composable
fun FreshTrackTheme(
    appearanceVm: AppearanceViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val appearance by appearanceVm.appearance.collectAsState()

    val useDark = when (appearance.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = remember(appearance.accentColor, useDark) {
        if (useDark) darkSchemeForAccent(appearance.accentColor)
        else lightSchemeForAccent(appearance.accentColor)
    }

    CompositionLocalProvider(LocalAppearance provides appearance) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

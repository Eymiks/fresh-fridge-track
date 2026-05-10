package com.freshtrack.ui.theme

import androidx.compose.runtime.compositionLocalOf

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AccentColor(val key: String, val label: String) {
    GREEN("green", "Vert"),
    BLUE("blue", "Bleu"),
    VIOLET("violet", "Violet"),
    ORANGE("orange", "Orange"),
    ROSE("rose", "Rose"),
    CYAN("cyan", "Cyan");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: GREEN
    }
}

enum class Density(val key: String, val label: String) {
    COMPACT("compact", "Compact"),
    NORMAL("normal", "Normal"),
    SPACIOUS("spacious", "Aéré");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: NORMAL
    }
}

enum class HamburgerSide(val key: String) {
    LEFT("left"), RIGHT("right");
    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: RIGHT
    }
}

data class AppearanceState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.GREEN,
    val density: Density = Density.NORMAL,
    val reduceMotion: Boolean = false,
    val hamburgerSide: HamburgerSide = HamburgerSide.RIGHT
)

val LocalAppearance = compositionLocalOf { AppearanceState() }

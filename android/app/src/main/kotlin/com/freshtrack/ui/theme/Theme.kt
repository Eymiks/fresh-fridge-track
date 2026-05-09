package com.freshtrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshtrack.R

private val FreshTrackShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val NunitoFontFamily = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold)
)

private fun freshTextStyle(
    fontSize: Int,
    lineHeight: Int,
    fontWeight: FontWeight = FontWeight.Normal
): TextStyle = TextStyle(
    fontFamily = NunitoFontFamily,
    fontWeight = fontWeight,
    fontSize = fontSize.sp,
    lineHeight = lineHeight.sp
)

object FreshTextStyles {
    val ProductTitle = freshTextStyle(14, 17, FontWeight.Bold)
    val ProductSubtitle = freshTextStyle(12, 15, FontWeight.Medium)
    val ProductMeta = freshTextStyle(10, 12, FontWeight.Bold)
    val ProductBadge = freshTextStyle(10, 12, FontWeight.ExtraBold)
    val ProductTinyBadge = freshTextStyle(9, 11, FontWeight.ExtraBold)
    val SectionHeader = freshTextStyle(12, 14, FontWeight.ExtraBold)
    val SectionCount = freshTextStyle(12, 14, FontWeight.Bold)
    val StatCount = freshTextStyle(16, 18, FontWeight.ExtraBold)
    val StatLabel = freshTextStyle(10, 12, FontWeight.Bold)
    val NavigationLabel = freshTextStyle(11, 13, FontWeight.Bold)
    val NavigationLabelSelected = freshTextStyle(11, 13, FontWeight.ExtraBold)
    val ButtonLabel = freshTextStyle(14, 18, FontWeight.ExtraBold)
    val FormSectionTitle = freshTextStyle(14, 18, FontWeight.ExtraBold)
    val FormHelper = freshTextStyle(12, 16, FontWeight.Medium)
    val MenuItem = freshTextStyle(13, 17, FontWeight.Bold)
    val ActionLabel = freshTextStyle(12, 15, FontWeight.ExtraBold)
}

private val FreshTrackTypography = Typography(
    displayLarge = freshTextStyle(40, 46, FontWeight.ExtraBold),
    displayMedium = freshTextStyle(34, 40, FontWeight.ExtraBold),
    displaySmall = freshTextStyle(28, 34, FontWeight.ExtraBold),
    headlineLarge = freshTextStyle(24, 30, FontWeight.ExtraBold),
    headlineMedium = freshTextStyle(22, 28, FontWeight.ExtraBold),
    headlineSmall = freshTextStyle(20, 25, FontWeight.ExtraBold),
    titleLarge = freshTextStyle(20, 24, FontWeight.ExtraBold),
    titleMedium = freshTextStyle(18, 22, FontWeight.Bold),
    titleSmall = freshTextStyle(14, 18, FontWeight.Bold),
    bodyLarge = freshTextStyle(16, 22, FontWeight.Medium),
    bodyMedium = freshTextStyle(14, 20, FontWeight.Medium),
    bodySmall = freshTextStyle(12, 16, FontWeight.Medium),
    labelLarge = freshTextStyle(14, 18, FontWeight.ExtraBold),
    labelMedium = freshTextStyle(12, 16, FontWeight.Bold),
    labelSmall = freshTextStyle(10, 12, FontWeight.Bold)
)

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
}.copy(
    background = FreshBackgroundLight,
    onBackground = FreshForegroundLight,
    surface = FreshCardLight,
    onSurface = FreshForegroundLight,
    surfaceVariant = FreshMutedLight,
    onSurfaceVariant = FreshForegroundLight.copy(alpha = 0.62f),
    surfaceContainerLowest = FreshCardLight,
    surfaceContainerLow = FreshCardLight,
    surfaceContainer = FreshMutedLight,
    surfaceContainerHigh = FreshMutedLight,
    outline = FreshBorderLight,
    outlineVariant = FreshBorderLight,
    error = ColorExpired,
    onError = GreenOnPrimary,
    errorContainer = ColorExpired.copy(alpha = 0.10f),
    onErrorContainer = ColorExpired,
    secondary = FreshMutedLight,
    onSecondary = FreshForegroundLight,
    secondaryContainer = FreshMutedLight,
    onSecondaryContainer = FreshForegroundLight,
    tertiary = BluePrimary,
    onTertiary = BlueOnPrimary,
    tertiaryContainer = BluePrimaryContainer,
    onTertiaryContainer = BlueOnPrimaryContainer
)

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
}.copy(
    background = FreshBackgroundDark,
    onBackground = FreshForegroundDark,
    surface = FreshCardDark,
    onSurface = FreshForegroundDark,
    surfaceVariant = FreshMutedDark,
    onSurfaceVariant = FreshForegroundDark.copy(alpha = 0.62f),
    surfaceContainerLowest = FreshBackgroundDark,
    surfaceContainerLow = FreshCardDark,
    surfaceContainer = FreshMutedDark,
    surfaceContainerHigh = FreshMutedDark,
    outline = FreshBorderDark,
    outlineVariant = FreshBorderDark,
    error = ColorExpired,
    onError = GreenOnPrimary,
    errorContainer = ColorExpired.copy(alpha = 0.18f),
    onErrorContainer = Color(0xFFFFD9D9),
    secondary = FreshMutedDark,
    onSecondary = FreshForegroundDark,
    secondaryContainer = FreshMutedDark,
    onSecondaryContainer = FreshForegroundDark,
    tertiary = BluePrimaryContainer,
    onTertiary = BlueOnPrimaryContainer,
    tertiaryContainer = BlueOnPrimaryContainer,
    onTertiaryContainer = BluePrimaryContainer
)

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
            shapes = FreshTrackShapes,
            typography = FreshTrackTypography,
            content = content
        )
    }
}

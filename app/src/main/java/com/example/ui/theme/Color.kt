package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Brand palette (sky blue seed). Used when dynamic color is unavailable (< Android 12)
 * or switched off. On Android 12+ the theme follows the user's wallpaper colors.
 */
internal val VaultLightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF00668B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC6E7FF),
    onPrimaryContainer = Color(0xFF001E2E),
    inversePrimary = Color(0xFF81CFFF),
    secondary = Color(0xFF4F616E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E5F5),
    onSecondaryContainer = Color(0xFF0B1D29),
    tertiary = Color(0xFF65587B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEBDCFF),
    onTertiaryContainer = Color(0xFF201634),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6FAFE),
    onBackground = Color(0xFF171C20),
    surface = Color(0xFFF6FAFE),
    onSurface = Color(0xFF171C20),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41484D),
    outline = Color(0xFF71787E),
    outlineVariant = Color(0xFFC1C7CE),
    inverseSurface = Color(0xFF2C3135),
    inverseOnSurface = Color(0xFFEDF1F6),
    surfaceDim = Color(0xFFD6DADF),
    surfaceBright = Color(0xFFF6FAFE),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F4F8),
    surfaceContainer = Color(0xFFEAEEF2),
    surfaceContainerHigh = Color(0xFFE4E9ED),
    surfaceContainerHighest = Color(0xFFDFE3E7),
)

internal val VaultDarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF81CFFF),
    onPrimary = Color(0xFF00344C),
    primaryContainer = Color(0xFF004C6C),
    onPrimaryContainer = Color(0xFFC6E7FF),
    inversePrimary = Color(0xFF006590),
    secondary = Color(0xFFB7C9D9),
    onSecondary = Color(0xFF21323E),
    secondaryContainer = Color(0xFF384956),
    onSecondaryContainer = Color(0xFFD2E5F5),
    tertiary = Color(0xFFCFBFE7),
    onTertiary = Color(0xFF352B4A),
    tertiaryContainer = Color(0xFF4C4162),
    onTertiaryContainer = Color(0xFFEBDCFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1418),
    onBackground = Color(0xFFDFE3E7),
    surface = Color(0xFF0F1418),
    onSurface = Color(0xFFDFE3E7),
    surfaceVariant = Color(0xFF41484D),
    onSurfaceVariant = Color(0xFFC1C7CE),
    outline = Color(0xFF8B9198),
    outlineVariant = Color(0xFF41484D),
    inverseSurface = Color(0xFFDFE3E7),
    inverseOnSurface = Color(0xFF2C3135),
    surfaceDim = Color(0xFF0F1418),
    surfaceBright = Color(0xFF353A3E),
    surfaceContainerLowest = Color(0xFF0A0F12),
    surfaceContainerLow = Color(0xFF171C20),
    surfaceContainer = Color(0xFF1B2024),
    surfaceContainerHigh = Color(0xFF262B2E),
    surfaceContainerHighest = Color(0xFF303539),
)

/**
 * Semantic colors the M3 ColorScheme has no slot for (success / warning).
 * They stay fixed so "green = verified" and "amber = caution" survive dynamic color.
 */
@Immutable
class VaultExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

internal val VaultExtendedLight = VaultExtendedColors(
    success = Color(0xFF1F6B3A),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFB3F2C5),
    onSuccessContainer = Color(0xFF00210E),
    warning = Color(0xFF8B5000),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFDCBE),
    onWarningContainer = Color(0xFF2C1600),
)

internal val VaultExtendedDark = VaultExtendedColors(
    success = Color(0xFF8AD6A1),
    onSuccess = Color(0xFF00391B),
    successContainer = Color(0xFF0C5129),
    onSuccessContainer = Color(0xFFB3F2C5),
    warning = Color(0xFFFFB870),
    onWarning = Color(0xFF4A2800),
    warningContainer = Color(0xFF693C00),
    onWarningContainer = Color(0xFFFFDCBE),
)

val LocalVaultColors = staticCompositionLocalOf { VaultExtendedLight }

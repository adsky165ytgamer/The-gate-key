package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VaultColorScheme = darkColorScheme(
    primary = VaultPrimary,
    onPrimary = VaultOnPrimary,
    primaryContainer = VaultPrimaryContainer,
    onPrimaryContainer = VaultOnPrimaryContainer,
    secondary = VaultSecondary,
    onSecondary = VaultOnSecondary,
    background = VaultBackground,
    onBackground = VaultTextPrimary,
    surface = VaultSurface,
    onSurface = VaultTextPrimary,
    surfaceVariant = VaultSurfaceVariant,
    onSurfaceVariant = VaultTextSecondary,
    outline = VaultBorder,
    error = VaultAccentRose,
    onError = VaultTextPrimary
)

@Composable
fun ADSkyVaultTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VaultColorScheme,
        typography = Typography,
        content = content
    )
}

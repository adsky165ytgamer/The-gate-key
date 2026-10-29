package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * All sizes are sp, so every style scales with the system font size.
 * Expressive type = the same scale, but with heavier "emphasized" variants for titles and labels.
 */
val VaultTypography = Typography()

/** Expressive emphasized variant of any style. */
fun TextStyle.emphasized(weight: FontWeight = FontWeight.SemiBold): TextStyle = copy(fontWeight = weight)

/** Monospace variant for keys, fingerprints, sizes. */
fun TextStyle.mono(): TextStyle = copy(fontFamily = FontFamily.Monospace)

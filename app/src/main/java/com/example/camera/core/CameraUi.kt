package com.example.camera.core

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/** Round control that floats over a camera preview. 48dp touch target. */
@Composable
fun OverlayToggleButton(
    icon: ImageVector,
    contentDescription: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    FilledTonalIconToggleButton(
        checked = checked,
        onCheckedChange = {
            haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            onCheckedChange(it)
        },
        enabled = enabled,
        modifier = modifier.size(48.dp),
        colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
            containerColor = Color.Black.copy(alpha = 0.45f),
            contentColor = Color.White,
            checkedContainerColor = MaterialTheme.colorScheme.primary,
            checkedContentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(24.dp))
    }
}

/** Non-toggle variant. */
@Composable
fun OverlayButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OverlayToggleButton(icon, contentDescription, checked = false, onCheckedChange = { onClick() }, modifier = modifier)
}

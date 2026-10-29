package com.example.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Expressive motion specs for AD Sky Vault.
 * Provides physical spring animations and fast easing curves for seamless UI responses.
 */
object VaultMotion {
    fun <T> defaultSpatialSpec(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> fastSpatialSpec(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = tween(
        durationMillis = 280,
        easing = FastOutSlowInEasing
    )

    fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = tween(
        durationMillis = 150,
        easing = LinearOutSlowInEasing
    )
}

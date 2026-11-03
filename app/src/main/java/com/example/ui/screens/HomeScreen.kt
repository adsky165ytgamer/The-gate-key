@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.VaultScreen
import com.example.ui.VaultViewModel
import com.example.ui.components.ShapeBadge
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.VaultBadgeShape
import com.example.ui.theme.VaultMotion
import com.example.ui.theme.emphasized

/**
 * Minimal home: one prominent "Security & Encryption" container, nothing else
 * (plus a quiet About link). No bottom navigation, no category grid, no stats.
 */
@Composable
fun HomeScreen(
    viewModel: VaultViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    // Very large system fonts: cards stack their content instead of squeezing three columns.
    val stacked = LocalDensity.current.fontScale > 1.3f

    BoxWithConstraints(modifier.fillMaxSize()) {
        val minHeight = (maxHeight - contentPadding.calculateTopPadding() - contentPadding.calculateBottomPadding())
            .coerceAtLeast(0.dp)

        // Scrolls only when it must (huge fonts / landscape); otherwise the cards sit centered.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .heightIn(min = minHeight),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HomeFeatureCard(
                title = "Security & Encryption",
                description = "Protect files and manage your security tools.",
                icon = Icons.Rounded.Shield,
                shape = VaultBadgeShape.Cookie12Sided,
                container = MaterialTheme.colorScheme.primaryContainer,
                onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                accent = MaterialTheme.colorScheme.primary,
                onAccent = MaterialTheme.colorScheme.onPrimary,
                stacked = stacked,
                index = 0,
                outerTag = "hero_vault_card",
                tag = "security_and_encryption_card",
                onClick = { viewModel.navigateTo(VaultScreen.SECURITY_AND_ENCRYPTION) }
            )
            HomeFeatureCard(
                title = "Camera Tools",
                description = "Scan documents, read codes, magnify and inspect images.",
                icon = Icons.Rounded.CameraAlt,
                shape = VaultBadgeShape.Sunny,
                container = MaterialTheme.colorScheme.tertiaryContainer,
                onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
                accent = MaterialTheme.colorScheme.tertiary,
                onAccent = MaterialTheme.colorScheme.onTertiary,
                stacked = stacked,
                index = 1,
                outerTag = "camera_tools_card_outer",
                tag = "camera_tools_card",
                onClick = { viewModel.navigateTo(VaultScreen.CAMERA_TOOLS) }
            )

            HomeFeatureCard(
                title = "Location Kit",
                description = "Explore the map, check your position, find north and measure distances.",
                icon = Icons.Rounded.Explore,
                shape = VaultBadgeShape.Clover4Leaf,
                container = MaterialTheme.colorScheme.secondaryContainer,
                onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
                accent = MaterialTheme.colorScheme.secondary,
                onAccent = MaterialTheme.colorScheme.onSecondary,
                stacked = stacked,
                index = 2,
                outerTag = "location_kit_card_outer",
                tag = "location_kit_card",
                onClick = { viewModel.navigateTo(VaultScreen.LOCATION_KIT) }
            )

            TextButton(
                onClick = { viewModel.navigateTo(VaultScreen.ABOUT) },
                modifier = Modifier.padding(top = 8.dp).heightIn(min = 48.dp).staggeredEntrance(3).testTag("nav_about_button")
            ) {
                Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("About AD Sky Vault", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HomeFeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    shape: VaultBadgeShape,
    container: Color,
    onContainer: Color,
    accent: Color,
    onAccent: Color,
    stacked: Boolean,
    index: Int,
    outerTag: String,
    tag: String,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // Press feedback: the card squishes and its corners tighten (expressive spatial spring).
    val corner by animateDpAsState(if (pressed) 24.dp else 40.dp, VaultMotion.fastSpatialSpec(), label = "home-card-corner")
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, VaultMotion.fastSpatialSpec(), label = "home-card-scale")

    Box(Modifier.testTag(outerTag)) {
        Surface(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            interactionSource = interaction,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .staggeredEntrance(index)
                .testTag(tag),
            shape = RoundedCornerShape(corner.coerceAtLeast(0.dp)),
            color = container,
            contentColor = onContainer,
            shadowElevation = 2.dp
        ) {
            val badge: @Composable () -> Unit = {
                ShapeBadge(icon = icon, polygon = shape, size = 64.dp, containerColor = accent, contentColor = onAccent)
            }
            val arrow: @Composable () -> Unit = {
                Box(Modifier.size(48.dp).background(accent, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = onAccent, modifier = Modifier.size(24.dp))
                }
            }
            val texts: @Composable (Modifier) -> Unit = { m ->
                Column(m) {
                    Text(title, style = MaterialTheme.typography.headlineSmall.emphasized(FontWeight.Bold))
                    Spacer(Modifier.size(6.dp))
                    Text(description, style = MaterialTheme.typography.bodyMedium, color = onContainer.copy(alpha = 0.84f))
                }
            }
            if (stacked) {
                Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        badge(); arrow()
                    }
                    texts(Modifier.fillMaxWidth())
                }
            } else {
                Row(Modifier.padding(28.dp), verticalAlignment = Alignment.CenterVertically) {
                    badge()
                    Spacer(Modifier.width(20.dp))
                    texts(Modifier.weight(1f))
                    Spacer(Modifier.width(16.dp))
                    arrow()
                }
            }
        }
    }
}

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Shield
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
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // Press feedback: the card squishes and its corners tighten (expressive spatial spring).
    val corner by animateDpAsState(if (pressed) 24.dp else 40.dp, VaultMotion.fastSpatialSpec(), label = "home-card-corner")
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, VaultMotion.fastSpatialSpec(), label = "home-card-scale")

    // Very large system fonts: stack the content instead of squeezing three columns.
    val stacked = LocalDensity.current.fontScale > 1.3f

    BoxWithConstraints(modifier.fillMaxSize()) {
        val minHeight = (maxHeight - contentPadding.calculateTopPadding() - contentPadding.calculateBottomPadding())
            .coerceAtLeast(0.dp)

        // Scrolls only when it must (huge fonts / landscape); otherwise the card sits centered.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .heightIn(min = minHeight),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.testTag("hero_vault_card")) {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.navigateTo(VaultScreen.SECURITY_AND_ENCRYPTION)
                    },
                    interactionSource = interaction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .staggeredEntrance(0)
                        .testTag("security_and_encryption_card"),
                    shape = RoundedCornerShape(corner.coerceAtLeast(0.dp)),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shadowElevation = 2.dp
                ) {
                    val badge: @Composable () -> Unit = {
                        ShapeBadge(
                            icon = Icons.Rounded.Shield,
                            polygon = VaultBadgeShape.Cookie12Sided,
                            size = 64.dp
                        )
                    }
                    val arrow: @Composable () -> Unit = {
                        Box(
                            Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    val texts: @Composable (Modifier) -> Unit = { m ->
                        Column(m) {
                            Text(
                                "Security & Encryption",
                                style = MaterialTheme.typography.headlineSmall.emphasized(FontWeight.Bold)
                            )
                            Spacer(Modifier.size(6.dp))
                            Text(
                                "Protect files and manage your security tools.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.84f)
                            )
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

            TextButton(
                onClick = { viewModel.navigateTo(VaultScreen.ABOUT) },
                modifier = Modifier.padding(top = 24.dp).heightIn(min = 48.dp).staggeredEntrance(1).testTag("nav_about_button")
            ) {
                Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("About AD Sky Vault", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

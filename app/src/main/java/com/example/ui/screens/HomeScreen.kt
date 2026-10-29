@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PublicOff
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.VaultScreen
import com.example.ui.VaultViewModel
import com.example.ui.components.AuditLogCard
import com.example.ui.components.EmptyState
import com.example.ui.components.ExpressiveTile
import com.example.ui.components.HardwareSecurityPill
import com.example.ui.components.InfoChip
import com.example.ui.components.SectionHeader
import com.example.ui.components.ShapeBadge
import com.example.ui.components.connectedShape
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.VaultBadgeShape
import com.example.ui.theme.VaultTheme
import com.example.ui.theme.emphasized

private enum class TileStyle { Primary, Success, Tertiary, Warning, Secondary, Neutral }

private class TileColors(val container: Color, val content: Color, val badge: Color, val onBadge: Color)

@Composable
private fun TileStyle.colors(): TileColors {
    val s = MaterialTheme.colorScheme
    val e = VaultTheme.colors
    return when (this) {
        TileStyle.Primary -> TileColors(s.primaryContainer, s.onPrimaryContainer, s.primary, s.onPrimary)
        TileStyle.Success -> TileColors(e.successContainer, e.onSuccessContainer, e.success, e.onSuccess)
        TileStyle.Tertiary -> TileColors(s.tertiaryContainer, s.onTertiaryContainer, s.tertiary, s.onTertiary)
        TileStyle.Warning -> TileColors(e.warningContainer, e.onWarningContainer, e.warning, e.onWarning)
        TileStyle.Secondary -> TileColors(s.secondaryContainer, s.onSecondaryContainer, s.secondary, s.onSecondary)
        TileStyle.Neutral -> TileColors(s.surfaceContainerHigh, s.onSurface, s.onSurfaceVariant, s.surfaceContainerHigh)
    }
}

private class HomeTile(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val style: TileStyle,
    val polygon: VaultBadgeShape,
    val target: VaultScreen,
    val tag: String
)

private val homeTiles = listOf(
    HomeTile("Encrypt file", "Stream into .adsky", Icons.Rounded.Lock, TileStyle.Primary, VaultBadgeShape.Cookie9Sided, VaultScreen.ENCRYPT, "nav_encrypt_button"),
    HomeTile("Decrypt file", "Unwrap and verify", Icons.Rounded.LockOpen, TileStyle.Success, VaultBadgeShape.Sunny, VaultScreen.DECRYPT, "nav_decrypt_button"),
    HomeTile("Batch queue", "Many files at once", Icons.Rounded.FolderZip, TileStyle.Tertiary, VaultBadgeShape.Clover4Leaf, VaultScreen.BATCH, "nav_batch_button"),
    HomeTile("Recovery secret", "Master cross-device key", Icons.Rounded.Key, TileStyle.Warning, VaultBadgeShape.Gem, VaultScreen.RECOVERY_KEY, "nav_recovery_button"),
    HomeTile("Device & security", "Keystore and audit log", Icons.Rounded.Security, TileStyle.Secondary, VaultBadgeShape.SoftBurst, VaultScreen.DEVICE_SECURITY, "nav_device_security_button"),
    HomeTile("About", "Architecture and spec", Icons.Rounded.Info, TileStyle.Neutral, VaultBadgeShape.Cookie12Sided, VaultScreen.ABOUT, "nav_about_button"),
)

@Composable
fun HomeScreen(
    viewModel: VaultViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val hardwareInfo by viewModel.hardwareSecurity.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()

    // Dynamic density & font scale for adaptive column sizing
    val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 2f)
    val recent = auditLogs.take(3)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp * fontScale),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggeredEntrance(0)
                    .testTag("hero_vault_card"),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Column(Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ShapeBadge(
                            icon = Icons.Rounded.Shield,
                            polygon = VaultBadgeShape.Cookie12Sided,
                            size = 72.dp,
                            rotate = true,
                            contentDescription = "Vault shield"
                        )
                        Spacer(Modifier.width(18.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Sealed on this device. Nowhere else.",
                                style = MaterialTheme.typography.titleLarge.emphasized(FontWeight.Bold),
                                maxLines = 2
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                "Authenticated .adsky containers backed by Android Keystore hardware keys.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                    Spacer(Modifier.size(16.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoChip(
                            text = "100% offline",
                            leading = Icons.Rounded.PublicOff,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                        HardwareSecurityPill(
                            info = hardwareInfo,
                            onClick = { viewModel.navigateTo(VaultScreen.DEVICE_SECURITY) }
                        )
                    }
                }
            }
        }

        item(key = "ops-header", span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader("Cryptographic operations")
        }

        itemsIndexed(homeTiles, key = { _, tile -> tile.tag }) { index, tile ->
            val c = tile.style.colors()
            ExpressiveTile(
                title = tile.title,
                subtitle = tile.subtitle,
                icon = tile.icon,
                containerColor = c.container,
                contentColor = c.content,
                badgeColor = c.badge,
                onBadgeColor = c.onBadge,
                badgePolygon = tile.polygon,
                modifier = Modifier.staggeredEntrance(index + 1),
                testTag = tile.tag,
                onClick = { viewModel.navigateTo(tile.target) }
            )
        }

        item(key = "audit-header", span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader("Recent activity", modifier = Modifier.weight(1f))
                TextButton(onClick = { viewModel.navigateTo(VaultScreen.DEVICE_SECURITY) }) {
                    Text("View all (${auditLogs.size})")
                }
            }
        }

        item(key = "audit-list", span = { GridItemSpan(maxLineSpan) }) {
            if (recent.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.History,
                    title = "No activity yet",
                    message = "Encryption and access attempts are recorded here automatically."
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    recent.forEachIndexed { i, log ->
                        AuditLogCard(log = log, shape = connectedShape(i, recent.size))
                    }
                }
            }
        }
    }
}

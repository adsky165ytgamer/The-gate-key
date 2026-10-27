package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.VaultScreen
import com.example.ui.VaultViewModel
import com.example.ui.components.AuditLogCard
import com.example.ui.components.HardwareSecurityPill
import com.example.ui.components.bounceClick
import com.example.ui.theme.VaultAccentAmber
import com.example.ui.theme.VaultAccentGreen
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultCard
import com.example.ui.theme.VaultPrimary
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceVariant
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

@Composable
fun HomeScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val hardwareInfo by viewModel.hardwareSecurity.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Hero Vault Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_vault_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VaultSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFF0369A1),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "100% OFFLINE AIR-GAPPED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                letterSpacing = 0.5.sp
                            )
                        }
                        HardwareSecurityPill(
                            info = hardwareInfo,
                            onClick = { viewModel.navigateTo(VaultScreen.DEVICE_SECURITY) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF082F49), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Vault Shield",
                                tint = VaultPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "AD Sky Vault",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = VaultTextPrimary
                            )
                            Text(
                                text = "Envelope Encryption & Key Recovery",
                                fontSize = 13.sp,
                                color = VaultTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Encrypted binary container system (.adsky) backed by Android Keystore, AES-256-GCM, XChaCha20-Poly1305, and Argon2id KDF.",
                        fontSize = 12.sp,
                        color = VaultTextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        item {
            Text(
                text = "Cryptographic Operations",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = VaultPrimary,
                letterSpacing = 0.5.sp
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard(
                    title = "Encrypt File",
                    subtitle = "Stream to .adsky format",
                    icon = Icons.Default.Lock,
                    iconTint = VaultPrimary,
                    modifier = Modifier.weight(1f),
                    testTag = "nav_encrypt_button",
                    onClick = { viewModel.navigateTo(VaultScreen.ENCRYPT) }
                )
                ActionCard(
                    title = "Decrypt File",
                    subtitle = "Unwrap & verify container",
                    icon = Icons.Default.LockOpen,
                    iconTint = VaultAccentGreen,
                    modifier = Modifier.weight(1f),
                    testTag = "nav_decrypt_button",
                    onClick = { viewModel.navigateTo(VaultScreen.DECRYPT) }
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard(
                    title = "Batch Processing",
                    subtitle = "Multi-file queue processing",
                    icon = Icons.Default.FolderZip,
                    iconTint = Color(0xFFA855F7),
                    modifier = Modifier.weight(1f),
                    testTag = "nav_batch_button",
                    onClick = { viewModel.navigateTo(VaultScreen.BATCH) }
                )
                ActionCard(
                    title = "Recovery Secret",
                    subtitle = "Master cross-device key",
                    icon = Icons.Default.Key,
                    iconTint = VaultAccentAmber,
                    modifier = Modifier.weight(1f),
                    testTag = "nav_recovery_button",
                    onClick = { viewModel.navigateTo(VaultScreen.RECOVERY_KEY) }
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard(
                    title = "Device & Security",
                    subtitle = "Keystore, TEE & audit logs",
                    icon = Icons.Default.Security,
                    iconTint = VaultPrimary,
                    modifier = Modifier.weight(1f),
                    testTag = "nav_device_security_button",
                    onClick = { viewModel.navigateTo(VaultScreen.DEVICE_SECURITY) }
                )
                ActionCard(
                    title = "About AD Sky",
                    subtitle = "Architecture & container spec",
                    icon = Icons.Default.Info,
                    iconTint = VaultTextSecondary,
                    modifier = Modifier.weight(1f),
                    testTag = "nav_about_button",
                    onClick = { viewModel.navigateTo(VaultScreen.ABOUT) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Security Audit Activity",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VaultPrimary
                )
                Text(
                    text = "View All (${auditLogs.size})",
                    fontSize = 12.sp,
                    color = VaultTextSecondary,
                    modifier = Modifier.clickable { viewModel.navigateTo(VaultScreen.DEVICE_SECURITY) }
                )
            }
        }

        if (auditLogs.isEmpty()) {
            item {
                Surface(
                    color = VaultCard,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = VaultTextSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No audit log entries yet",
                            color = VaultTextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Encryption and access attempts will be recorded here automatically.",
                            color = VaultBorder,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            val recentLogs = auditLogs.take(3)
            items(recentLogs.size) { idx ->
                val log = recentLogs[idx]
                AuditLogCard(log = log)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .bounceClick { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = VaultCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(VaultSurfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = VaultTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = VaultTextSecondary,
                lineHeight = 14.sp
            )
        }
    }
}


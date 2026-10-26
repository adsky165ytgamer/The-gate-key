package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.KeystoreManager
import com.example.ui.VaultViewModel
import com.example.ui.components.AuditLogCard
import com.example.ui.theme.VaultAccentAmber
import com.example.ui.theme.VaultAccentGreen
import com.example.ui.theme.VaultAccentRose
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultCard
import com.example.ui.theme.VaultPrimary
import com.example.ui.theme.VaultSurfaceVariant
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

@Composable
fun DeviceSecurityScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val hardwareInfo by viewModel.hardwareSecurity.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }
    var filterErrorsOnly by remember { mutableStateOf(false) }

    val defaultKeyFingerprint = remember {
        try {
            val keyPair = KeystoreManager.getOrCreateSigningKeyPair(isCustom = false)
            KeystoreManager.computePublicKeyFingerprint(keyPair.public.encoded)
        } catch (e: Exception) {
            "Error querying fingerprint: ${e.message}"
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Security Audit Log?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove all local records of cryptographic operations.", color = VaultTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearAuditLogs()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultAccentRose)
                ) {
                    Text("Clear All Logs")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = VaultTextSecondary)
                }
            },
            containerColor = VaultCard,
            shape = RoundedCornerShape(12.dp)
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Device & Security Architecture",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary
            )
            Text(
                text = "Hardware Keymaster integrity, signing identity fingerprints, and access audit log.",
                fontSize = 12.sp,
                color = VaultTextSecondary
            )
        }

        // 1. Hardware Keystore Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hardware_keystore_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (hardwareInfo.isInsideSecureHardware) VaultAccentGreen else VaultAccentAmber,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Android Keystore Keymaster",
                                fontWeight = FontWeight.Bold,
                                color = VaultTextPrimary,
                                fontSize = 14.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = VaultPrimary,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { viewModel.refreshHardwareSecurity() }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = VaultSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Security Level:", fontSize = 12.sp, color = VaultTextSecondary)
                                Text(
                                    text = hardwareInfo.securityLevelName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hardwareInfo.isInsideSecureHardware) VaultAccentGreen else VaultAccentAmber
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Isolated Hardware:", fontSize = 12.sp, color = VaultTextSecondary)
                                Text(
                                    text = if (hardwareInfo.isInsideSecureHardware) "YES (TEE / SE)" else "NO (Software Sandbox)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hardwareInfo.isInsideSecureHardware) VaultAccentGreen else VaultAccentAmber
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Master Alias:", fontSize = 12.sp, color = VaultTextSecondary)
                                Text(
                                    text = KeystoreManager.DEVICE_MASTER_KEY_ALIAS,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = VaultPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = hardwareInfo.description,
                        fontSize = 11.sp,
                        color = VaultTextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // 2. Signing Identity Fingerprints
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signing_identities_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = VaultPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ECDSA P-256 Signing Identities",
                            fontWeight = FontWeight.Bold,
                            color = VaultTextPrimary,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Default Hidden Identity Fingerprint (SHA-256):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VaultTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = VaultSurfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = defaultKeyFingerprint.chunked(4).joinToString(" "),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = VaultPrimary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }

        // 3. Security Audit Logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Security Audit Log (${auditLogs.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = VaultPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (filterErrorsOnly) "Show All" else "Errors Only",
                        fontSize = 11.sp,
                        color = VaultAccentAmber,
                        modifier = Modifier
                            .clickable { filterErrorsOnly = !filterErrorsOnly }
                            .padding(end = 12.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear logs",
                        tint = VaultAccentRose,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { showClearDialog = true }
                    )
                }
            }
        }

        val displayedLogs = if (filterErrorsOnly) {
            auditLogs.filter { it.status != "SUCCESS" }
        } else {
            auditLogs
        }

        if (displayedLogs.isEmpty()) {
            item {
                Surface(
                    color = VaultCard,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (filterErrorsOnly) "No failed access attempts recorded." else "Audit log is empty.",
                        color = VaultTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(displayedLogs.size) { idx ->
                val log = displayedLogs[idx]
                AuditLogCard(log = log)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

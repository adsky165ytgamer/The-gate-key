@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crypto.KeystoreManager
import com.example.data.AuditLogEntry
import com.example.ui.VaultViewModel
import com.example.ui.components.AuditLogCard
import com.example.ui.components.EmptyState
import com.example.ui.components.KeyValueRow
import com.example.ui.components.SectionHeader
import com.example.ui.components.ShapeBadge
import com.example.ui.components.connectedShape
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.VaultBadgeShape
import com.example.ui.theme.VaultTheme
import com.example.ui.theme.emphasized
import com.example.ui.theme.mono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DeviceSecurityScreen(
    viewModel: VaultViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val ext = VaultTheme.colors
    val hardwareInfo by viewModel.hardwareSecurity.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()

    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    var filterErrorsOnly by rememberSaveable { mutableStateOf(false) }
    var refreshTurns by remember { mutableIntStateOf(0) }
    val refreshRotation by animateFloatAsState(refreshTurns * 360f, tween(600), label = "refresh-rotation")

    val fingerprint by produceState<String?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            try {
                val keyPair = KeystoreManager.getOrCreateSigningKeyPair(isCustom = false)
                KeystoreManager.computePublicKeyFingerprint(keyPair.public.encoded)
            } catch (e: Exception) {
                "Error querying fingerprint: ${e.message}"
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
            title = { Text("Clear security audit log?") },
            text = { Text("This permanently removes every local record of cryptographic operations.") },
            confirmButton = {
                TextButton(onClick = { showClearDialog = false; viewModel.clearAuditLogs() }) {
                    Text("Clear all logs", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } }
        )
    }

    val displayedLogs = if (filterErrorsOnly) auditLogs.filter { it.status != "SUCCESS" } else auditLogs

    BoxWithConstraints(modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        if (isWide) {
            // Adaptive 2-column wide layout: Hardware info on left, Audit logs on right
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    KeystoreInfoCard(
                        hardwareInfo = hardwareInfo,
                        ext = ext,
                        refreshRotation = refreshRotation,
                        onRefresh = { refreshTurns++; viewModel.refreshHardwareSecurity() }
                    )

                    SigningIdentityCard(fingerprint = fingerprint)
                }

                Column(
                    Modifier
                        .weight(1.2f)
                        .fillMaxSize()
                ) {
                    AuditLogHeader(
                        totalLogs = auditLogs.size,
                        filterErrorsOnly = filterErrorsOnly,
                        onToggleFilter = { filterErrorsOnly = !filterErrorsOnly },
                        hasLogs = auditLogs.isNotEmpty(),
                        onClearRequest = { showClearDialog = true }
                    )
                    Spacer(Modifier.size(8.dp))
                    if (displayedLogs.isEmpty()) {
                        EmptyState(
                            icon = Icons.Rounded.History,
                            title = if (filterErrorsOnly) "No failed attempts" else "Audit log is empty",
                            message = if (filterErrorsOnly) "No failed access attempts have been recorded." else "Operations will appear here as you use the vault."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            itemsIndexed(displayedLogs, key = { _, log -> log.id }) { index, log ->
                                AuditLogCard(
                                    log = log,
                                    shape = connectedShape(index, displayedLogs.size)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Standard single-column flow on compact phone
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item(key = "keymaster") {
                    KeystoreInfoCard(
                        hardwareInfo = hardwareInfo,
                        ext = ext,
                        refreshRotation = refreshRotation,
                        onRefresh = { refreshTurns++; viewModel.refreshHardwareSecurity() }
                    )
                }

                item(key = "identity") {
                    SigningIdentityCard(fingerprint = fingerprint)
                }

                item(key = "audit-header") {
                    AuditLogHeader(
                        totalLogs = auditLogs.size,
                        filterErrorsOnly = filterErrorsOnly,
                        onToggleFilter = { filterErrorsOnly = !filterErrorsOnly },
                        hasLogs = auditLogs.isNotEmpty(),
                        onClearRequest = { showClearDialog = true }
                    )
                }

                if (displayedLogs.isEmpty()) {
                    item(key = "audit-empty") {
                        EmptyState(
                            icon = Icons.Rounded.History,
                            title = if (filterErrorsOnly) "No failed attempts" else "Audit log is empty",
                            message = if (filterErrorsOnly) "No failed access attempts have been recorded." else "Operations will appear here as you use the vault."
                        )
                    }
                } else {
                    itemsIndexed(displayedLogs, key = { _, log -> log.id }) { index, log ->
                        AuditLogCard(
                            log = log,
                            shape = connectedShape(index, displayedLogs.size)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeystoreInfoCard(
    hardwareInfo: com.example.crypto.HardwareSecurityInfo,
    ext: com.example.ui.theme.VaultExtendedColors,
    refreshRotation: Float,
    onRefresh: () -> Unit
) {
    val secureColor = if (hardwareInfo.isInsideSecureHardware) ext.success else ext.warning
    Surface(
        Modifier
            .fillMaxWidth()
            .staggeredEntrance(0)
            .testTag("hardware_keystore_card"),
        shape = RoundedCornerShape(28.dp),
        color = if (hardwareInfo.isInsideSecureHardware) ext.successContainer else ext.warningContainer,
        contentColor = if (hardwareInfo.isInsideSecureHardware) ext.onSuccessContainer else ext.onWarningContainer
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShapeBadge(
                    Icons.Rounded.Shield,
                    polygon = VaultBadgeShape.Cookie9Sided,
                    size = 56.dp,
                    rotate = true,
                    containerColor = secureColor,
                    contentColor = if (hardwareInfo.isInsideSecureHardware) ext.onSuccess else ext.onWarning
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    "Android Keystore",
                    style = MaterialTheme.typography.titleLarge.emphasized(),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Rounded.Refresh, contentDescription = "Refresh",
                        modifier = Modifier.graphicsLayer { rotationZ = refreshRotation }
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            KeyValueRow("Security level", hardwareInfo.securityLevelName)
            KeyValueRow(
                "Isolated hardware",
                if (hardwareInfo.isInsideSecureHardware) "Yes (TEE / SE)" else "No (software sandbox)"
            )
            KeyValueRow("Master alias", KeystoreManager.DEVICE_MASTER_KEY_ALIAS, monospace = true)
            Text(
                hardwareInfo.description,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SigningIdentityCard(fingerprint: String?) {
    Column(Modifier.staggeredEntrance(1).testTag("signing_identities_card"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Signing identity", subtitle = "ECDSA P-256 · default hidden identity")
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("SHA-256 fingerprint", style = MaterialTheme.typography.labelLarge.emphasized())
                    Text(
                        fingerprint?.chunked(4)?.joinToString(" ") ?: "Computing…",
                        style = MaterialTheme.typography.bodyMedium.mono(),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AuditLogHeader(
    totalLogs: Int,
    filterErrorsOnly: Boolean,
    onToggleFilter: () -> Unit,
    hasLogs: Boolean,
    onClearRequest: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SectionHeader("Audit log ($totalLogs)", modifier = Modifier.weight(1f))
        FilterChip(
            selected = filterErrorsOnly,
            onClick = onToggleFilter,
            label = { Text("Errors only") },
            leadingIcon = if (filterErrorsOnly) {
                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
            } else null
        )
        IconButton(onClick = onClearRequest, enabled = hasLogs) {
            Icon(Icons.Rounded.Delete, contentDescription = "Clear logs")
        }
    }
}

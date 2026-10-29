@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.PublishedWithChanges
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crypto.CipherAlgorithm
import com.example.crypto.ContainerConstants
import com.example.ui.DecryptAuthMode
import com.example.ui.VaultViewModel
import com.example.ui.components.AlgorithmSelectionCard
import com.example.ui.components.BannerKind
import com.example.ui.components.KeyValueRow
import com.example.ui.components.OperationProgressCard
import com.example.ui.components.RadioOptionRow
import com.example.ui.components.ResultCard
import com.example.ui.components.ScreenWithActionBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.SecretTextField
import com.example.ui.components.ShapeBadge
import com.example.ui.components.StatusBanner
import com.example.ui.components.ToggleOptionRow
import com.example.ui.components.VaultButton
import com.example.ui.components.VaultTonalButton
import com.example.ui.components.connectedShape
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.VaultBadgeShape
import com.example.ui.theme.emphasized

@Composable
fun MigrationScreen(
    viewModel: VaultViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val containerUri by viewModel.migrationContainerUri.collectAsStateWithLifecycle()
    val header by viewModel.migrationHeader.collectAsStateWithLifecycle()
    val envelopes by viewModel.migrationEnvelopes.collectAsStateWithLifecycle()
    val authMode by viewModel.migrationAuthMode.collectAsStateWithLifecycle()
    val authCredential by viewModel.migrationAuthCredential.collectAsStateWithLifecycle()
    val authRecoverySecret by viewModel.migrationAuthRecoverySecret.collectAsStateWithLifecycle()
    val targetAlgorithm by viewModel.migrationTargetAlgorithm.collectAsStateWithLifecycle()
    val newCredential by viewModel.migrationNewCredential.collectAsStateWithLifecycle()
    val rebindKeystore by viewModel.migrationRebindKeystore.collectAsStateWithLifecycle()
    val enableRecovery by viewModel.migrationEnableRecovery.collectAsStateWithLifecycle()
    val resultFile by viewModel.migrationResultFile.collectAsStateWithLifecycle()

    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val progressFraction by viewModel.progressFraction.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val currentChunk by viewModel.currentChunk.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.operationSuccessMessage.collectAsStateWithLifecycle()

    val containerPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.setMigrationContainerUri(uri)
    }

    val saveDestinationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { destinationUri ->
        if (destinationUri != null && resultFile != null) {
            viewModel.saveResultToDestinationUri(resultFile!!, destinationUri)
        }
    }

    ScreenWithActionBar(
        contentPadding = contentPadding,
        modifier = modifier,
        actionBar = {
            VaultButton(
                text = if (isProcessing) "Migrating Container..." else "Migrate Container",
                icon = Icons.Rounded.PublishedWithChanges,
                onClick = { viewModel.startMigration() },
                enabled = containerUri != null && header != null && !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("migrate_container_button")
            )
        }
    ) { listPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = listPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Source Container Selection
            item(key = "source-picker") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth().staggeredEntrance(0)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ShapeBadge(
                                icon = Icons.Rounded.FileOpen,
                                polygon = VaultBadgeShape.Cookie9Sided,
                                size = 48.dp,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Source Container",
                                    style = MaterialTheme.typography.titleMedium.emphasized()
                                )
                                Text(
                                    if (containerUri == null) "Select an existing .adsky container to migrate or upgrade"
                                    else containerUri!!.lastPathSegment ?: "Container selected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.size(16.dp))
                        VaultTonalButton(
                            text = if (containerUri == null) "Choose .adsky Container" else "Change Container",
                            icon = Icons.Rounded.FileOpen,
                            onClick = { containerPicker.launch(arrayOf("*/*")) },
                            modifier = Modifier.fillMaxWidth().testTag("pick_migration_container_button")
                        )
                    }
                }
            }

            // Container Inspection Details
            if (header != null) {
                item(key = "inspection-card") {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth().staggeredEntrance(1)
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Container Header Inspection",
                                style = MaterialTheme.typography.titleMedium.emphasized()
                            )
                            KeyValueRow(label = "Format Version", value = "v${header!!.formatVersion}")
                            KeyValueRow(label = "Current Generation", value = "Gen ${header!!.generationNumber}")
                            KeyValueRow(label = "Current Algorithm", value = header!!.algorithm.displayName)
                            KeyValueRow(label = "Envelopes Attached", value = envelopes.joinToString(", "))
                            KeyValueRow(
                                label = "Digital Signature",
                                value = if ((header!!.flags.toInt() and ContainerConstants.FLAG_IS_SIGNED.toInt()) != 0) "Signed (Hardware Key)" else "Unsigned"
                            )
                        }
                    }
                }

                // Authentication for Decryption
                item(key = "auth-section") {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth().staggeredEntrance(2)
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Unlock Authorization",
                                style = MaterialTheme.typography.titleMedium.emphasized()
                            )
                            Text(
                                "Provide authentication to unlock and migrate this container's payload.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val hasDevice = header!!.envelopes.any { it.envelopeType == ContainerConstants.ENVELOPE_DEVICE_KEYSTORE }
                            val hasCred = header!!.envelopes.any { it.envelopeType == ContainerConstants.ENVELOPE_CUSTOM_CREDENTIAL }
                            val hasRec = header!!.envelopes.any { it.envelopeType == ContainerConstants.ENVELOPE_RECOVERY_SECRET }

                            Column {
                                if (hasDevice) {
                                    RadioOptionRow(
                                        title = "Device Keystore",
                                        description = "Bound to this device's hardware security module",
                                        selected = authMode == DecryptAuthMode.DEVICE_KEYSTORE,
                                        shape = connectedShape(0, 3),
                                        onClick = { viewModel.setMigrationAuthMode(DecryptAuthMode.DEVICE_KEYSTORE) }
                                    )
                                }
                                if (hasCred) {
                                    RadioOptionRow(
                                        title = "Custom PIN or Password",
                                        description = "Argon2id credential derived unwrapping",
                                        selected = authMode == DecryptAuthMode.CUSTOM_CREDENTIAL,
                                        shape = connectedShape(1, 3),
                                        onClick = { viewModel.setMigrationAuthMode(DecryptAuthMode.CUSTOM_CREDENTIAL) }
                                    )
                                }
                                if (hasRec) {
                                    RadioOptionRow(
                                        title = "Master Recovery Secret",
                                        description = "24-word or 64-char installation master key",
                                        selected = authMode == DecryptAuthMode.RECOVERY_SECRET,
                                        shape = connectedShape(2, 3),
                                        onClick = { viewModel.setMigrationAuthMode(DecryptAuthMode.RECOVERY_SECRET) }
                                    )
                                }
                            }

                            if (authMode == DecryptAuthMode.CUSTOM_CREDENTIAL) {
                                SecretTextField(
                                    value = authCredential,
                                    onValueChange = { viewModel.setMigrationAuthCredential(it) },
                                    label = "Enter Current PIN / Password",
                                    modifier = Modifier.fillMaxWidth().testTag("migration_auth_credential_input")
                                )
                            } else if (authMode == DecryptAuthMode.RECOVERY_SECRET) {
                                SecretTextField(
                                    value = authRecoverySecret,
                                    onValueChange = { viewModel.setMigrationAuthRecoverySecret(it) },
                                    label = "Enter Recovery Secret",
                                    modifier = Modifier.fillMaxWidth().testTag("migration_auth_recovery_input")
                                )
                            }
                        }
                    }
                }

                // Target Migration Parameters
                item(key = "target-params") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Target Container Parameters")

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AlgorithmSelectionCard(
                                algorithm = CipherAlgorithm.AES_256_GCM,
                                isSelected = targetAlgorithm == CipherAlgorithm.AES_256_GCM,
                                onSelect = { viewModel.setMigrationTargetAlgorithm(CipherAlgorithm.AES_256_GCM) },
                                shape = connectedShape(0, 2)
                            )
                            AlgorithmSelectionCard(
                                algorithm = CipherAlgorithm.XCHACHA20_POLY1305,
                                isSelected = targetAlgorithm == CipherAlgorithm.XCHACHA20_POLY1305,
                                onSelect = { viewModel.setMigrationTargetAlgorithm(CipherAlgorithm.XCHACHA20_POLY1305) },
                                shape = connectedShape(1, 2)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ToggleOptionRow(
                                    title = "Rebind to this device's Android Keystore",
                                    description = "Generates new DEK envelope bound to current hardware HSM",
                                    checked = rebindKeystore,
                                    onCheckedChange = { viewModel.setMigrationRebindKeystore(it) },
                                    shape = connectedShape(0, 2)
                                )
                                ToggleOptionRow(
                                    title = "Wrap with Master Recovery Secret",
                                    description = "Ensures container can be recovered across devices",
                                    checked = enableRecovery,
                                    onCheckedChange = { viewModel.setMigrationEnableRecovery(it) },
                                    shape = connectedShape(1, 2)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "Optional New PIN / Password",
                                    style = MaterialTheme.typography.titleSmall.emphasized()
                                )
                                Spacer(Modifier.size(8.dp))
                                SecretTextField(
                                    value = newCredential,
                                    onValueChange = { viewModel.setMigrationNewCredential(it) },
                                    label = "Leave blank to keep existing credential mode",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // Progress / Status
            if (isProcessing) {
                item(key = "progress") {
                    OperationProgressCard(
                        isProcessing = isProcessing,
                        progress = progressFraction,
                        statusMessage = statusMessage,
                        currentChunk = currentChunk,
                        bytesProcessed = 0L,
                        totalBytes = 0L,
                        onCancel = { viewModel.cancelOperation() },
                        headline = "Migrating Container"
                    )
                }
            }

            if (errorMessage != null) {
                item(key = "error") {
                    StatusBanner(
                        kind = BannerKind.Error,
                        message = errorMessage!!
                    )
                }
            }

            if (successMessage != null && resultFile != null) {
                item(key = "success") {
                    ResultCard(
                        title = "Migration Succeeded!",
                        lines = listOf(successMessage!!),
                        actionText = "Save Migrated Container (.adsky)",
                        actionIcon = Icons.Rounded.SaveAlt,
                        onAction = {
                            saveDestinationLauncher.launch(resultFile!!.name)
                        },
                        cardTag = "migration_result_card",
                        actionTag = "save_migrated_file_button"
                    )
                }
            }
        }
    }
}

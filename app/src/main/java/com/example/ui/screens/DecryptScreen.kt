@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.ui.screens

import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crypto.ContainerConstants
import com.example.crypto.ParsedContainerHeader
import com.example.ui.DecryptAuthMode
import com.example.ui.VaultViewModel
import com.example.ui.components.BannerKind
import com.example.ui.components.InfoChip
import com.example.ui.components.KeyValueRow
import com.example.ui.components.OperationProgressCard
import com.example.ui.components.RadioOptionRow
import com.example.ui.components.ResultCard
import com.example.ui.components.ScreenWithActionBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.SecretTextField
import com.example.ui.components.ShapeBadge
import com.example.ui.components.StatusBanner
import com.example.ui.components.VaultButton
import com.example.ui.components.VaultTonalButton
import com.example.ui.components.connectedShape
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.VaultBadgeShape
import com.example.ui.theme.VaultMotion
import com.example.ui.theme.VaultTheme
import com.example.ui.theme.emphasized

@Composable
fun DecryptScreen(
    viewModel: VaultViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val ext = VaultTheme.colors

    val containerUri by viewModel.decryptContainerUri.collectAsStateWithLifecycle()
    val inspectedHeader by viewModel.inspectedHeader.collectAsStateWithLifecycle()
    val inspectedEnvelopes by viewModel.inspectedEnvelopes.collectAsStateWithLifecycle()
    val authMode by viewModel.decryptAuthMode.collectAsStateWithLifecycle()
    val credentialInput by viewModel.decryptCredentialInput.collectAsStateWithLifecycle()
    val recoverySecretInput by viewModel.decryptRecoverySecretInput.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val progressFraction by viewModel.progressFraction.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val currentChunk by viewModel.currentChunk.collectAsStateWithLifecycle()
    val bytesProcessed by viewModel.bytesProcessed.collectAsStateWithLifecycle()
    val totalBytes by viewModel.totalBytes.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val decryptedResult by viewModel.decryptedResult.collectAsStateWithLifecycle()

    val containerPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        viewModel.setDecryptContainerUri(uri)
    }
    val saveFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destinationUri: Uri? ->
        val result = decryptedResult
        if (destinationUri != null && result != null) {
            viewModel.saveResultToDestinationUri(result.decryptedTempFile, destinationUri)
        }
    }

    val header = inspectedHeader
    val listState = rememberLazyListState()
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            haptic.performHapticFeedback(HapticFeedbackType.Reject)
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1)
        }
    }
    LaunchedEffect(decryptedResult) {
        if (decryptedResult != null) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1)
        }
    }

    val canDecrypt = containerUri != null &&
        header != null &&
        !isProcessing &&
        (authMode != DecryptAuthMode.CUSTOM_CREDENTIAL || credentialInput.isNotEmpty()) &&
        (authMode != DecryptAuthMode.RECOVERY_SECRET || recoverySecretInput.isNotEmpty())

    BoxWithConstraints(modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        if (isWide) {
            // Adaptive 2-column layout on wide screens
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Column: Container file picker + Authorization method
                Column(
                    Modifier
                        .weight(1f)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    DecryptContainerCard(
                        containerUri = containerUri,
                        ext = ext,
                        onSelectContainer = { containerPicker.launch(arrayOf("*/*")) }
                    )

                    if (header != null) {
                        AuthorizationCard(
                            header = header,
                            authMode = authMode,
                            onSelectMode = { viewModel.setDecryptAuthMode(it) },
                            credentialInput = credentialInput,
                            onCredentialChange = { viewModel.setDecryptCredentialInput(it) },
                            recoverySecretInput = recoverySecretInput,
                            onRecoverySecretChange = { viewModel.setDecryptRecoverySecretInput(it) }
                        )
                    }
                }

                // Right Column: Header details + Action button + Progress / Results
                Column(
                    Modifier
                        .weight(1f)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (header != null) {
                        HeaderDetailsCard(
                            header = header,
                            inspectedEnvelopes = inspectedEnvelopes
                        )
                    }

                    OperationProgressCard(
                        isProcessing = isProcessing,
                        progress = progressFraction,
                        statusMessage = statusMessage,
                        currentChunk = currentChunk,
                        bytesProcessed = bytesProcessed,
                        totalBytes = totalBytes,
                        onCancel = { viewModel.cancelOperation() }
                    )

                    AnimatedVisibility(
                        visible = !isProcessing,
                        enter = expandVertically(VaultMotion.defaultSpatialSpec()) + fadeIn(VaultMotion.defaultEffectsSpec()),
                        exit = shrinkVertically(VaultMotion.fastSpatialSpec()) + fadeOut(VaultMotion.fastEffectsSpec())
                    ) {
                        VaultButton(
                            text = "Decrypt & verify container",
                            icon = Icons.Rounded.LockOpen,
                            enabled = canDecrypt,
                            onClick = { viewModel.startDecryption() },
                            modifier = Modifier.fillMaxWidth().testTag("start_decryption_button")
                        )
                    }

                    if (errorMessage != null) {
                        StatusBanner(
                            kind = BannerKind.Error,
                            message = errorMessage.orEmpty(),
                            modifier = Modifier.testTag("decryption_error_box")
                        )
                    }

                    val result = decryptedResult
                    if (result != null) {
                        ResultCard(
                            title = "Decrypted & verified",
                            lines = listOf(
                                result.metadata.originalFilename,
                                "${Formatter.formatFileSize(context, result.metadata.originalByteCount)} · ${result.metadata.mimeType}"
                            ),
                            badge = if (result.signatureVerified) "Signature verified: ${result.signerIdentity}" else null,
                            actionText = "Save decrypted file",
                            actionIcon = Icons.Rounded.SaveAlt,
                            onAction = { saveFileLauncher.launch(result.metadata.originalFilename) },
                            cardTag = "decryption_result_card",
                            actionTag = "save_decrypted_file_button"
                        )
                    }
                }
            }
        } else {
            // Standard single-column flow with action bar for compact phones
            ScreenWithActionBar(
                contentPadding = contentPadding,
                actionBar = {
                    OperationProgressCard(
                        isProcessing = isProcessing,
                        progress = progressFraction,
                        statusMessage = statusMessage,
                        currentChunk = currentChunk,
                        bytesProcessed = bytesProcessed,
                        totalBytes = totalBytes,
                        onCancel = { viewModel.cancelOperation() }
                    )
                    AnimatedVisibility(
                        visible = !isProcessing,
                        enter = expandVertically(VaultMotion.defaultSpatialSpec()) + fadeIn(VaultMotion.defaultEffectsSpec()),
                        exit = shrinkVertically(VaultMotion.fastSpatialSpec()) + fadeOut(VaultMotion.fastEffectsSpec())
                    ) {
                        VaultButton(
                            text = "Decrypt & verify container",
                            icon = Icons.Rounded.LockOpen,
                            enabled = canDecrypt,
                            onClick = { viewModel.startDecryption() },
                            modifier = Modifier.fillMaxWidth().testTag("start_decryption_button")
                        )
                    }
                }
            ) { padding ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = padding,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item(key = "picker") {
                        DecryptContainerCard(
                            containerUri = containerUri,
                            ext = ext,
                            onSelectContainer = { containerPicker.launch(arrayOf("*/*")) }
                        )
                    }

                    if (header != null) {
                        item(key = "header") {
                            HeaderDetailsCard(
                                header = header,
                                inspectedEnvelopes = inspectedEnvelopes
                            )
                        }

                        item(key = "auth") {
                            AuthorizationCard(
                                header = header,
                                authMode = authMode,
                                onSelectMode = { viewModel.setDecryptAuthMode(it) },
                                credentialInput = credentialInput,
                                onCredentialChange = { viewModel.setDecryptCredentialInput(it) },
                                recoverySecretInput = recoverySecretInput,
                                onRecoverySecretChange = { viewModel.setDecryptRecoverySecretInput(it) }
                            )
                        }
                    }

                    if (errorMessage != null) {
                        item(key = "error") {
                            StatusBanner(
                                kind = BannerKind.Error,
                                message = errorMessage.orEmpty(),
                                modifier = Modifier.testTag("decryption_error_box")
                            )
                        }
                    }

                    val result = decryptedResult
                    if (result != null) {
                        item(key = "result") {
                            ResultCard(
                                title = "Decrypted & verified",
                                lines = listOf(
                                    result.metadata.originalFilename,
                                    "${Formatter.formatFileSize(context, result.metadata.originalByteCount)} · ${result.metadata.mimeType}"
                                ),
                                badge = if (result.signatureVerified) "Signature verified: ${result.signerIdentity}" else null,
                                actionText = "Save decrypted file",
                                actionIcon = Icons.Rounded.SaveAlt,
                                onAction = { saveFileLauncher.launch(result.metadata.originalFilename) },
                                cardTag = "decryption_result_card",
                                actionTag = "save_decrypted_file_button"
                            )
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────────── component cards ─────────────────────────────

@Composable
private fun DecryptContainerCard(
    containerUri: Uri?,
    ext: com.example.ui.theme.VaultExtendedColors,
    onSelectContainer: () -> Unit
) {
    Column(Modifier.staggeredEntrance(0), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Encrypted file", step = 1, subtitle = "Choose an .adsky container.")
        Surface(
            Modifier.fillMaxWidth().testTag("decrypt_container_card"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShapeBadge(
                        icon = if (containerUri != null) Icons.Rounded.Check else Icons.Rounded.FileOpen,
                        polygon = if (containerUri != null) VaultBadgeShape.Cookie9Sided else VaultBadgeShape.Sunny,
                        size = 56.dp,
                        containerColor = if (containerUri != null) ext.success else MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = if (containerUri != null) ext.onSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        if (containerUri != null) "Container selected" else "No container selected",
                        style = MaterialTheme.typography.titleMedium.emphasized(),
                        modifier = Modifier.weight(1f)
                    )
                }
                VaultTonalButton(
                    text = if (containerUri == null) "Select .adsky file" else "Change container",
                    icon = Icons.Rounded.FileOpen,
                    onClick = onSelectContainer,
                    modifier = Modifier.fillMaxWidth().testTag("select_container_button")
                )
            }
        }
    }
}

@Composable
private fun HeaderDetailsCard(
    header: ParsedContainerHeader,
    inspectedEnvelopes: List<String>
) {
    val isSigned = (header.flags.toInt() and ContainerConstants.FLAG_IS_SIGNED.toInt()) != 0
    Surface(
        Modifier.fillMaxWidth().testTag("inspected_header_card"),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShapeBadge(
                    Icons.Rounded.Shield,
                    polygon = VaultBadgeShape.Clover4Leaf,
                    size = 44.dp,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
                Spacer(Modifier.width(14.dp))
                Text("Structure validated", style = MaterialTheme.typography.titleMedium.emphasized(), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.size(10.dp))
            KeyValueRow("Algorithm", header.algorithm.displayName)
            KeyValueRow("Format version", "v${header.formatVersion}", monospace = true)
            KeyValueRow("Generation", "#${header.generationNumber}", monospace = true)
            KeyValueRow(
                "Signature",
                if (isSigned) "Signed (ECDSA P-256)" else "Unsigned",
                valueColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.size(12.dp))
            Text("Available envelopes", style = MaterialTheme.typography.labelLarge.emphasized())
            Spacer(Modifier.size(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                inspectedEnvelopes.forEach { env ->
                    InfoChip(
                        env,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthorizationCard(
    header: ParsedContainerHeader,
    authMode: DecryptAuthMode,
    onSelectMode: (DecryptAuthMode) -> Unit,
    credentialInput: String,
    onCredentialChange: (String) -> Unit,
    recoverySecretInput: String,
    onRecoverySecretChange: (String) -> Unit
) {
    val hasDevice = header.envelopes.any { it.envelopeType == ContainerConstants.ENVELOPE_DEVICE_KEYSTORE }
    val hasCred = header.envelopes.any { it.envelopeType == ContainerConstants.ENVELOPE_CUSTOM_CREDENTIAL }
    val hasRecovery = header.envelopes.any { it.envelopeType == ContainerConstants.ENVELOPE_RECOVERY_SECRET }
    val count = listOf(hasDevice, hasCred, hasRecovery).count { it }
    var index = 0

    Column(Modifier.testTag("auth_mode_card"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Authorization", step = 2, subtitle = "Pick how to unlock this container.")
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (hasDevice) {
                RadioOptionRow(
                    selected = authMode == DecryptAuthMode.DEVICE_KEYSTORE,
                    onClick = { onSelectMode(DecryptAuthMode.DEVICE_KEYSTORE) },
                    title = "Original-device Keystore",
                    description = "Unlocks automatically with this phone's Keystore master key.",
                    shape = connectedShape(index++, count)
                )
            }
            if (hasCred) {
                RadioOptionRow(
                    selected = authMode == DecryptAuthMode.CUSTOM_CREDENTIAL,
                    onClick = { onSelectMode(DecryptAuthMode.CUSTOM_CREDENTIAL) },
                    title = "PIN or passphrase",
                    description = "Derives key with the Argon2id parameters stored in the header.",
                    shape = connectedShape(index++, count)
                )
            }
            if (hasRecovery) {
                RadioOptionRow(
                    selected = authMode == DecryptAuthMode.RECOVERY_SECRET,
                    onClick = { onSelectMode(DecryptAuthMode.RECOVERY_SECRET) },
                    title = "Master recovery secret",
                    description = "Use the secret from the original install, or one transferred from another device.",
                    shape = connectedShape(index++, count)
                )
            }
        }

        AnimatedVisibility(
            visible = authMode == DecryptAuthMode.CUSTOM_CREDENTIAL && hasCred,
            enter = expandVertically(VaultMotion.defaultSpatialSpec()) + fadeIn(VaultMotion.defaultEffectsSpec()),
            exit = shrinkVertically(VaultMotion.fastSpatialSpec()) + fadeOut(VaultMotion.fastEffectsSpec())
        ) {
            Surface(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(Modifier.padding(20.dp)) {
                    SecretTextField(
                        value = credentialInput,
                        onValueChange = onCredentialChange,
                        label = "PIN or passphrase",
                        modifier = Modifier.testTag("input_decrypt_credential")
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = authMode == DecryptAuthMode.RECOVERY_SECRET && hasRecovery,
            enter = expandVertically(VaultMotion.defaultSpatialSpec()) + fadeIn(VaultMotion.defaultEffectsSpec()),
            exit = shrinkVertically(VaultMotion.fastSpatialSpec()) + fadeOut(VaultMotion.fastEffectsSpec())
        ) {
            Surface(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(Modifier.padding(20.dp)) {
                    SecretTextField(
                        value = recoverySecretInput,
                        onValueChange = onRecoverySecretChange,
                        label = "Recovery secret (ADSKY-…)",
                        placeholder = "ADSKY-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX",
                        obscured = false,
                        modifier = Modifier.testTag("input_decrypt_recovery_secret")
                    )
                }
            }
        }
    }
}

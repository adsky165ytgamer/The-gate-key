@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui.screens

import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SaveAlt
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crypto.CipherAlgorithm
import com.example.crypto.KeystoreManager
import com.example.crypto.SignatureMode
import com.example.ui.VaultViewModel
import com.example.ui.components.AlgorithmSelectionCard
import com.example.ui.components.BannerKind
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
import com.example.ui.components.WeakPinWarningBox
import com.example.ui.components.connectedShape
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.VaultBadgeShape
import com.example.ui.theme.VaultMotion
import com.example.ui.theme.VaultTheme
import com.example.ui.theme.emphasized
import java.util.Locale

private const val SECTION_COUNT = 4

@Composable
fun EncryptScreen(
    viewModel: VaultViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val sourceUri by viewModel.encryptSourceUri.collectAsStateWithLifecycle()
    val sourceInfo by viewModel.encryptSourceInfo.collectAsStateWithLifecycle()
    val algorithm by viewModel.selectedAlgorithm.collectAsStateWithLifecycle()
    val credentialInput by viewModel.customCredentialInput.collectAsStateWithLifecycle()
    val enableDeviceKey by viewModel.enableDeviceKeystore.collectAsStateWithLifecycle()
    val enableRecoveryKey by viewModel.enableRecoverySecret.collectAsStateWithLifecycle()
    val signatureMode by viewModel.signatureMode.collectAsStateWithLifecycle()
    val customSigLabel by viewModel.customSignatureLabel.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val progressFraction by viewModel.progressFraction.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val currentChunk by viewModel.currentChunk.collectAsStateWithLifecycle()
    val bytesProcessed by viewModel.bytesProcessed.collectAsStateWithLifecycle()
    val totalBytes by viewModel.totalBytes.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.operationSuccessMessage.collectAsStateWithLifecycle()
    val resultFile by viewModel.encryptedResultFile.collectAsStateWithLifecycle()

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        viewModel.setEncryptSourceUri(uri)
    }
    val saveFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destinationUri: Uri? ->
        val file = resultFile
        if (destinationUri != null && file != null) viewModel.saveResultToDestinationUri(file, destinationUri)
    }

    val listState = rememberLazyListState()
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            haptic.performHapticFeedback(HapticFeedbackType.Reject)
            listState.animateScrollToItem(SECTION_COUNT)
        }
    }
    LaunchedEffect(resultFile) {
        if (resultFile != null) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            listState.animateScrollToItem(SECTION_COUNT + if (errorMessage != null) 1 else 0)
        }
    }

    val canEncrypt = sourceUri != null &&
        !isProcessing &&
        (signatureMode != SignatureMode.CUSTOM_8_LETTER || KeystoreManager.isValidCustomIdentity(customSigLabel)) &&
        (enableDeviceKey || enableRecoveryKey || credentialInput.isNotEmpty())

    BoxWithConstraints(modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        if (isWide) {
            // Adaptive 2-column layout for tablets, foldables, and landscape
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Column: Source file + Algorithm + Envelopes
                Column(
                    Modifier
                        .weight(1f)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1 - Source Payload
                    SourceFileCard(
                        sourceInfo = sourceInfo,
                        context = context,
                        onSelectFile = { filePicker.launch(arrayOf("*/*")) }
                    )

                    // 2 - Algorithm
                    AlgorithmCard(
                        algorithm = algorithm,
                        onSelectAlgorithm = { viewModel.setAlgorithm(it) }
                    )

                    // 3 - Envelopes & Passcode
                    EnvelopesCard(
                        enableDeviceKey = enableDeviceKey,
                        onToggleDeviceKey = { viewModel.setEnableDeviceKeystore(it) },
                        enableRecoveryKey = enableRecoveryKey,
                        onToggleRecoveryKey = { viewModel.setEnableRecoverySecret(it) },
                        credentialInput = credentialInput,
                        onCredentialChange = { viewModel.setCustomCredentialInput(it) }
                    )
                }

                // Right Column: Signature + Action + Progress + Results
                Column(
                    Modifier
                        .weight(1f)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    SignatureCard(
                        signatureMode = signatureMode,
                        onSelectMode = { viewModel.setSignatureMode(it) },
                        customSigLabel = customSigLabel,
                        onCustomLabelChange = { viewModel.setCustomSignatureLabel(it) }
                    )

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
                            text = "Encrypt to .adsky container",
                            icon = Icons.Rounded.Lock,
                            enabled = canEncrypt,
                            onClick = { viewModel.startEncryption() },
                            modifier = Modifier.fillMaxWidth().testTag("start_encryption_button")
                        )
                    }

                    if (errorMessage != null) {
                        StatusBanner(
                            kind = BannerKind.Error,
                            message = errorMessage.orEmpty(),
                            modifier = Modifier.testTag("encryption_error_box")
                        )
                    }

                    if (resultFile != null && successMessage != null) {
                        ResultCard(
                            title = "Container verified",
                            lines = listOf(successMessage.orEmpty()),
                            actionText = "Save .adsky container",
                            actionIcon = Icons.Rounded.SaveAlt,
                            onAction = { saveFileLauncher.launch("${sourceInfo?.first ?: "file"}.adsky") },
                            cardTag = "encryption_success_card",
                            actionTag = "save_encrypted_file_button"
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
                            text = "Encrypt to .adsky container",
                            icon = Icons.Rounded.Lock,
                            enabled = canEncrypt,
                            onClick = { viewModel.startEncryption() },
                            modifier = Modifier.fillMaxWidth().testTag("start_encryption_button")
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
                    item(key = "source") {
                        SourceFileCard(
                            sourceInfo = sourceInfo,
                            context = context,
                            onSelectFile = { filePicker.launch(arrayOf("*/*")) }
                        )
                    }

                    item(key = "algorithm") {
                        AlgorithmCard(
                            algorithm = algorithm,
                            onSelectAlgorithm = { viewModel.setAlgorithm(it) }
                        )
                    }

                    item(key = "envelopes") {
                        EnvelopesCard(
                            enableDeviceKey = enableDeviceKey,
                            onToggleDeviceKey = { viewModel.setEnableDeviceKeystore(it) },
                            enableRecoveryKey = enableRecoveryKey,
                            onToggleRecoveryKey = { viewModel.setEnableRecoverySecret(it) },
                            credentialInput = credentialInput,
                            onCredentialChange = { viewModel.setCustomCredentialInput(it) }
                        )
                    }

                    item(key = "signature") {
                        SignatureCard(
                            signatureMode = signatureMode,
                            onSelectMode = { viewModel.setSignatureMode(it) },
                            customSigLabel = customSigLabel,
                            onCustomLabelChange = { viewModel.setCustomSignatureLabel(it) }
                        )
                    }

                    if (errorMessage != null) {
                        item(key = "error") {
                            StatusBanner(
                                kind = BannerKind.Error,
                                message = errorMessage.orEmpty(),
                                modifier = Modifier.testTag("encryption_error_box")
                            )
                        }
                    }

                    if (resultFile != null && successMessage != null) {
                        item(key = "result") {
                            ResultCard(
                                title = "Container verified",
                                lines = listOf(successMessage.orEmpty()),
                                actionText = "Save .adsky container",
                                actionIcon = Icons.Rounded.SaveAlt,
                                onAction = { saveFileLauncher.launch("${sourceInfo?.first ?: "file"}.adsky") },
                                cardTag = "encryption_success_card",
                                actionTag = "save_encrypted_file_button"
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
private fun SourceFileCard(
    sourceInfo: Pair<String, Long>?,
    context: android.content.Context,
    onSelectFile: () -> Unit
) {
    Column(Modifier.staggeredEntrance(0), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Source file", step = 1, subtitle = "Documents, photos, archives - anything.")
        Surface(
            Modifier.fillMaxWidth().testTag("file_selection_card"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AnimatedContent(
                    targetState = sourceInfo,
                    transitionSpec = { fadeIn(VaultMotion.defaultEffectsSpec()) togetherWith fadeOut(VaultMotion.fastEffectsSpec()) },
                    label = "source-info"
                ) { info ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ShapeBadge(
                            icon = if (info != null) Icons.Rounded.Check else Icons.Rounded.FileOpen,
                            polygon = if (info != null) VaultBadgeShape.Cookie9Sided else VaultBadgeShape.Sunny,
                            size = 56.dp,
                            containerColor = if (info != null) VaultTheme.colors.success else MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = if (info != null) VaultTheme.colors.onSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                info?.first ?: "No file selected",
                                style = MaterialTheme.typography.titleMedium.emphasized(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (info != null) {
                                    "${Formatter.formatFileSize(context, info.second)} · ${String.format(Locale.getDefault(), "%,d", info.second)} bytes"
                                } else "Choose a file to begin",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                VaultTonalButton(
                    text = if (sourceInfo == null) "Select file to encrypt" else "Change selected file",
                    icon = Icons.Rounded.FileOpen,
                    onClick = onSelectFile,
                    modifier = Modifier.fillMaxWidth().testTag("select_source_file_button")
                )
            }
        }
    }
}

@Composable
private fun AlgorithmCard(
    algorithm: CipherAlgorithm,
    onSelectAlgorithm: (CipherAlgorithm) -> Unit
) {
    Column(Modifier.staggeredEntrance(1).testTag("algorithm_selection_card"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Payload algorithm", step = 2)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AlgorithmSelectionCard(
                algorithm = CipherAlgorithm.AES_256_GCM,
                isSelected = algorithm == CipherAlgorithm.AES_256_GCM,
                onSelect = { onSelectAlgorithm(CipherAlgorithm.AES_256_GCM) },
                shape = connectedShape(0, 2)
            )
            AlgorithmSelectionCard(
                algorithm = CipherAlgorithm.XCHACHA20_POLY1305,
                isSelected = algorithm == CipherAlgorithm.XCHACHA20_POLY1305,
                onSelect = { onSelectAlgorithm(CipherAlgorithm.XCHACHA20_POLY1305) },
                shape = connectedShape(1, 2)
            )
        }
    }
}

@Composable
private fun EnvelopesCard(
    enableDeviceKey: Boolean,
    onToggleDeviceKey: (Boolean) -> Unit,
    enableRecoveryKey: Boolean,
    onToggleRecoveryKey: (Boolean) -> Unit,
    credentialInput: String,
    onCredentialChange: (String) -> Unit
) {
    Column(Modifier.staggeredEntrance(2).testTag("envelopes_card"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Access envelopes", step = 3, subtitle = "Each one wraps the same 256-bit key independently.")
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ToggleOptionRow(
                checked = enableDeviceKey,
                onCheckedChange = onToggleDeviceKey,
                title = "Original-device Keystore",
                description = "Unlocks automatically on this phone using a non-exportable Keystore key.",
                shape = connectedShape(0, 3),
                switchTag = "switch_device_keystore"
            )
            ToggleOptionRow(
                checked = enableRecoveryKey,
                onCheckedChange = onToggleRecoveryKey,
                title = "Recovery secret",
                description = "Lets you recover or migrate to another device with your master secret.",
                shape = connectedShape(1, 3),
                switchTag = "switch_recovery_secret"
            )
            Surface(
                Modifier.fillMaxWidth(),
                shape = connectedShape(2, 3),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column {
                        Text("PIN or passphrase", style = MaterialTheme.typography.titleSmall.emphasized())
                        Text(
                            "Optional. Derived with Argon2id (3 iterations, 64 MB, 2 lanes).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SecretTextField(
                        value = credentialInput,
                        onValueChange = onCredentialChange,
                        label = "Leave blank to skip",
                        modifier = Modifier.testTag("input_custom_credential")
                    )
                    WeakPinWarningBox(input = credentialInput)
                }
            }
        }
    }
}

@Composable
private fun SignatureCard(
    signatureMode: SignatureMode,
    onSelectMode: (SignatureMode) -> Unit,
    customSigLabel: String,
    onCustomLabelChange: (String) -> Unit
) {
    Column(Modifier.staggeredEntrance(3).testTag("signature_card"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Signature identity", step = 4, subtitle = "ECDSA P-256 over container digest proves authenticity.")
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            RadioOptionRow(
                selected = signatureMode == SignatureMode.DEFAULT_HIDDEN,
                onClick = { onSelectMode(SignatureMode.DEFAULT_HIDDEN) },
                title = "Default hidden identity",
                description = "Anonymous signature, verifiable without revealing who signed.",
                shape = connectedShape(0, 3)
            )
            RadioOptionRow(
                selected = signatureMode == SignatureMode.CUSTOM_8_LETTER,
                onClick = { onSelectMode(SignatureMode.CUSTOM_8_LETTER) },
                title = "Custom eight-letter identity",
                description = "A label bound to a dedicated signing-key fingerprint.",
                shape = connectedShape(1, 3)
            )
            RadioOptionRow(
                selected = signatureMode == SignatureMode.NONE,
                onClick = { onSelectMode(SignatureMode.NONE) },
                title = "Unsigned",
                description = "Payload and header are protected by AEAD only.",
                shape = connectedShape(2, 3)
            )
        }
        AnimatedVisibility(
            visible = signatureMode == SignatureMode.CUSTOM_8_LETTER,
            enter = expandVertically(VaultMotion.defaultSpatialSpec()) + fadeIn(VaultMotion.defaultEffectsSpec()),
            exit = shrinkVertically(VaultMotion.fastSpatialSpec()) + fadeOut(VaultMotion.fastEffectsSpec())
        ) {
            val valid = customSigLabel.length == 8 && KeystoreManager.isValidCustomIdentity(customSigLabel)
            Surface(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(Modifier.padding(20.dp)) {
                    SecretTextField(
                        value = customSigLabel,
                        onValueChange = onCustomLabelChange,
                        label = "Eight-letter label (e.g. SECVAULT)",
                        supportingText = "Exactly 8 letters (${customSigLabel.length}/8)",
                        isError = !valid,
                        obscured = false,
                        modifier = Modifier.testTag("input_custom_signature_label")
                    )
                }
            }
        }
    }
}

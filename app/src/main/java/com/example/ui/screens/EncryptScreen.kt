package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CipherAlgorithm
import com.example.crypto.KeystoreManager
import com.example.crypto.SignatureMode
import com.example.ui.VaultViewModel
import com.example.ui.components.AlgorithmSelectionCard
import com.example.ui.components.OperationProgressCard
import com.example.ui.components.WeakPinWarningBox
import com.example.ui.theme.VaultAccentAmber
import com.example.ui.theme.VaultAccentGreen
import com.example.ui.theme.VaultAccentRose
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultCard
import com.example.ui.theme.VaultPrimary
import com.example.ui.theme.VaultPrimaryContainer
import com.example.ui.theme.VaultSurfaceVariant
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

@Composable
fun EncryptScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val sourceUri by viewModel.encryptSourceUri.collectAsState()
    val sourceInfo by viewModel.encryptSourceInfo.collectAsState()
    val algorithm by viewModel.selectedAlgorithm.collectAsState()
    val credentialInput by viewModel.customCredentialInput.collectAsState()
    val enableDeviceKey by viewModel.enableDeviceKeystore.collectAsState()
    val enableRecoveryKey by viewModel.enableRecoverySecret.collectAsState()
    val signatureMode by viewModel.signatureMode.collectAsState()
    val customSigLabel by viewModel.customSignatureLabel.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val progressFraction by viewModel.progressFraction.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val currentChunk by viewModel.currentChunk.collectAsState()
    val bytesProcessed by viewModel.bytesProcessed.collectAsState()
    val totalBytes by viewModel.totalBytes.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.operationSuccessMessage.collectAsState()
    val resultFile by viewModel.encryptedResultFile.collectAsState()

    // SAF Picker to select source file
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        viewModel.setEncryptSourceUri(uri)
    }

    // SAF Creator to save encrypted .adsky container
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destinationUri: Uri? ->
        if (destinationUri != null && resultFile != null) {
            viewModel.saveResultToDestinationUri(resultFile!!, destinationUri)
        }
    }

    var showPassword by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Encrypt to .adsky Container",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary
            )
            Text(
                text = "Stream arbitrary file into an authenticated, envelope-encrypted container.",
                fontSize = 12.sp,
                color = VaultTextSecondary
            )
        }

        // 1. File Selection Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("file_selection_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Source Payload",
                        fontWeight = FontWeight.Bold,
                        color = VaultPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (sourceInfo != null) {
                        Surface(
                            color = VaultSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = VaultAccentGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sourceInfo!!.first,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = VaultTextPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${sourceInfo!!.second / 1024} KB (${sourceInfo!!.second} bytes)",
                                        fontSize = 12.sp,
                                        color = VaultTextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("select_source_file_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = VaultPrimaryContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileOpen, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (sourceInfo == null) "Select File to Encrypt" else "Change Selected File",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2. Algorithm Selection
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("algorithm_selection_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. Payload Encryption Algorithm",
                        fontWeight = FontWeight.Bold,
                        color = VaultPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    AlgorithmSelectionCard(
                        algorithm = CipherAlgorithm.AES_256_GCM,
                        isSelected = algorithm == CipherAlgorithm.AES_256_GCM,
                        onSelect = { viewModel.setAlgorithm(CipherAlgorithm.AES_256_GCM) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    AlgorithmSelectionCard(
                        algorithm = CipherAlgorithm.XCHACHA20_POLY1305,
                        isSelected = algorithm == CipherAlgorithm.XCHACHA20_POLY1305,
                        onSelect = { viewModel.setAlgorithm(CipherAlgorithm.XCHACHA20_POLY1305) }
                    )
                }
            }
        }

        // 3. Authorization Envelopes
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("envelopes_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3. Access Envelopes (Key Management)",
                        fontWeight = FontWeight.Bold,
                        color = VaultPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Each selected envelope wraps the single 256-bit DEK independently.",
                        fontSize = 11.sp,
                        color = VaultTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Device Keystore Envelope
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setEnableDeviceKeystore(!enableDeviceKey) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = enableDeviceKey,
                            onCheckedChange = { viewModel.setEnableDeviceKeystore(it) },
                            modifier = Modifier.testTag("switch_device_keystore"),
                            colors = SwitchDefaults.colors(checkedThumbColor = VaultPrimary)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Original-Device Keystore Envelope",
                                fontWeight = FontWeight.SemiBold,
                                color = VaultTextPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Automatic unwrapping on this hardware using non-exportable Keystore key.",
                                fontSize = 11.sp,
                                color = VaultTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Recovery Secret Envelope
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setEnableRecoverySecret(!enableRecoveryKey) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = enableRecoveryKey,
                            onCheckedChange = { viewModel.setEnableRecoverySecret(it) },
                            modifier = Modifier.testTag("switch_recovery_secret"),
                            colors = SwitchDefaults.colors(checkedThumbColor = VaultAccentAmber)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Recovery-Secret Envelope",
                                fontWeight = FontWeight.SemiBold,
                                color = VaultTextPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Enables emergency recovery and migration to other devices using master secret.",
                                fontSize = 11.sp,
                                color = VaultTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Custom Credential (PIN / Passphrase)
                    Text(
                        text = "Custom User Credential (Optional PIN or Password)",
                        fontWeight = FontWeight.SemiBold,
                        color = VaultTextPrimary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Derived via Argon2id (iterations=3, memory=64MB, parallelism=2).",
                        fontSize = 11.sp,
                        color = VaultTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = credentialInput,
                        onValueChange = { viewModel.setCustomCredentialInput(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_credential"),
                        label = { Text("Enter PIN or Passphrase (leave blank to omit)") },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VaultPrimary,
                            unfocusedBorderColor = VaultBorder,
                            focusedTextColor = VaultTextPrimary,
                            unfocusedTextColor = VaultTextPrimary
                        ),
                        singleLine = true,
                        trailingIcon = {
                            Text(
                                text = if (showPassword) "Hide" else "Show",
                                color = VaultPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable { showPassword = !showPassword }
                                    .padding(end = 12.dp)
                            )
                        }
                    )

                    WeakPinWarningBox(
                        input = credentialInput,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // 4. Digital Signature Identity
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signature_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "4. Digital Signature Identity",
                        fontWeight = FontWeight.Bold,
                        color = VaultPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "ECDSA P-256 with SHA-256 signs container digest to guarantee authenticity.",
                        fontSize = 11.sp,
                        color = VaultTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Mode 1: Default Hidden Identity
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setSignatureMode(SignatureMode.DEFAULT_HIDDEN) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = signatureMode == SignatureMode.DEFAULT_HIDDEN,
                            onClick = { viewModel.setSignatureMode(SignatureMode.DEFAULT_HIDDEN) },
                            colors = RadioButtonDefaults.colors(selectedColor = VaultPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Default Hidden Identity",
                                fontWeight = FontWeight.SemiBold,
                                color = VaultTextPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Anonymous ECDSA signature verified without disclosing identity in plaintext.",
                                fontSize = 11.sp,
                                color = VaultTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mode 2: Custom 8-Letter Identity
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setSignatureMode(SignatureMode.CUSTOM_8_LETTER) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = signatureMode == SignatureMode.CUSTOM_8_LETTER,
                            onClick = { viewModel.setSignatureMode(SignatureMode.CUSTOM_8_LETTER) },
                            colors = RadioButtonDefaults.colors(selectedColor = VaultPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Custom Eight-Letter Identity",
                                fontWeight = FontWeight.SemiBold,
                                color = VaultTextPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Strictly 8 letters label bound to dedicated signing key fingerprint.",
                                fontSize = 11.sp,
                                color = VaultTextSecondary
                            )
                        }
                    }

                    AnimatedVisibility(visible = signatureMode == SignatureMode.CUSTOM_8_LETTER) {
                        Column(modifier = Modifier.padding(top = 10.dp, start = 32.dp)) {
                            OutlinedTextField(
                                value = customSigLabel,
                                onValueChange = { viewModel.setCustomSignatureLabel(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_custom_signature_label"),
                                label = { Text("Eight-Letter Label (e.g. SECVAULT)") },
                                singleLine = true,
                                isError = customSigLabel.length != 8 || !KeystoreManager.isValidCustomIdentity(customSigLabel),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VaultPrimary,
                                    unfocusedBorderColor = VaultBorder,
                                    focusedTextColor = VaultTextPrimary,
                                    unfocusedTextColor = VaultTextPrimary
                                )
                            )
                            Text(
                                text = "Must contain exactly 8 alphabetic letters (${customSigLabel.length}/8).",
                                fontSize = 11.sp,
                                color = if (customSigLabel.length == 8) VaultAccentGreen else VaultAccentAmber,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mode 3: Unsigned
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setSignatureMode(SignatureMode.NONE) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = signatureMode == SignatureMode.NONE,
                            onClick = { viewModel.setSignatureMode(SignatureMode.NONE) },
                            colors = RadioButtonDefaults.colors(selectedColor = VaultPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Unsigned Container (Payload & header authenticated by AEAD only)",
                            fontSize = 13.sp,
                            color = VaultTextSecondary
                        )
                    }
                }
            }
        }

        // Error message if any
        if (errorMessage != null) {
            item {
                Surface(
                    color = Color(0x33F43F5E),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VaultAccentRose),
                    modifier = Modifier.fillMaxWidth().testTag("encryption_error_box")
                ) {
                    Text(
                        text = errorMessage!!,
                        color = VaultAccentRose,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Progress Card during streaming
        item {
            OperationProgressCard(
                isProcessing = isProcessing,
                progress = progressFraction,
                statusMessage = statusMessage,
                currentChunk = currentChunk,
                bytesProcessed = bytesProcessed,
                totalBytes = totalBytes,
                onCancel = { viewModel.cancelOperation() }
            )
        }

        // Success Confirmation & Save Action
        if (resultFile != null && successMessage != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("encryption_success_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VaultAccentGreen)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = VaultAccentGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Container Verified Successfully",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = successMessage!!,
                            color = Color(0xFFD1FAE5),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                val originalName = sourceInfo?.first ?: "file"
                                saveFileLauncher.launch("$originalName.adsky")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_encrypted_file_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = VaultAccentGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.SaveAlt, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save .adsky Container via SAF", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Primary Action Button
        item {
            val canEncrypt = sourceUri != null &&
                    !isProcessing &&
                    (signatureMode != SignatureMode.CUSTOM_8_LETTER || KeystoreManager.isValidCustomIdentity(customSigLabel)) &&
                    (enableDeviceKey || enableRecoveryKey || credentialInput.isNotEmpty())

            Button(
                onClick = { viewModel.startEncryption() },
                enabled = canEncrypt,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("start_encryption_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VaultPrimary,
                    disabledContainerColor = VaultBorder
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = VaultSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isProcessing) "Encrypting Stream..." else "Encrypt to .adsky Container",
                    color = if (canEncrypt) VaultSurfaceVariant else VaultTextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

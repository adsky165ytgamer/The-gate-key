package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.ContainerConstants
import com.example.ui.DecryptAuthMode
import com.example.ui.VaultViewModel
import com.example.ui.components.OperationProgressCard
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
fun DecryptScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val containerUri by viewModel.decryptContainerUri.collectAsState()
    val inspectedHeader by viewModel.inspectedHeader.collectAsState()
    val inspectedEnvelopes by viewModel.inspectedEnvelopes.collectAsState()
    val authMode by viewModel.decryptAuthMode.collectAsState()
    val credentialInput by viewModel.decryptCredentialInput.collectAsState()
    val recoverySecretInput by viewModel.decryptRecoverySecretInput.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val progressFraction by viewModel.progressFraction.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val currentChunk by viewModel.currentChunk.collectAsState()
    val bytesProcessed by viewModel.bytesProcessed.collectAsState()
    val totalBytes by viewModel.totalBytes.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.operationSuccessMessage.collectAsState()
    val decryptedResult by viewModel.decryptedResult.collectAsState()

    var showPassword by remember { mutableStateOf(false) }

    // SAF Picker to select .adsky container
    val containerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        viewModel.setDecryptContainerUri(uri)
    }

    // SAF Creator to save decrypted payload
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destinationUri: Uri? ->
        if (destinationUri != null && decryptedResult != null) {
            viewModel.saveResultToDestinationUri(decryptedResult!!.decryptedTempFile, destinationUri)
        }
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
                text = "Decrypt & Verify Container",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary
            )
            Text(
                text = "Unwrap authorization envelopes, authenticate chunks, and verify digital signatures.",
                fontSize = 12.sp,
                color = VaultTextSecondary
            )
        }

        // 1. Container Picker
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("decrypt_container_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Encrypted .adsky File",
                        fontWeight = FontWeight.Bold,
                        color = VaultPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { containerPicker.launch(arrayOf("*/*")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("select_container_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = VaultPrimaryContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileOpen, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (containerUri == null) "Select .adsky File" else "Change Selected Container",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2. Inspected Header Details (Public metadata only, no private metadata before auth)
        if (inspectedHeader != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inspected_header_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = VaultSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = VaultPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Container Structure Validated",
                                fontWeight = FontWeight.Bold,
                                color = VaultTextPrimary,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Algorithm:", color = VaultTextSecondary, fontSize = 12.sp)
                            Text(
                                text = inspectedHeader!!.algorithm.displayName,
                                fontWeight = FontWeight.Bold,
                                color = VaultPrimary,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Format Version:", color = VaultTextSecondary, fontSize = 12.sp)
                            Text(
                                text = "v${inspectedHeader!!.formatVersion}",
                                color = VaultTextPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Generation Count:", color = VaultTextSecondary, fontSize = 12.sp)
                            Text(
                                text = "#${inspectedHeader!!.generationNumber}",
                                color = VaultTextPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        val isSigned = (inspectedHeader!!.flags.toInt() and ContainerConstants.FLAG_IS_SIGNED.toInt()) != 0
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Signature:", color = VaultTextSecondary, fontSize = 12.sp)
                            Text(
                                text = if (isSigned) "Signed (ECDSA P-256)" else "Unsigned",
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSigned) VaultAccentGreen else VaultTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Available Authorization Envelopes:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VaultTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        for (env in inspectedEnvelopes) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Box(modifier = Modifier.size(6.dp).background(VaultAccentGreen, CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = env, fontSize = 12.sp, color = VaultTextPrimary)
                            }
                        }
                    }
                }
            }

            // 3. Authorization Mode Selection
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_mode_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = VaultCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "2. Authorization Method",
                            fontWeight = FontWeight.Bold,
                            color = VaultPrimary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Device Keystore Option
                        val hasDeviceEnv = inspectedHeader!!.envelopes.any { it.envelopeType == ContainerConstants.ENVELOPE_DEVICE_KEYSTORE }
                        if (hasDeviceEnv) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setDecryptAuthMode(DecryptAuthMode.DEVICE_KEYSTORE) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = authMode == DecryptAuthMode.DEVICE_KEYSTORE,
                                    onClick = { viewModel.setDecryptAuthMode(DecryptAuthMode.DEVICE_KEYSTORE) },
                                    colors = RadioButtonDefaults.colors(selectedColor = VaultPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Original-Device Keystore",
                                        fontWeight = FontWeight.SemiBold,
                                        color = VaultTextPrimary,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Unwraps automatically using local Android Keystore master key.",
                                        fontSize = 11.sp,
                                        color = VaultTextSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Custom Credential Option
                        val hasCredEnv = inspectedHeader!!.envelopes.any { it.envelopeType == ContainerConstants.ENVELOPE_CUSTOM_CREDENTIAL }
                        if (hasCredEnv) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setDecryptAuthMode(DecryptAuthMode.CUSTOM_CREDENTIAL) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = authMode == DecryptAuthMode.CUSTOM_CREDENTIAL,
                                    onClick = { viewModel.setDecryptAuthMode(DecryptAuthMode.CUSTOM_CREDENTIAL) },
                                    colors = RadioButtonDefaults.colors(selectedColor = VaultPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Custom PIN or Passphrase",
                                        fontWeight = FontWeight.SemiBold,
                                        color = VaultTextPrimary,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Derives KEK with Argon2id parameters stored in the container header.",
                                        fontSize = 11.sp,
                                        color = VaultTextSecondary
                                    )
                                }
                            }

                            AnimatedVisibility(visible = authMode == DecryptAuthMode.CUSTOM_CREDENTIAL) {
                                Column(modifier = Modifier.padding(top = 8.dp, start = 32.dp)) {
                                    OutlinedTextField(
                                        value = credentialInput,
                                        onValueChange = { viewModel.setDecryptCredentialInput(it) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_decrypt_credential"),
                                        label = { Text("Enter PIN or Passphrase") },
                                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = VaultPrimary,
                                            unfocusedBorderColor = VaultBorder,
                                            focusedTextColor = VaultTextPrimary,
                                            unfocusedTextColor = VaultTextPrimary
                                        ),
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
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Recovery Secret Option
                        val hasRecEnv = inspectedHeader!!.envelopes.any { it.envelopeType == ContainerConstants.ENVELOPE_RECOVERY_SECRET }
                        if (hasRecEnv) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setDecryptAuthMode(DecryptAuthMode.RECOVERY_SECRET) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = authMode == DecryptAuthMode.RECOVERY_SECRET,
                                    onClick = { viewModel.setDecryptAuthMode(DecryptAuthMode.RECOVERY_SECRET) },
                                    colors = RadioButtonDefaults.colors(selectedColor = VaultAccentAmber)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Master Recovery Secret",
                                        fontWeight = FontWeight.SemiBold,
                                        color = VaultTextPrimary,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Use emergency recovery secret from original installation or cross-device transfer.",
                                        fontSize = 11.sp,
                                        color = VaultTextSecondary
                                    )
                                }
                            }

                            AnimatedVisibility(visible = authMode == DecryptAuthMode.RECOVERY_SECRET) {
                                Column(modifier = Modifier.padding(top = 8.dp, start = 32.dp)) {
                                    OutlinedTextField(
                                        value = recoverySecretInput,
                                        onValueChange = { viewModel.setDecryptRecoverySecretInput(it) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_decrypt_recovery_secret"),
                                        label = { Text("Enter Recovery Secret (ADSKY-...)") },
                                        placeholder = { Text("ADSKY-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = VaultAccentAmber,
                                            unfocusedBorderColor = VaultBorder,
                                            focusedTextColor = VaultTextPrimary,
                                            unfocusedTextColor = VaultTextPrimary
                                        )
                                    )
                                }
                            }
                        }
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
                    modifier = Modifier.fillMaxWidth().testTag("decryption_error_box")
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

        // Progress Card
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

        // Decrypted Result Card (Private metadata now visible because authenticated)
        if (decryptedResult != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("decryption_result_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VaultAccentGreen)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = VaultAccentGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Decrypted & Integrity Verified",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Original File: ${decryptedResult!!.metadata.originalFilename}",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Size: ${decryptedResult!!.metadata.originalByteCount / 1024} KB (${decryptedResult!!.metadata.originalByteCount} bytes) | Type: ${decryptedResult!!.metadata.mimeType}",
                            color = Color(0xFFD1FAE5),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (decryptedResult!!.signatureVerified) {
                            Surface(
                                color = Color(0xFF047857),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Signature Verified: ${decryptedResult!!.signerIdentity}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                saveFileLauncher.launch(decryptedResult!!.metadata.originalFilename)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_decrypted_file_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = VaultAccentGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.SaveAlt, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Decrypted File via SAF", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Action Button
        item {
            val canDecrypt = containerUri != null &&
                    inspectedHeader != null &&
                    !isProcessing &&
                    (authMode != DecryptAuthMode.CUSTOM_CREDENTIAL || credentialInput.isNotEmpty()) &&
                    (authMode != DecryptAuthMode.RECOVERY_SECRET || recoverySecretInput.isNotEmpty())

            Button(
                onClick = { viewModel.startDecryption() },
                enabled = canDecrypt,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("start_decryption_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VaultAccentGreen,
                    disabledContainerColor = VaultBorder
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isProcessing) "Decrypting Stream..." else "Decrypt & Verify Container",
                    color = if (canDecrypt) Color.Black else VaultTextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

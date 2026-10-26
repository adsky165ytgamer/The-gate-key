package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.SkyVaultEngine
import com.example.ui.VaultViewModel
import com.example.ui.theme.VaultAccentGreen
import com.example.ui.theme.VaultAccentRose
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultCard
import com.example.ui.theme.VaultPrimary
import com.example.ui.theme.VaultPrimaryContainer
import com.example.ui.theme.VaultSurfaceVariant
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import java.io.File

@Composable
fun BatchScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val batchUris by viewModel.batchUris.collectAsState()
    val batchCompletedCount by viewModel.batchCompletedCount.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val progressFraction by viewModel.progressFraction.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val algorithm by viewModel.selectedAlgorithm.collectAsState()
    val successMessage by viewModel.operationSuccessMessage.collectAsState()

    val multiFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        viewModel.setBatchUris(uris)
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
                text = "Batch Cryptographic Queue",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary
            )
            Text(
                text = "Process multiple files in sequence with independent random DEKs and unified security policies.",
                fontSize = 12.sp,
                color = VaultTextSecondary
            )
        }

        // Multi-file picker button
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("batch_picker_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Select Files for Queue",
                        fontWeight = FontWeight.Bold,
                        color = VaultPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Active algorithm: ${algorithm.displayName}. Files will be stored as encrypted containers.",
                        fontSize = 12.sp,
                        color = VaultTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { multiFilePicker.launch(arrayOf("*/*")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("select_multiple_files_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = VaultPrimaryContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (batchUris.isEmpty()) "Select Files (Multiple)" else "Reselect Queue (${batchUris.size} files)",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Queue List
        if (batchUris.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Queued Files (${batchUris.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = VaultPrimary
                    )
                    if (isProcessing) {
                        Text(
                            text = "Completed $batchCompletedCount of ${batchUris.size}",
                            fontSize = 12.sp,
                            color = VaultAccentGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            items(batchUris.size) { index ->
                val uri = batchUris[index]
                val info = SkyVaultEngine.getFileInfo(context, uri)
                val isCompleted = index < batchCompletedCount
                val isCurrentlyActive = isProcessing && index == batchCompletedCount

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("batch_item_$index"),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isCurrentlyActive) VaultSurfaceVariant else VaultCard),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isCurrentlyActive) VaultPrimary else if (isCompleted) VaultAccentGreen else VaultBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    if (isCompleted) VaultAccentGreen else if (isCurrentlyActive) VaultPrimary else VaultBorder,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = info.first,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = VaultTextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "${info.second / 1024} KB",
                                fontSize = 11.sp,
                                color = VaultTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Done",
                                tint = VaultAccentGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Progress Card
        if (isProcessing) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("batch_progress_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = VaultSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VaultPrimary)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Batch Processing",
                            fontWeight = FontWeight.Bold,
                            color = VaultPrimary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = VaultPrimary,
                            trackColor = VaultBorder
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = statusMessage,
                            fontSize = 12.sp,
                            color = VaultTextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { viewModel.cancelOperation() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultAccentRose),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VaultAccentRose)
                        ) {
                            Text("Cancel Batch")
                        }
                    }
                }
            }
        }

        if (successMessage != null) {
            item {
                Surface(
                    color = Color(0xFF064E3B),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VaultAccentGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = successMessage!!,
                        color = Color(0xFFD1FAE5),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Start Action Button
        item {
            val canStart = batchUris.isNotEmpty() && !isProcessing

            Button(
                onClick = {
                    viewModel.startBatchEncryption { file, outputName ->
                        // Save in app internal vault storage directory
                        val vaultDir = File(context.filesDir, "vault_batch_output").apply { mkdirs() }
                        val destFile = File(vaultDir, outputName)
                        file.copyTo(destFile, overwrite = true)
                    }
                },
                enabled = canStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("start_batch_encryption_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFA855F7),
                    disabledContainerColor = VaultBorder
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.FolderZip, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isProcessing) "Processing Queue..." else "Start Batch Encryption (${batchUris.size} files)",
                    color = if (canStart) Color.White else VaultTextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

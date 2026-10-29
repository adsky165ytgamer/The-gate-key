@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crypto.SkyVaultEngine
import com.example.ui.VaultViewModel
import com.example.ui.components.BannerKind
import com.example.ui.components.EmptyState
import com.example.ui.components.LoadingIndicator
import com.example.ui.components.OperationProgressCard
import com.example.ui.components.ScreenWithActionBar
import com.example.ui.components.SectionHeader
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
import com.example.ui.theme.mono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun BatchScreen(
    viewModel: VaultViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val ext = VaultTheme.colors

    val batchUris by viewModel.batchUris.collectAsStateWithLifecycle()
    val batchCompletedCount by viewModel.batchCompletedCount.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val progressFraction by viewModel.progressFraction.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val algorithm by viewModel.selectedAlgorithm.collectAsStateWithLifecycle()
    val successMessage by viewModel.operationSuccessMessage.collectAsStateWithLifecycle()

    val multiFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        viewModel.setBatchUris(uris)
    }

    val infos by produceState(initialValue = emptyList<Pair<String, Long>>(), batchUris) {
        value = withContext(Dispatchers.IO) { batchUris.map { SkyVaultEngine.getFileInfo(context, it) } }
    }

    LaunchedEffect(successMessage) {
        if (successMessage != null) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    val canStart = batchUris.isNotEmpty() && !isProcessing

    BoxWithConstraints(modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        if (isWide) {
            // Adaptive wide layout: queue configuration on the left, queue items on the right
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Column: Queue control card + action + progress
                Column(
                    Modifier
                        .weight(1f)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    BatchPickerCard(
                        batchUris = batchUris,
                        algorithmName = algorithm.displayName,
                        onSelectFiles = { multiFilePicker.launch(arrayOf("*/*")) }
                    )

                    OperationProgressCard(
                        isProcessing = isProcessing,
                        progress = progressFraction,
                        statusMessage = statusMessage,
                        currentChunk = 0,
                        bytesProcessed = 0,
                        totalBytes = 0,
                        onCancel = { viewModel.cancelOperation() },
                        headline = "Batch encryption · $batchCompletedCount of ${batchUris.size}",
                        cancelLabel = "Cancel batch",
                        modifier = Modifier.testTag("batch_progress_card")
                    )

                    AnimatedVisibility(
                        visible = !isProcessing,
                        enter = expandVertically(VaultMotion.defaultSpatialSpec()) + fadeIn(VaultMotion.defaultEffectsSpec()),
                        exit = shrinkVertically(VaultMotion.fastSpatialSpec()) + fadeOut(VaultMotion.fastEffectsSpec())
                    ) {
                        VaultButton(
                            text = if (batchUris.isEmpty()) "Start batch encryption" else "Encrypt ${batchUris.size} files",
                            icon = Icons.Rounded.FolderZip,
                            enabled = canStart,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            ),
                            onClick = {
                                viewModel.startBatchEncryption { file, outputName ->
                                    val vaultDir = File(context.filesDir, "vault_batch_output").apply { mkdirs() }
                                    file.copyTo(File(vaultDir, outputName), overwrite = true)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("start_batch_encryption_button")
                        )
                    }

                    if (successMessage != null) {
                        StatusBanner(
                            kind = BannerKind.Success,
                            title = "Batch finished",
                            message = successMessage.orEmpty()
                        )
                    }
                }

                // Right Column: Items list
                Column(
                    Modifier
                        .weight(1.2f)
                        .fillMaxSize()
                ) {
                    if (batchUris.isEmpty()) {
                        EmptyState(
                            icon = Icons.Rounded.FolderZip,
                            title = "Your queue is empty",
                            message = "Select several files and they are encrypted one after another."
                        )
                    } else {
                        SectionHeader("Queue", step = 2, subtitle = "Completed $batchCompletedCount of ${batchUris.size}")
                        Spacer(Modifier.size(8.dp))
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            itemsIndexed(batchUris, key = { index, uri -> "$index-$uri" }) { index, _ ->
                                val info = infos.getOrNull(index)
                                BatchItemRow(
                                    index = index,
                                    info = info,
                                    batchCompletedCount = batchCompletedCount,
                                    isProcessing = isProcessing,
                                    totalSize = batchUris.size,
                                    ext = ext,
                                    context = context
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Standard single-column flow with action bar for phone
            ScreenWithActionBar(
                contentPadding = contentPadding,
                actionBar = {
                    OperationProgressCard(
                        isProcessing = isProcessing,
                        progress = progressFraction,
                        statusMessage = statusMessage,
                        currentChunk = 0,
                        bytesProcessed = 0,
                        totalBytes = 0,
                        onCancel = { viewModel.cancelOperation() },
                        headline = "Batch encryption · $batchCompletedCount of ${batchUris.size}",
                        cancelLabel = "Cancel batch",
                        modifier = Modifier.testTag("batch_progress_card")
                    )
                    AnimatedVisibility(
                        visible = !isProcessing,
                        enter = expandVertically(VaultMotion.defaultSpatialSpec()) + fadeIn(VaultMotion.defaultEffectsSpec()),
                        exit = shrinkVertically(VaultMotion.fastSpatialSpec()) + fadeOut(VaultMotion.fastEffectsSpec())
                    ) {
                        VaultButton(
                            text = if (batchUris.isEmpty()) "Start batch encryption" else "Encrypt ${batchUris.size} files",
                            icon = Icons.Rounded.FolderZip,
                            enabled = canStart,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            ),
                            onClick = {
                                viewModel.startBatchEncryption { file, outputName ->
                                    val vaultDir = File(context.filesDir, "vault_batch_output").apply { mkdirs() }
                                    file.copyTo(File(vaultDir, outputName), overwrite = true)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("start_batch_encryption_button")
                        )
                    }
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = padding,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item(key = "picker") {
                        BatchPickerCard(
                            batchUris = batchUris,
                            algorithmName = algorithm.displayName,
                            onSelectFiles = { multiFilePicker.launch(arrayOf("*/*")) }
                        )
                    }

                    if (batchUris.isEmpty()) {
                        item(key = "empty") {
                            EmptyState(
                                icon = Icons.Rounded.FolderZip,
                                title = "Your queue is empty",
                                message = "Select several files and they are encrypted one after another."
                            )
                        }
                    } else {
                        item(key = "queue-header") {
                            SectionHeader("Queue", step = 2, subtitle = "Completed $batchCompletedCount of ${batchUris.size}")
                        }
                        itemsIndexed(batchUris, key = { index, uri -> "$index-$uri" }) { index, _ ->
                            val info = infos.getOrNull(index)
                            BatchItemRow(
                                index = index,
                                info = info,
                                batchCompletedCount = batchCompletedCount,
                                isProcessing = isProcessing,
                                totalSize = batchUris.size,
                                ext = ext,
                                context = context
                            )
                        }
                    }

                    if (successMessage != null) {
                        item(key = "success") {
                            StatusBanner(
                                kind = BannerKind.Success,
                                title = "Batch finished",
                                message = successMessage.orEmpty()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchPickerCard(
    batchUris: List<Uri>,
    algorithmName: String,
    onSelectFiles: () -> Unit
) {
    Column(Modifier.staggeredEntrance(0), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(
            "Choose files",
            step = 1,
            subtitle = "Every file gets its own random key. Algorithm: $algorithmName."
        )
        Surface(
            Modifier.fillMaxWidth().testTag("batch_picker_card"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShapeBadge(
                        Icons.Rounded.FolderOpen,
                        polygon = VaultBadgeShape.Clover4Leaf,
                        size = 56.dp,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        if (batchUris.isEmpty()) "Nothing queued yet" else "${batchUris.size} files queued",
                        style = MaterialTheme.typography.titleMedium.emphasized(),
                        modifier = Modifier.weight(1f)
                    )
                }
                VaultTonalButton(
                    text = if (batchUris.isEmpty()) "Select files" else "Reselect queue",
                    icon = Icons.Rounded.FolderOpen,
                    onClick = onSelectFiles,
                    modifier = Modifier.fillMaxWidth().testTag("select_multiple_files_button")
                )
            }
        }
    }
}

@Composable
private fun BatchItemRow(
    index: Int,
    info: Pair<String, Long>?,
    batchCompletedCount: Int,
    isProcessing: Boolean,
    totalSize: Int,
    ext: com.example.ui.theme.VaultExtendedColors,
    context: android.content.Context
) {
    val isCompleted = index < batchCompletedCount
    val isActive = isProcessing && index == batchCompletedCount
    val container = when {
        isActive -> MaterialTheme.colorScheme.secondaryContainer
        isCompleted -> ext.successContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Surface(
        Modifier.fillMaxWidth().testTag("batch_item_$index"),
        shape = connectedShape(index, totalSize),
        color = container
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.sizeIn(minWidth = 40.dp, minHeight = 40.dp), contentAlignment = Alignment.Center) {
                when {
                    isCompleted -> Surface(shape = CircleShape, color = ext.success, contentColor = ext.onSuccess) {
                        Icon(Icons.Rounded.Check, contentDescription = "Done", modifier = Modifier.padding(6.dp).size(20.dp))
                    }
                    isActive -> LoadingIndicator(Modifier.size(36.dp))
                    else -> Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelLarge.emphasized(),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    info?.first ?: "Reading file…",
                    style = MaterialTheme.typography.titleSmall.emphasized(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (info != null) {
                    Text(
                        Formatter.formatShortFileSize(context, info.second),
                        style = MaterialTheme.typography.bodySmall.mono(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

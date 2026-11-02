@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.camera.recognition

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.camera.core.CameraConfig
import com.example.camera.core.CameraPermissionGate
import com.example.camera.core.CameraPreview
import com.example.camera.core.OverlayButton
import com.example.camera.core.OverlayToggleButton
import com.example.camera.core.PreviewGeometry
import com.example.camera.core.rememberCameraSession
import com.example.ui.components.EmptyState
import com.example.ui.components.InfoChip
import com.example.ui.components.KeyValueRow
import com.example.ui.components.LoadingIndicator
import com.example.ui.components.VaultButton
import com.example.ui.components.VaultOutlinedButton
import com.example.ui.components.VaultTonalButton
import com.example.ui.components.connectedShape
import com.example.ui.theme.VaultTheme
import com.example.ui.theme.emphasized
import kotlinx.coroutines.delay

@Composable
fun QrScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: RecognitionViewModel = viewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session = rememberCameraSession()
    val caps by session.capabilities.collectAsStateWithLifecycle()
    var showHistory by remember { mutableStateOf(false) }

    val analyzer = remember(viewModel) { BarcodeAnalyzer { viewModel.onDetected(it) } }
    LaunchedEffect(state.current) {
        analyzer.enabled = state.current == null
        if (state.current != null) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
    }
    val config = remember(analyzer) { CameraConfig(analyzer = analyzer) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.decodeImage(uri)
    }
    LaunchedEffect(state.message) {
        if (state.message != null) { delay(3500); viewModel.clearMessage() }
    }

    CameraPermissionGate(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.Black)
        ) {
            CameraPreview(session = session, config = config, modifier = Modifier.fillMaxSize())
            ViewfinderOverlay(state.current)

            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (caps.hasFlash) {
                    OverlayToggleButton(
                        icon = if (caps.torchOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                        contentDescription = "Flashlight",
                        checked = caps.torchOn,
                        onCheckedChange = { session.setTorch(it) }
                    )
                } else Spacer(Modifier.size(48.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OverlayButton(Icons.Rounded.PhotoLibrary, "Scan from image", { picker.launch(arrayOf("image/*")) })
                    OverlayButton(Icons.Rounded.History, "Scan history", { showHistory = true })
                }
            }

            if (state.current == null) {
                Surface(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
                    shape = CircleShape, color = Color.Black.copy(alpha = 0.55f), contentColor = Color.White
                ) {
                    Text(
                        state.message ?: "Align a code inside the frame",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
            }

            if (state.decodingImage) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                    LoadingIndicator(Modifier.size(48.dp))
                }
            }

            AnimatedVisibility(
                visible = state.current != null,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                state.current?.let { ResultPanel(it, onClose = { viewModel.dismissCurrent() }) }
            }
        }
    }

    if (showHistory) {
        HistorySheet(
            items = state.history,
            onDismiss = { showHistory = false },
            onSelect = { viewModel.show(it); showHistory = false },
            onDelete = viewModel::delete,
            onClear = viewModel::clearHistory
        )
    }
}

@Composable
private fun ViewfinderOverlay(result: RecognitionResult?) {
    val detected = result != null
    val idle = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.95f)
    val hit = VaultTheme.colors.success
    val pulse by rememberInfiniteTransition(label = "viewfinder").animateFloat(
        initialValue = 0.96f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "pulse"
    )
    Canvas(Modifier.fillMaxSize()) {
        val side = minOf(size.width, size.height) * 0.68f * (if (detected) 1f else pulse)
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f - size.height * 0.04f
        val scrim = Color.Black.copy(alpha = 0.45f)
        // dim everything outside the frame
        drawRect(scrim, Offset.Zero, Size(size.width, top))
        drawRect(scrim, Offset(0f, top + side), Size(size.width, size.height - top - side))
        drawRect(scrim, Offset(0f, top), Size(left, side))
        drawRect(scrim, Offset(left + side, top), Size(size.width - left - side, side))

        val color = if (detected) hit else idle
        val arm = side * 0.2f
        val sw = 5.dp.toPx()
        val r = 18.dp.toPx()
        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(color, Offset(x, y + dy * r), Offset(x, y + dy * arm), sw, StrokeCap.Round)
            drawLine(color, Offset(x + dx * r, y), Offset(x + dx * arm, y), sw, StrokeCap.Round)
            drawArc(
                color, startAngle = if (dx > 0 && dy > 0) 180f else if (dx < 0 && dy > 0) 270f else if (dx < 0) 0f else 90f,
                sweepAngle = 90f, useCenter = false,
                topLeft = Offset(if (dx > 0) x else x - 2 * r, if (dy > 0) y else y - 2 * r),
                size = Size(2 * r, 2 * r), style = Stroke(sw, cap = StrokeCap.Round)
            )
        }
        corner(left, top, 1f, 1f)
        corner(left + side, top, -1f, 1f)
        corner(left + side, top + side, -1f, -1f)
        corner(left, top + side, 1f, -1f)

        // highlight the recognized code
        result?.takeIf { it.points.isNotEmpty() && it.frameWidth > 0 }?.let { r2 ->
            r2.points.forEach {
                drawCircle(hit, 7.dp.toPx(), PreviewGeometry.toView(it, r2.frameWidth, r2.frameHeight, size))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultPanel(result: RecognitionResult, onClose: () -> Unit) {
    val context = LocalContext.current
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(Modifier.padding(20.dp).heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoChip(result.format, leading = Icons.Rounded.QrCodeScanner)
                    InfoChip(
                        result.kind.label,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close result") }
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                if (result.fields.isNotEmpty()) {
                    result.fields.forEach { (k, v) -> KeyValueRow(k, v) }
                } else {
                    SelectionContainer {
                        Text(
                            result.text,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = if (result.kind == ContentKind.Product) FontFamily.Monospace else null
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                VaultButton(
                    "Copy", { copyText(context, result.copyValue()) }, Modifier.weight(1f), Icons.Rounded.ContentCopy
                )
                VaultTonalButton("Share", { shareText(context, result.text) }, Modifier.weight(1f), Icons.Rounded.Share)
            }
            result.actionIntent()?.let { (label, intent) ->
                VaultOutlinedButton(label, { runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }, Modifier.fillMaxWidth(), Icons.Rounded.OpenInNew)
            }
            VaultOutlinedButton("Scan again", onClose, Modifier.fillMaxWidth())
        }
    }
}

private fun RecognitionResult.copyValue(): String =
    if (kind == ContentKind.Wifi) fields.firstOrNull { it.first == "Password" }?.second ?: text else text

private fun RecognitionResult.actionIntent(): Pair<String, Intent>? = when (kind) {
    ContentKind.Url -> "Open link" to Intent(Intent.ACTION_VIEW, Uri.parse(text.trim()))
    ContentKind.Email -> "Write email" to Intent(Intent.ACTION_SENDTO, Uri.parse(if (text.startsWith("mailto:", true)) text else "mailto:${text.trim()}"))
    ContentKind.Phone -> "Dial number" to Intent(Intent.ACTION_DIAL, Uri.parse(text.trim()))
    ContentKind.Geo -> "Open in maps" to Intent(Intent.ACTION_VIEW, Uri.parse(text.trim()))
    else -> null
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Scanned code", text))
}

private fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
private fun HistorySheet(
    items: List<RecognitionResult>,
    onDismiss: () -> Unit,
    onSelect: (RecognitionResult) -> Unit,
    onDelete: (RecognitionResult) -> Unit,
    onClear: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Scan history", style = MaterialTheme.typography.titleLarge.emphasized(), modifier = Modifier.weight(1f))
                if (items.isNotEmpty()) TextButton(onClick = onClear) { Text("Clear all") }
            }
            Spacer(Modifier.size(8.dp))
            if (items.isEmpty()) {
                EmptyState(Icons.Rounded.History, "No scans yet", "Codes you scan are listed here.")
                Spacer(Modifier.size(24.dp))
            } else {
                LazyColumn(Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    itemsIndexed(items, key = { i, it -> "${it.timestamp}-$i" }) { i, item ->
                        Surface(
                            onClick = { onSelect(item) },
                            shape = connectedShape(i, items.size),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.text, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "${item.format} · ${DateUtils.getRelativeTimeSpanString(item.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDelete(item) }) { Icon(Icons.Rounded.Delete, contentDescription = "Delete from history") }
                            }
                        }
                    }
                }
                Spacer(Modifier.size(24.dp))
            }
        }
    }
}

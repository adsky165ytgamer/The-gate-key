@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.camera.scanner

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.camera.core.BitmapLoader
import com.example.camera.core.CameraConfig
import com.example.camera.core.CameraPermissionGate
import com.example.camera.core.CameraPreview
import com.example.camera.core.OverlayButton
import com.example.camera.core.OverlayToggleButton
import com.example.camera.core.Pt
import com.example.camera.core.PreviewGeometry
import com.example.camera.core.rememberCameraSession
import com.example.ui.components.BannerKind
import com.example.ui.components.EmptyState
import com.example.ui.components.LoadingIndicator
import com.example.ui.components.StatusBanner
import com.example.ui.components.VaultButton
import com.example.ui.components.VaultOutlinedButton
import com.example.ui.components.VaultTonalButton
import com.example.ui.theme.VaultTheme
import com.example.ui.theme.emphasized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScannerScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: ScannerViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler(enabled = state.stage != ScannerStage.Camera) {
        if (state.stage == ScannerStage.Crop) viewModel.cancelEditing() else viewModel.backToCamera()
    }

    Box(modifier.fillMaxSize()) {
        Crossfade(targetState = state.stage, label = "scanner-stage") { stage ->
            when (stage) {
                ScannerStage.Camera -> ScannerCamera(viewModel, state, contentPadding)
                ScannerStage.Crop -> ScannerCrop(viewModel, state, contentPadding)
                ScannerStage.Review -> ScannerReview(viewModel, state, contentPadding)
            }
        }
        BusyOverlay(state.busyLabel)
    }
}

@Composable
private fun BusyOverlay(label: String?) {
    AnimatedVisibility(visible = label != null) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    LoadingIndicator(Modifier.size(40.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(label.orEmpty(), style = MaterialTheme.typography.titleMedium.emphasized())
                }
            }
        }
    }
}

// ───────────────────────────── camera ─────────────────────────────

@Composable
private fun ScannerCamera(vm: ScannerViewModel, state: ScannerUiState, contentPadding: PaddingValues) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val session = rememberCameraSession()
    val caps by session.capabilities.collectAsStateWithLifecycle()
    val analyzer = remember(vm) { DocumentAnalyzer { q, w, h -> vm.onFrame(q, w, h) } }
    val config = remember(analyzer) { CameraConfig(capture = true, analyzer = analyzer) }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        vm.importImages(uris)
    }

    fun takePhoto() {
        scope.launch {
            session.capture(vm.newCaptureFile())
                .onSuccess { haptic.performHapticFeedback(HapticFeedbackType.Confirm); vm.onPhotoCaptured(it) }
                .onFailure { vm.onCaptureFailed("Could not capture the page. Try again.") }
        }
    }

    LaunchedEffect(vm) { vm.captureRequests.collect { takePhoto() } }
    LaunchedEffect(state.message) {
        if (state.message != null) { delay(4000); vm.dismissMessage() }
    }

    CameraPermissionGate(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.Black)
        ) {
            CameraPreview(session = session, config = config, modifier = Modifier.fillMaxSize())
            DetectionOverlay(vm)

            // Top controls
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (caps.hasFlash) {
                        OverlayToggleButton(
                            icon = if (caps.torchOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                            contentDescription = "Flashlight",
                            checked = caps.torchOn,
                            onCheckedChange = { session.setTorch(it) }
                        )
                    }
                    OverlayToggleButton(
                        icon = Icons.Rounded.CenterFocusStrong,
                        contentDescription = "Automatic capture",
                        checked = state.autoCapture,
                        onCheckedChange = { vm.setAutoCapture(it) }
                    )
                }
                HintChip(vm, state.autoCapture)
            }

            state.message?.let {
                StatusBanner(
                    kind = if (it.isError) BannerKind.Error else BannerKind.Info,
                    message = it.text,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp, start = 12.dp, end = 12.dp)
                )
            }

            // Bottom controls
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OverlayButton(Icons.Rounded.PhotoLibrary, "Import images", { gallery.launch(arrayOf("image/*")) }, Modifier.size(56.dp))
                ShutterButton(vm) { if (vm.markCaptureStarted()) takePhoto() }
                if (state.pages.isNotEmpty()) {
                    BadgedBox(badge = { Badge { Text("${state.pages.size}") } }) {
                        OverlayButton(Icons.Rounded.Layers, "Review ${state.pages.size} pages", { vm.showReview() }, Modifier.size(56.dp))
                    }
                } else {
                    Spacer(Modifier.size(56.dp))
                }
            }
        }
    }
}

@Composable
private fun HintChip(vm: ScannerViewModel, auto: Boolean) {
    val live by vm.live.collectAsStateWithLifecycle()
    val text = when {
        live.quad == null -> "Point at a document"
        live.stability < 1f -> "Hold steady"
        auto -> "Capturing"
        else -> "Ready"
    }
    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.55f), contentColor = Color.White) {
        Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}

@Composable
private fun DetectionOverlay(vm: ScannerViewModel) {
    val live by vm.live.collectAsStateWithLifecycle()
    val primary = MaterialTheme.colorScheme.primary
    val ready = VaultTheme.colors.success
    val quad = live.quad ?: return
    val stable by animateFloatAsState(live.stability, label = "stability")
    val stroke = androidx.compose.ui.graphics.lerp(primary, ready, stable)
    Canvas(Modifier.fillMaxSize()) {
        val pts = quad.points().map { PreviewGeometry.toView(it, live.frameWidth, live.frameHeight, size) }
        val path = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until 4) lineTo(pts[i].x, pts[i].y)
            close()
        }
        drawPath(path, stroke.copy(alpha = 0.16f + 0.14f * stable))
        drawPath(path, stroke, style = Stroke(width = 3.dp.toPx()))
        pts.forEach { drawCircle(stroke, radius = 6.dp.toPx(), center = it) }
    }
}

@Composable
private fun ShutterButton(vm: ScannerViewModel, onClick: () -> Unit) {
    val live by vm.live.collectAsStateWithLifecycle()
    val progress by animateFloatAsState(live.stability, label = "shutter-progress")
    val ring = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .size(80.dp)
            .clip(CircleShape)
            .clickable(onClickLabel = "Capture page", onClick = onClick)
            .semantics { contentDescription = "Capture page" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = 0.35f), style = Stroke(width = 4.dp.toPx()))
            if (progress > 0f) {
                drawArc(
                    color = ring, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                    style = Stroke(width = 5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
        }
        Box(Modifier.size(60.dp).clip(CircleShape).background(Color.White))
    }
}

// ───────────────────────────── crop ─────────────────────────────

@Composable
private fun ScannerCrop(vm: ScannerViewModel, state: ScannerUiState, contentPadding: PaddingValues) {
    val page = state.editing ?: return
    val bitmap by produceState<Bitmap?>(null, page.file) {
        value = withContext(Dispatchers.IO) { BitmapLoader.decode(page.file, 1600) }
    }
    val quad by rememberUpdatedState(page.quad)
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val ld = LocalLayoutDirection.current

    Column(
        Modifier.fillMaxSize().padding(
            start = contentPadding.calculateStartPadding(ld),
            end = contentPadding.calculateEndPadding(ld),
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            val bmp = bitmap
            if (bmp == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator(Modifier.size(48.dp)) }
            } else {
                BoxWithConstraints(Modifier.fillMaxSize().padding(20.dp)) {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val boxW = with(density) { maxWidth.toPx() }
                    val boxH = with(density) { maxHeight.toPx() }
                    val scale = minOf(boxW / bmp.width, boxH / bmp.height)
                    val imgW = bmp.width * scale
                    val imgH = bmp.height * scale
                    val left = (boxW - imgW) / 2f
                    val top = (boxH - imgH) / 2f
                    val handleHit = with(density) { 56.dp.toPx() }

                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Captured page",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    Canvas(
                        Modifier.fillMaxSize().pointerInput(bmp) {
                            var active = -1
                            detectDragGestures(
                                onDragStart = { start ->
                                    val pts = quad.points().map { Offset(left + it.x * imgW, top + it.y * imgH) }
                                    val nearest = pts.indices.minByOrNull { (pts[it] - start).getDistance() } ?: -1
                                    active = if (nearest >= 0 && (pts[nearest] - start).getDistance() <= handleHit) nearest else -1
                                },
                                onDrag = { change, _ ->
                                    if (active >= 0) {
                                        change.consume()
                                        val nx = ((change.position.x - left) / imgW).coerceIn(0f, 1f)
                                        val ny = ((change.position.y - top) / imgH).coerceIn(0f, 1f)
                                        vm.updateEditingQuad(quad.with(active, Pt(nx, ny)))
                                    }
                                },
                                onDragEnd = { active = -1 },
                                onDragCancel = { active = -1 }
                            )
                        }
                    ) {
                        val pts = page.quad.points().map { Offset(left + it.x * imgW, top + it.y * imgH) }
                        val polygon = Path().apply {
                            moveTo(pts[0].x, pts[0].y)
                            for (i in 1 until 4) lineTo(pts[i].x, pts[i].y)
                            close()
                        }
                        val shade = Path().apply {
                            fillType = PathFillType.EvenOdd
                            addRect(androidx.compose.ui.geometry.Rect(left, top, left + imgW, top + imgH))
                            addPath(polygon)
                        }
                        drawPath(shade, Color.Black.copy(alpha = 0.5f))
                        drawPath(polygon, primary, style = Stroke(width = 3.dp.toPx()))
                        pts.forEach {
                            drawCircle(Color.White, radius = 13.dp.toPx(), center = it)
                            drawCircle(primary, radius = 13.dp.toPx(), center = it, style = Stroke(width = 3.dp.toPx()))
                            drawCircle(onPrimary.copy(alpha = 0f), radius = 1f, center = it)
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VaultTonalButton("Detect edges", { vm.redetectEditing() }, Modifier.weight(1f), Icons.Rounded.DocumentScanner)
            VaultTonalButton("Full page", { vm.useFullImage() }, Modifier.weight(1f), Icons.Rounded.Crop)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VaultOutlinedButton(if (state.editingIsNew) "Retake" else "Cancel", { vm.cancelEditing() }, Modifier.weight(1f))
            VaultButton("Apply", { vm.confirmEditing() }, Modifier.weight(1f), Icons.Rounded.Check)
        }
    }
}

// ───────────────────────────── review ─────────────────────────────

@Composable
private fun ScannerReview(vm: ScannerViewModel, state: ScannerUiState, contentPadding: PaddingValues) {
    val page = state.selected
    val scope = rememberCoroutineScope()
    val ld = LocalLayoutDirection.current
    val stamp = remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date()) }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) vm.exportPdf(uri)
    }
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
        if (uri != null) vm.exportImage(uri)
    }
    LaunchedEffect(state.message) {
        if (state.message != null) { delay(4000); vm.dismissMessage() }
    }

    if (page == null) {
        Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
            EmptyState(Icons.Rounded.DocumentScanner, "No pages yet", "Scan a page to start a document.")
        }
        return
    }
    val index = state.pages.indexOfFirst { it.id == page.id }

    Column(
        Modifier.fillMaxSize().padding(
            start = contentPadding.calculateStartPadding(ld),
            end = contentPadding.calculateEndPadding(ld),
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        state.message?.let {
            StatusBanner(if (it.isError) BannerKind.Error else BannerKind.Success, it.text)
        }

        // Large preview
        Surface(
            Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            val bitmap by produceState<Bitmap?>(null, page) { value = vm.preview(page, 1400) }
            Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                bitmap?.let {
                    Image(
                        it.asImageBitmap(), "Page ${index + 1} of ${state.pages.size}",
                        Modifier.fillMaxSize(), contentScale = ContentScale.Fit
                    )
                } ?: LoadingIndicator(Modifier.size(48.dp))
                Surface(
                    Modifier.align(Alignment.TopEnd),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    contentColor = Color.White
                ) {
                    Text(
                        "${index + 1} / ${state.pages.size}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Filters
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScanFilter.entries.forEach { filter ->
                FilterChip(
                    selected = page.filter == filter,
                    onClick = { vm.setFilter(page.id, filter) },
                    label = { Text(filter.label) }
                )
            }
        }

        // Page actions
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            PageAction(Icons.Rounded.ArrowBack, "Move page earlier", enabled = index > 0) { vm.movePage(page.id, -1) }
            PageAction(Icons.Rounded.ArrowForward, "Move page later", enabled = index < state.pages.lastIndex) { vm.movePage(page.id, 1) }
            PageAction(Icons.AutoMirrored.Rounded.RotateRight, "Rotate page") { vm.rotate(page.id) }
            PageAction(Icons.Rounded.Crop, "Adjust corners") { vm.adjustCrop(page.id) }
            PageAction(Icons.Rounded.Delete, "Delete page") { vm.deletePage(page.id) }
        }

        // Page strip
        LazyRow(
            Modifier.fillMaxWidth().height(96.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(state.pages, key = { _, p -> p.id }) { i, p ->
                val thumb by produceState<Bitmap?>(null, p) { value = vm.preview(p, 260) }
                val selected = p.id == page.id
                Surface(
                    onClick = { vm.selectPage(p.id) },
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(0.75f)
                        .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else Modifier),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    thumb?.let {
                        Image(it.asImageBitmap(), "Page ${i + 1}", Modifier.fillMaxSize().graphicsLayer { }, contentScale = ContentScale.Crop)
                    }
                }
            }
            item(key = "add") {
                Surface(
                    onClick = { vm.backToCamera() },
                    modifier = Modifier.fillMaxHeight().aspectRatio(0.75f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text("Add", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        // Export
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VaultButton("Save PDF", { pdfLauncher.launch("Scan_$stamp.pdf") }, Modifier.weight(1.2f), Icons.Rounded.PictureAsPdf)
            VaultTonalButton("Save image", { imageLauncher.launch("Scan_${stamp}_${index + 1}.jpg") }, Modifier.weight(1f), Icons.Rounded.Image)
        }
    }
}

@Composable
private fun PageAction(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(52.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors()
    ) { Icon(icon, contentDescription = description) }
}

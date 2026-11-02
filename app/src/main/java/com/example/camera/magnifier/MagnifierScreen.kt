@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.camera.magnifier

import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.camera.view.PreviewView.ImplementationMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.camera.core.CameraConfig
import com.example.camera.core.CameraPermissionGate
import com.example.camera.core.CameraPreview
import com.example.camera.core.rememberCameraSession
import com.example.ui.theme.emphasized
import java.util.Locale

@Composable
fun MagnifierScreen(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    val session = rememberCameraSession()
    val caps by session.capabilities.collectAsStateWithLifecycle()
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var filterOrdinal by rememberSaveable { mutableIntStateOf(0) }
    val filter = ViewFilter.entries[filterOrdinal]
    var tuneOpen by rememberSaveable { mutableStateOf(false) }
    val config = remember { CameraConfig(implementationMode = ImplementationMode.COMPATIBLE) }

    LaunchedEffect(filter, previewView) { previewView?.let { MagnifierController.applyFilter(it, filter) } }

    CameraPermissionGate(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.Black)
        ) {
            CameraPreview(
                session = session,
                config = config,
                modifier = Modifier.fillMaxSize(),
                onPreviewView = { previewView = it }
            )


            // Zoom readout
            Surface(
                Modifier.align(Alignment.TopCenter).padding(12.dp),
                shape = CircleShape, color = Color.Black.copy(alpha = 0.55f), contentColor = Color.White
            ) {
                Text(
                    String.format(Locale.getDefault(), "%.1f×", caps.zoomRatio),
                    style = MaterialTheme.typography.titleMedium.emphasized(),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp).semantics { contentDescription = "Zoom ${"%.1f".format(caps.zoomRatio)} times" }
                )
            }

            // Controls
            Surface(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f)
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (caps.canZoom) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { session.setZoomRatio(caps.zoomRatio / 1.25f) }) {
                                Icon(Icons.Rounded.ZoomOut, contentDescription = "Zoom out")
                            }
                            Slider(
                                value = MagnifierController.fractionFor(caps.zoomRatio, caps.minZoomRatio, caps.maxZoomRatio),
                                onValueChange = { session.setZoomRatio(MagnifierController.ratioFor(it, caps.minZoomRatio, caps.maxZoomRatio)) },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { session.setZoomRatio(caps.zoomRatio * 1.25f) }) {
                                Icon(Icons.Rounded.ZoomIn, contentDescription = "Zoom in")
                            }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (caps.hasFlash) {
                            FilterChip(
                                selected = caps.torchOn,
                                onClick = { session.setTorch(!caps.torchOn) },
                                label = { Text("Flashlight") },
                                leadingIcon = { Icon(if (caps.torchOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff, null) }
                            )
                        }
                        AssistChip(
                            onClick = { session.focusCenter() },
                            label = { Text("Focus") },
                            leadingIcon = { Icon(Icons.Rounded.CenterFocusStrong, null) }
                        )
                        FilterChip(
                            selected = tuneOpen,
                            onClick = { tuneOpen = !tuneOpen },
                            label = { Text("View") },
                            leadingIcon = { Icon(Icons.Rounded.Tune, null) }
                        )
                    }
                    AnimatedVisibility(
                        visible = tuneOpen,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ViewFilter.entries.forEach { f ->
                                    FilterChip(
                                        selected = f == filter,
                                        onClick = { filterOrdinal = f.ordinal },
                                        label = { Text(f.label) }
                                    )
                                }
                            }
                            if (caps.exposureSupported && caps.exposureMax > caps.exposureMin) {
                                Text("Brightness", style = MaterialTheme.typography.labelLarge)
                                Slider(
                                    value = caps.exposureIndex.toFloat(),
                                    onValueChange = { session.setExposureIndex(it.toInt()) },
                                    valueRange = caps.exposureMin.toFloat()..caps.exposureMax.toFloat(),
                                    steps = (caps.exposureMax - caps.exposureMin - 1).coerceAtLeast(0)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

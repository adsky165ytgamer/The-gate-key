package com.example.camera.core

import android.annotation.SuppressLint
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Creates a [CameraSession] for the current composition and releases it when the tool leaves the screen. */
@Composable
fun rememberCameraSession(): CameraSession {
    val context = androidx.compose.ui.platform.LocalContext.current
    val session = remember { CameraSession(context) }
    DisposableEffect(session) { onDispose { session.release() } }
    return session
}

/**
 * Live camera preview bound to the host lifecycle.
 * Handles pinch-to-zoom and tap-to-focus (with an animated focus ring) for every tool.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun CameraPreview(
    session: CameraSession,
    config: CameraConfig,
    modifier: Modifier = Modifier,
    gesturesEnabled: Boolean = true,
    onPreviewView: (PreviewView) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    val focusProgress = remember { Animatable(0f) }
    val currentConfig by rememberUpdatedState(config)

    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).also {
                    previewView = it
                    onPreviewView(it)
                }
            }
        )

        val view = previewView
        if (view != null) {
            DisposableEffect(view, lifecycleOwner) {
                session.attach(lifecycleOwner, view, currentConfig)
                onDispose { session.detach() }
            }
        }

        if (gesturesEnabled) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(session) {
                        detectTapGestures { offset ->
                            session.focusAt(offset.x, offset.y)
                            focusPoint = offset
                        }
                    }
                    .pointerInput(session) {
                        detectTransformGestures { _, _, zoomChange, _ ->
                            if (zoomChange != 1f) {
                                val caps = session.capabilities.value
                                session.setZoomRatio(caps.zoomRatio * zoomChange)
                            }
                        }
                    }
            )
        }

        // Focus ring
        val point = focusPoint
        if (point != null) {
            LaunchedEffect(point) {
                focusProgress.snapTo(0f)
                focusProgress.animateTo(1f, androidx.compose.animation.core.tween(650))
                focusPoint = null
            }
            val ringColor = MaterialTheme.colorScheme.primary
            Canvas(Modifier.fillMaxSize()) {
                val p = focusProgress.value
                val radius = (46.dp.toPx()) * (1.35f - 0.35f * p)
                drawCircle(
                    color = ringColor.copy(alpha = 1f - 0.6f * p),
                    radius = radius,
                    center = point,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}

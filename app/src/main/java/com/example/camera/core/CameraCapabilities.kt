package com.example.camera.core

/** Snapshot of what the bound camera can do. Updated live by [CameraSession]. */
data class CameraCapabilities(
    val isBound: Boolean = false,
    val hasFlash: Boolean = false,
    val torchOn: Boolean = false,
    val zoomRatio: Float = 1f,
    val minZoomRatio: Float = 1f,
    val maxZoomRatio: Float = 1f,
    val exposureSupported: Boolean = false,
    val exposureIndex: Int = 0,
    val exposureMin: Int = 0,
    val exposureMax: Int = 0
) {
    val canZoom: Boolean get() = maxZoomRatio - minZoomRatio > 0.05f
}

package com.example.camera.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/** A point in normalized (0..1) coordinates of an upright image. */
data class Pt(val x: Float, val y: Float)

/**
 * Maps normalized coordinates of an analysed (upright) frame to view pixels for a FILL_CENTER preview,
 * so overlays line up with what the camera shows.
 */
object PreviewGeometry {
    fun toView(point: Pt, frameWidth: Int, frameHeight: Int, view: Size): Offset {
        if (frameWidth <= 0 || frameHeight <= 0 || view.width <= 0f || view.height <= 0f) return Offset.Zero
        val scale = maxOf(view.width / frameWidth, view.height / frameHeight)
        val offsetX = (view.width - frameWidth * scale) / 2f
        val offsetY = (view.height - frameHeight * scale) / 2f
        return Offset(offsetX + point.x * frameWidth * scale, offsetY + point.y * frameHeight * scale)
    }
}

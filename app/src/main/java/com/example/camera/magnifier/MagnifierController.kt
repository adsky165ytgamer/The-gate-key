package com.example.camera.magnifier

import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.View
import androidx.camera.view.PreviewView
import kotlin.math.ln
import kotlin.math.pow

/** Reading aids applied to the live preview. */
enum class ViewFilter(val label: String) {
    Normal("Normal"),
    HighContrast("High contrast"),
    Grayscale("Grayscale"),
    Inverted("Inverted");

    fun matrix(): FloatArray = when (this) {
        Normal -> floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
        HighContrast -> floatArrayOf(1.7f, 0f, 0f, 0f, -70f, 0f, 1.7f, 0f, 0f, -70f, 0f, 0f, 1.7f, 0f, -70f, 0f, 0f, 0f, 1f, 0f)
        Grayscale -> floatArrayOf(
            0.33f, 0.59f, 0.11f, 0f, 0f, 0.33f, 0.59f, 0.11f, 0f, 0f, 0.33f, 0.59f, 0.11f, 0f, 0f, 0f, 0f, 0f, 1f, 0f
        )
        Inverted -> floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f)
    }
}

/** Zoom maths and preview filtering for the magnifier. Camera hardware access stays in CameraSession. */
object MagnifierController {

    /** Slider position (0..1) to zoom ratio, spaced logarithmically so every step feels the same. */
    fun ratioFor(fraction: Float, min: Float, max: Float): Float =
        if (max <= min) min else min * (max / min).pow(fraction.coerceIn(0f, 1f))

    fun fractionFor(ratio: Float, min: Float, max: Float): Float =
        if (max <= min) 0f else (ln(ratio / min) / ln(max / min)).coerceIn(0f, 1f)

    fun applyFilter(view: PreviewView, filter: ViewFilter) {
        if (filter == ViewFilter.Normal) {
            view.setLayerType(View.LAYER_TYPE_NONE, null)
        } else {
            val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(filter.matrix()) }
            view.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
        }
    }
}

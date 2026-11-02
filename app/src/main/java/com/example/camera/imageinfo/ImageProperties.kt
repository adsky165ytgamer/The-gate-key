package com.example.camera.imageinfo

import android.net.Uri

data class Property(val label: String, val value: String)

data class PropertySection(val title: String, val items: List<Property>)

data class PaletteColor(val argb: Int, val share: Float) {
    val hex: String get() = String.format("#%06X", argb and 0xFFFFFF)
}

data class ImageAnalysisResult(
    val palette: List<PaletteColor>,
    /** 24 luminance bins, each 0..1 relative to the tallest bin. */
    val histogram: List<Float>,
    val averageBrightness: Float,
    val averageSaturation: Float,
    val hasTransparency: Boolean
)

data class ImageInfo(
    val uri: Uri,
    val displayName: String,
    val width: Int,
    val height: Int,
    val summary: List<Property>,
    val sections: List<PropertySection>,
    val technical: List<Property>,
    val analysis: ImageAnalysisResult?
) {
    fun asText(): String = buildString {
        appendLine(displayName)
        (summary + sections.flatMap { it.items }).forEach { appendLine("${it.label}: ${it.value}") }
        analysis?.palette?.takeIf { it.isNotEmpty() }?.let { appendLine("Palette: " + it.joinToString(", ") { c -> c.hex }) }
    }.trim()
}

package com.example.camera.scanner

import java.io.File

enum class ScanFilter(val label: String) {
    Original("Original"),
    Color("Color"),
    Grayscale("Grayscale"),
    BlackWhite("Black & white")
}

/** One scanned page: the captured photo, its corners, and the processing applied on top. */
data class ScanPage(
    val id: Long,
    val file: File,
    val quad: Quad,
    val filter: ScanFilter = ScanFilter.Color,
    val rotation: Int = 0
)

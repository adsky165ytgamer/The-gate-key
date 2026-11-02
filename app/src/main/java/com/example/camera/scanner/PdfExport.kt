package com.example.camera.scanner

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

/** Writes scanned pages into a multi-page PDF or a single JPEG. */
object PdfExport {

    private const val PAGE_LONG_EDGE_PX = 2400
    private const val POINTS_SHORT = 595 // A4 short edge in PostScript points

    fun writePdf(pages: List<ScanPage>, renderer: ScanRenderer, out: OutputStream) {
        val document = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        try {
            pages.forEachIndexed { index, page ->
                val bitmap = renderer.renderUncached(page, PAGE_LONG_EDGE_PX)
                    ?: error("Could not render page ${index + 1}")
                try {
                    val portrait = bitmap.height >= bitmap.width
                    val pdfW = if (portrait) POINTS_SHORT else (POINTS_SHORT * bitmap.width.toFloat() / bitmap.height).toInt().coerceAtLeast(200)
                    val pdfH = if (portrait) (POINTS_SHORT * bitmap.height.toFloat() / bitmap.width).toInt().coerceAtLeast(200) else POINTS_SHORT
                    val info = PdfDocument.PageInfo.Builder(pdfW, pdfH, index + 1).create()
                    val pdfPage = document.startPage(info)
                    pdfPage.canvas.drawBitmap(bitmap, null, Rect(0, 0, pdfW, pdfH), paint)
                    document.finishPage(pdfPage)
                } finally {
                    bitmap.recycle()
                }
            }
            document.writeTo(out)
        } finally {
            document.close()
        }
    }

    fun writeJpeg(page: ScanPage, renderer: ScanRenderer, out: OutputStream): Boolean {
        val bitmap = renderer.renderUncached(page, 3200) ?: return false
        return try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        } finally {
            bitmap.recycle()
        }
    }
}

package com.example.camera.scanner

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.LruCache
import com.example.camera.core.BitmapLoader
import kotlin.math.max

/** Turns a [ScanPage] into its final bitmap (perspective-corrected, filtered, rotated) and caches previews. */
class ScanRenderer {

    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    /** Cached render for on-screen use. The returned bitmap must not be recycled by the caller. */
    fun preview(page: ScanPage, maxEdge: Int): Bitmap? {
        val key = "${page.id}|${page.quad}|${page.filter}|${page.rotation}|$maxEdge"
        cache.get(key)?.let { return it }
        val bitmap = renderUncached(page, maxEdge) ?: return null
        cache.put(key, bitmap)
        return bitmap
    }

    /** Fresh render (for export). The caller owns the bitmap. */
    fun renderUncached(page: ScanPage, maxEdge: Int): Bitmap? {
        val sourceEdge = (maxEdge * 1.6f).toInt().coerceIn(900, 4096)
        val source = BitmapLoader.decode(page.file, sourceEdge) ?: return null
        val warped = PerspectiveCorrection.warp(source, page.quad, maxEdge)
        if (warped !== source) source.recycle()
        val filtered = ScanProcessing.apply(warped, page.filter)
        if (filtered !== warped) warped.recycle()
        return rotate(filtered, page.rotation)
    }

    fun evict(pageId: Long) {
        val prefix = "$pageId|"
        cache.snapshot().keys.filter { it.startsWith(prefix) }.forEach { cache.remove(it) }
    }

    fun clear() = cache.evictAll()

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        val d = ((degrees % 360) + 360) % 360
        if (d == 0) return bitmap
        val matrix = Matrix().apply { postRotate(d.toFloat()) }
        val out = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (out !== bitmap) bitmap.recycle()
        return out
    }
}

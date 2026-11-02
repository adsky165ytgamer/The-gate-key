package com.example.camera.imageinfo

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
import kotlin.math.sqrt

/** Palette extraction, luminance histogram and basic colour statistics. */
object ImageColorAnalysis {

    private const val SAMPLE_EDGE = 128
    private const val HISTOGRAM_BINS = 24

    fun analyze(source: Bitmap, hasAlphaChannel: Boolean): ImageAnalysisResult {
        val scale = SAMPLE_EDGE.toFloat() / max(source.width, source.height)
        val w = max(1, (source.width * scale).toInt().coerceAtMost(source.width))
        val h = max(1, (source.height * scale).toInt().coerceAtMost(source.height))
        val sample = if (scale < 1f) Bitmap.createScaledBitmap(source, w, h, true) else source
        val pixels = IntArray(sample.width * sample.height)
        sample.getPixels(pixels, 0, sample.width, 0, 0, sample.width, sample.height)
        if (sample !== source) sample.recycle()

        val bins = HashMap<Int, IntArray>() // 4 bits per channel -> [count, r, g, b]
        val histogram = IntArray(HISTOGRAM_BINS)
        var transparent = false
        var lumSum = 0.0
        var satSum = 0.0
        var counted = 0
        val hsv = FloatArray(3)

        for (p in pixels) {
            val a = Color.alpha(p)
            if (a < 250) transparent = true
            if (a < 32) continue
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)
            val lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
            lumSum += lum
            histogram[((lum / 256.0) * HISTOGRAM_BINS).toInt().coerceIn(0, HISTOGRAM_BINS - 1)]++
            Color.RGBToHSV(r, g, b, hsv)
            satSum += hsv[1]
            counted++
            val key = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
            val e = bins.getOrPut(key) { IntArray(4) }
            e[0]++; e[1] += r; e[2] += g; e[3] += b
        }
        if (counted == 0) return ImageAnalysisResult(emptyList(), List(HISTOGRAM_BINS) { 0f }, 0f, 0f, transparent)

        // Pick the most frequent colours that are visibly different from the ones already chosen.
        val ranked = bins.values.sortedByDescending { it[0] }
        val chosen = ArrayList<PaletteColor>()
        for (e in ranked) {
            val r = e[1] / e[0]
            val g = e[2] / e[0]
            val b = e[3] / e[0]
            val tooClose = chosen.any {
                val dr = Color.red(it.argb) - r
                val dg = Color.green(it.argb) - g
                val db = Color.blue(it.argb) - b
                sqrt((dr * dr + dg * dg + db * db).toDouble()) < 56.0
            }
            if (!tooClose) chosen += PaletteColor(Color.rgb(r, g, b), e[0].toFloat() / counted)
            if (chosen.size == 6) break
        }

        val tallest = max(1, histogram.max())
        return ImageAnalysisResult(
            palette = chosen,
            histogram = histogram.map { it.toFloat() / tallest },
            averageBrightness = (lumSum / counted / 255.0).toFloat(),
            averageSaturation = (satSum / counted).toFloat(),
            hasTransparency = transparent
        )
    }
}

package com.example.camera.recognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import com.example.camera.core.BitmapLoader
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer

/** Finds a QR code or barcode in a still image (gallery pick). */
object ImageBarcodeDecoder {
    fun decode(context: Context, uri: Uri): RecognitionResult? {
        // BitmapLoader applies EXIF orientation before decoding, so gallery images enter upright.
        val bitmap = BitmapLoader.decode(context, uri, 2048) ?: return null
        return try {
            decode(bitmap) ?: decodeRotations(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeRotations(bitmap: Bitmap): RecognitionResult? {
        for (degrees in intArrayOf(90, 180, 270)) {
            val rotated = runCatching {
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
            }.getOrNull() ?: continue
            try {
                decode(rotated)?.let { return it }
            } finally {
                if (rotated !== bitmap) rotated.recycle()
            }
        }
        return null
    }

    private fun decode(bitmap: Bitmap): RecognitionResult? {
        if (bitmap.width <= 0 || bitmap.height <= 0) return null
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        val reader = MultiFormatReader().apply {
            setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to ALL_FORMATS, DecodeHintType.TRY_HARDER to true))
        }
        val attempts = listOf(
            { BinaryBitmap(HybridBinarizer(source)) },
            { BinaryBitmap(GlobalHistogramBinarizer(source)) },
            { BinaryBitmap(HybridBinarizer(source.invert())) }
        )
        for (attempt in attempts) {
            try {
                val result = reader.decodeWithState(attempt())
                return RecognitionResult(result.text.orEmpty(), RecognitionResult.prettyFormat(result.barcodeFormat))
            } catch (_: Exception) {
                // Unsupported or unreadable images are a normal no-result path.
            } finally {
                reader.reset()
            }
        }
        return null
    }
}

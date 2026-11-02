package com.example.camera.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream
import kotlin.math.max

/** Decodes images at a bounded size and applies their EXIF orientation, so results are always upright. */
object BitmapLoader {

    fun decode(context: Context, uri: Uri, maxEdge: Int): Bitmap? = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val bitmap = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions(bounds, maxEdge))
        } ?: return@runCatching null
        val orientation = resolver.openInputStream(uri)?.use { readOrientation(it) } ?: ExifInterface.ORIENTATION_NORMAL
        finish(bitmap, orientation, maxEdge)
    }.getOrNull()

    fun decode(file: File, maxEdge: Int): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions(bounds, maxEdge)) ?: return@runCatching null
        val orientation = file.inputStream().use { readOrientation(it) }
        finish(bitmap, orientation, maxEdge)
    }.getOrNull()

    /** Dimensions of the image as displayed (EXIF rotation applied), without decoding pixels. */
    fun uprightSize(file: File): Pair<Int, Int>? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val orientation = file.inputStream().use { readOrientation(it) }
        if (isSideways(orientation)) bounds.outHeight to bounds.outWidth else bounds.outWidth to bounds.outHeight
    }.getOrNull()

    private fun decodeOptions(bounds: BitmapFactory.Options, maxEdge: Int): BitmapFactory.Options {
        var sample = 1
        val longest = max(bounds.outWidth, bounds.outHeight)
        while (longest / (sample * 2) >= maxEdge) sample *= 2
        return BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    }

    private fun readOrientation(stream: InputStream): Int = runCatching {
        ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    private fun isSideways(orientation: Int) = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90,
        ExifInterface.ORIENTATION_ROTATE_270,
        ExifInterface.ORIENTATION_TRANSPOSE,
        ExifInterface.ORIENTATION_TRANSVERSE -> true
        else -> false
    }

    private fun finish(source: Bitmap, orientation: Int, maxEdge: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
        }
        val longest = max(source.width, source.height)
        if (longest > maxEdge) {
            val s = maxEdge.toFloat() / longest
            matrix.postScale(s, s)
        }
        if (matrix.isIdentity) return source
        val out = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (out !== source) source.recycle()
        return out
    }
}

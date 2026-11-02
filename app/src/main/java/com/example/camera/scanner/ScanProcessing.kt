package com.example.camera.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/** Document-friendly image processing. */
object ScanProcessing {

    fun apply(source: Bitmap, filter: ScanFilter): Bitmap {
        if (filter == ScanFilter.Original) return source
        if (OpenCvRuntime.ensureLoaded()) {
            runCatching { return applyWithOpenCv(source, filter) }
        }
        return applyWithColorMatrix(source, filter)
    }

    private fun applyWithOpenCv(source: Bitmap, filter: ScanFilter): Bitmap {
        val rgba = Mat()
        val out = Mat()
        try {
            Utils.bitmapToMat(source, rgba)
            when (filter) {
                ScanFilter.Color -> enhanceColor(rgba, out)
                ScanFilter.Grayscale -> grayscale(rgba, out)
                ScanFilter.BlackWhite -> blackWhite(rgba, out)
                ScanFilter.Original -> rgba.copyTo(out)
            }
            val result = Bitmap.createBitmap(out.cols(), out.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(out, result)
            return result
        } finally {
            rgba.release()
            out.release()
        }
    }

    /** Local contrast on lightness only, so colours stay natural while text gets crisper. */
    private fun enhanceColor(rgba: Mat, out: Mat) {
        val rgb = Mat()
        val lab = Mat()
        val channels = ArrayList<Mat>()
        val boosted = Mat()
        try {
            Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
            Imgproc.cvtColor(rgb, lab, Imgproc.COLOR_RGB2Lab)
            Core.split(lab, channels)
            val clahe = Imgproc.createCLAHE(2.2, Size(8.0, 8.0))
            clahe.apply(channels[0], channels[0])
            Core.merge(channels, lab)
            Imgproc.cvtColor(lab, rgb, Imgproc.COLOR_Lab2RGB)
            rgb.convertTo(boosted, -1, 1.08, 4.0)
            Imgproc.cvtColor(boosted, out, Imgproc.COLOR_RGB2RGBA)
        } finally {
            rgb.release(); lab.release(); boosted.release()
            channels.forEach { it.release() }
        }
    }

    /** Removes uneven lighting and shadows by dividing by an estimate of the paper background. */
    private fun normalizedGray(rgba: Mat, normalized: Mat) {
        val gray = Mat()
        val small = Mat()
        val background = Mat()
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
        try {
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.resize(gray, small, Size(), 0.125, 0.125, Imgproc.INTER_AREA)
            Imgproc.dilate(small, small, kernel) // drops dark ink so only paper is left
            Imgproc.GaussianBlur(small, small, Size(0.0, 0.0), 3.0)
            Imgproc.resize(small, background, gray.size(), 0.0, 0.0, Imgproc.INTER_LINEAR)
            Core.divide(gray, background, normalized, 255.0)
        } finally {
            gray.release(); small.release(); background.release(); kernel.release()
        }
    }

    private fun grayscale(rgba: Mat, out: Mat) {
        val normalized = Mat()
        try {
            normalizedGray(rgba, normalized)
            Imgproc.cvtColor(normalized, out, Imgproc.COLOR_GRAY2RGBA)
        } finally {
            normalized.release()
        }
    }

    private fun blackWhite(rgba: Mat, out: Mat) {
        val normalized = Mat()
        val binary = Mat()
        try {
            normalizedGray(rgba, normalized)
            Imgproc.threshold(normalized, binary, 0.0, 255.0, Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU)
            Imgproc.cvtColor(binary, out, Imgproc.COLOR_GRAY2RGBA)
        } finally {
            normalized.release(); binary.release()
        }
    }

    private fun applyWithColorMatrix(source: Bitmap, filter: ScanFilter): Bitmap {
        val matrix = ColorMatrix()
        when (filter) {
            ScanFilter.Color -> matrix.set(contrast(1.25f, -18f))
            ScanFilter.Grayscale -> {
                matrix.setSaturation(0f)
                matrix.postConcat(ColorMatrix(contrast(1.3f, -22f)))
            }
            ScanFilter.BlackWhite -> {
                matrix.setSaturation(0f)
                matrix.postConcat(ColorMatrix(contrast(3.2f, -330f)))
            }
            ScanFilter.Original -> Unit
        }
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(matrix) }
        Canvas(out).drawBitmap(source, 0f, 0f, paint)
        return out
    }

    private fun contrast(scale: Float, shift: Float) = floatArrayOf(
        scale, 0f, 0f, 0f, shift,
        0f, scale, 0f, 0f, shift,
        0f, 0f, scale, 0f, shift,
        0f, 0f, 0f, 1f, 0f
    )
}

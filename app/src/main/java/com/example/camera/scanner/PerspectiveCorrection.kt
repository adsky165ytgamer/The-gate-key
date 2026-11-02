package com.example.camera.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.geometry.Geometry
import org.opencv.imgproc.Imgproc
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

/** Flattens a photographed page into a straight, rectangular image. */
object PerspectiveCorrection {

    fun warp(source: Bitmap, quad: Quad, maxEdge: Int): Bitmap {
        val w = source.width.toFloat()
        val h = source.height.toFloat()
        val p = quad.points().map { it.x * w to it.y * h }

        val top = hypot(p[1].first - p[0].first, p[1].second - p[0].second)
        val bottom = hypot(p[2].first - p[3].first, p[2].second - p[3].second)
        val left = hypot(p[3].first - p[0].first, p[3].second - p[0].second)
        val right = hypot(p[2].first - p[1].first, p[2].second - p[1].second)
        var outW = max(top, bottom)
        var outH = max(left, right)
        val longest = max(outW, outH)
        if (longest > maxEdge) {
            val s = maxEdge / longest
            outW *= s
            outH *= s
        }
        val width = outW.roundToInt().coerceAtLeast(32)
        val height = outH.roundToInt().coerceAtLeast(32)

        if (OpenCvRuntime.ensureLoaded()) {
            runCatching { return warpWithOpenCv(source, p, width, height) }
        }
        return warpWithMatrix(source, p, width, height)
    }

    private fun warpWithOpenCv(source: Bitmap, p: List<Pair<Float, Float>>, width: Int, height: Int): Bitmap {
        val src = Mat()
        val dst = Mat()
        val srcPts = MatOfPoint2f(*p.map { Point(it.first.toDouble(), it.second.toDouble()) }.toTypedArray())
        val dstPts = MatOfPoint2f(
            Point(0.0, 0.0), Point(width - 1.0, 0.0), Point(width - 1.0, height - 1.0), Point(0.0, height - 1.0)
        )
        var transform: Mat? = null
        try {
            Utils.bitmapToMat(source, src)
            transform = Geometry.getPerspectiveTransform(srcPts, dstPts)
            Imgproc.warpPerspective(src, dst, transform, Size(width.toDouble(), height.toDouble()), Imgproc.INTER_CUBIC)
            val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(dst, out)
            return out
        } finally {
            src.release()
            dst.release()
            srcPts.release()
            dstPts.release()
            transform?.release()
        }
    }

    /** Pure-Android fallback (used only if the OpenCV native library cannot load). */
    private fun warpWithMatrix(source: Bitmap, p: List<Pair<Float, Float>>, width: Int, height: Int): Bitmap {
        val srcArr = floatArrayOf(p[0].first, p[0].second, p[1].first, p[1].second, p[2].first, p[2].second, p[3].first, p[3].second)
        val dstArr = floatArrayOf(0f, 0f, width.toFloat(), 0f, width.toFloat(), height.toFloat(), 0f, height.toFloat())
        val matrix = Matrix()
        matrix.setPolyToPoly(srcArr, 0, dstArr, 0, 4)
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(source, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }
}

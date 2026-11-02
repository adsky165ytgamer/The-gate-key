package com.example.camera.scanner

import android.graphics.Bitmap
import com.example.camera.core.Pt
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.geometry.Geometry
import org.opencv.imgproc.Imgproc
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Loads the OpenCV native library once. */
object OpenCvRuntime {
    private var state: Boolean? = null

    @Synchronized
    fun ensureLoaded(): Boolean {
        state?.let { return it }
        val ok = try {
            System.loadLibrary("opencv_java5")
            true
        } catch (t: Throwable) {
            false
        }
        state = ok
        return ok
    }
}

/** Finds the page boundary in an image using edge detection and contour analysis (OpenCV). */
class DocumentDetector {

    fun detect(bitmap: Bitmap): Quad? {
        if (!OpenCvRuntime.ensureLoaded()) return null
        val rgba = Mat()
        val gray = Mat()
        return try {
            Utils.bitmapToMat(bitmap, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            detectGray(gray)
        } catch (t: Throwable) {
            null
        } finally {
            rgba.release()
            gray.release()
        }
    }

    /** [gray] must be an upright 8-bit single channel image. */
    fun detectGray(gray: Mat): Quad? {
        if (!OpenCvRuntime.ensureLoaded() || gray.empty()) return null
        val longest = max(gray.cols(), gray.rows()).toDouble()
        val scale = if (longest > WORK_EDGE) WORK_EDGE / longest else 1.0
        val small = Mat()
        val blurred = Mat()
        try {
            if (scale < 1.0) Imgproc.resize(gray, small, Size(), scale, scale, Imgproc.INTER_AREA) else gray.copyTo(small)
            Imgproc.GaussianBlur(small, blurred, Size(5.0, 5.0), 0.0)
            val w = small.cols()
            val h = small.rows()
            val corners = fromEdges(blurred, w, h) ?: fromThreshold(blurred, w, h) ?: return null
            return orderAndNormalize(corners, w, h)
        } finally {
            small.release()
            blurred.release()
        }
    }

    private fun fromEdges(blurred: Mat, w: Int, h: Int): List<Point>? {
        val edges = Mat()
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        return try {
            val mean = Core.mean(blurred).`val`[0]
            val low = (0.5 * mean).coerceIn(20.0, 80.0)
            Imgproc.Canny(blurred, edges, low, low * 2.5)
            Imgproc.dilate(edges, edges, kernel, Point(-1.0, -1.0), 2)
            bestQuad(edges, w, h)
        } finally {
            edges.release()
            kernel.release()
        }
    }

    private fun fromThreshold(blurred: Mat, w: Int, h: Int): List<Point>? {
        val mask = Mat()
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(7.0, 7.0))
        return try {
            Imgproc.threshold(blurred, mask, 0.0, 255.0, Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU)
            Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel)
            bestQuad(mask, w, h) ?: run {
                Core.bitwise_not(mask, mask)
                bestQuad(mask, w, h)
            }
        } finally {
            mask.release()
            kernel.release()
        }
    }

    private fun bestQuad(binary: Mat, w: Int, h: Int): List<Point>? {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        val work = binary.clone()
        try {
            Imgproc.findContours(work, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
            val imageArea = w.toDouble() * h
            val candidates = contours
                .map { it to Geometry.contourArea(it) }
                .filter { it.second > imageArea * MIN_AREA_RATIO && it.second < imageArea * MAX_AREA_RATIO }
                .sortedByDescending { it.second }
                .take(8)

            for ((contour, _) in candidates) {
                val c2f = MatOfPoint2f()
                contour.convertTo(c2f, CvType.CV_32FC2)
                val perimeter = Geometry.arcLength(c2f, true)
                for (epsilon in EPSILONS) {
                    val approx = MatOfPoint2f()
                    Geometry.approxPolyDP(c2f, approx, epsilon * perimeter, true)
                    val points = approx.toArray()
                    approx.release()
                    if (points.size == 4 && isPlausible(points.toList(), w, h)) {
                        c2f.release()
                        return points.toList()
                    }
                }
                c2f.release()
            }
            return null
        } finally {
            contours.forEach { it.release() }
            hierarchy.release()
            work.release()
        }
    }

    private fun isPlausible(p: List<Point>, w: Int, h: Int): Boolean {
        val minSide = min(w, h) * MIN_SIDE_RATIO
        var sign = 0
        for (i in 0 until 4) {
            val a = p[i]
            val b = p[(i + 1) % 4]
            val c = p[(i + 2) % 4]
            if (hypot(b.x - a.x, b.y - a.y) < minSide) return false
            val abx = a.x - b.x
            val aby = a.y - b.y
            val cbx = c.x - b.x
            val cby = c.y - b.y
            val cross = (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
            val s = if (cross > 0) 1 else -1
            if (sign == 0) sign = s else if (s != sign) return false // not convex
            val denom = hypot(abx, aby) * hypot(cbx, cby)
            if (denom == 0.0) return false
            val angle = Math.toDegrees(acos(((abx * cbx + aby * cby) / denom).coerceIn(-1.0, 1.0)))
            if (angle < 55.0 || angle > 125.0) return false
        }
        return true
    }

    private fun orderAndNormalize(points: List<Point>, w: Int, h: Int): Quad {
        val cx = points.map { it.x }.average()
        val cy = points.map { it.y }.average()
        var ordered = points.sortedBy { atan2(it.y - cy, it.x - cx) }.toMutableList()
        fun area(ps: List<Point>): Double = ps.indices.sumOf { i ->
            val a = ps[i]
            val b = ps[(i + 1) % ps.size]
            a.x * b.y - a.y * b.x
        }
        if (area(ordered) < 0.0) ordered = ordered.asReversed().toMutableList()
        val start = ordered.indices.minByOrNull { ordered[it].x + ordered[it].y } ?: 0
        ordered = (ordered.drop(start) + ordered.take(start)).toMutableList()
        fun n(p: Point) = Pt((p.x / w).toFloat().coerceIn(0f, 1f), (p.y / h).toFloat().coerceIn(0f, 1f))
        return Quad(n(ordered[0]), n(ordered[1]), n(ordered[2]), n(ordered[3]))
    }

    private companion object {
        const val WORK_EDGE = 480.0
        const val MIN_AREA_RATIO = 0.15
        const val MAX_AREA_RATIO = 0.985
        const val MIN_SIDE_RATIO = 0.12
        val EPSILONS = doubleArrayOf(0.02, 0.035, 0.05)
    }
}

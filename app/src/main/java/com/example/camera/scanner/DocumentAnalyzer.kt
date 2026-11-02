package com.example.camera.scanner

import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat

/** Camera frame analyzer that reports the detected document quad about 6-7 times per second. */
class DocumentAnalyzer(
    private val onResult: (quad: Quad?, frameWidth: Int, frameHeight: Int) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector = DocumentDetector()
    private var lastRun = 0L

    override fun analyze(image: ImageProxy) {
        try {
            val now = SystemClock.elapsedRealtime()
            if (now - lastRun < MIN_INTERVAL_MS || !OpenCvRuntime.ensureLoaded()) return
            lastRun = now

            val w = image.width
            val h = image.height
            val plane = image.planes[0]
            val buffer = plane.buffer
            val rowStride = plane.rowStride
            val bytes = ByteArray(w * h)
            for (row in 0 until h) {
                buffer.position(row * rowStride)
                buffer.get(bytes, row * w, w)
            }

            val source = Mat(h, w, CvType.CV_8UC1)
            source.put(0, 0, bytes)
            val upright = Mat()
            val rotateCode = when (image.imageInfo.rotationDegrees) {
                90 -> Core.ROTATE_90_CLOCKWISE
                180 -> Core.ROTATE_180
                270 -> Core.ROTATE_90_COUNTERCLOCKWISE
                else -> -1
            }
            if (rotateCode >= 0) Core.rotate(source, upright, rotateCode) else source.copyTo(upright)

            val quad = try {
                detector.detectGray(upright)
            } finally {
                source.release()
            }
            val fw = upright.cols()
            val fh = upright.rows()
            upright.release()
            onResult(quad, fw, fh)
        } catch (_: Throwable) {
            // A bad frame must never crash the analysis thread.
        } finally {
            image.close()
        }
    }

    private companion object {
        const val MIN_INTERVAL_MS = 150L
    }
}

/** Smooths detections over time and measures how long the document has been held steady. */
class QuadStabilizer {
    var smoothed: Quad? = null
        private set
    var stableFrames = 0
        private set
    private var missing = 0

    fun update(detected: Quad?): Quad? {
        if (detected == null) {
            missing++
            if (missing >= 4) {
                smoothed = null
                stableFrames = 0
            }
            return smoothed
        }
        missing = 0
        val previous = smoothed
        if (previous == null) {
            smoothed = detected
            stableFrames = 1
        } else {
            val d = previous.distanceTo(detected)
            when {
                d < 0.025f -> { stableFrames++; smoothed = previous.lerp(detected, 0.4f) }
                d < 0.12f -> { stableFrames = 0; smoothed = previous.lerp(detected, 0.6f) }
                else -> { stableFrames = 0; smoothed = detected }
            }
        }
        return smoothed
    }

    fun reset() {
        smoothed = null
        stableFrames = 0
        missing = 0
    }
}

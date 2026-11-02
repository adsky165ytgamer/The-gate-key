package com.example.camera.recognition

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.camera.core.Pt
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer

internal val ALL_FORMATS: List<BarcodeFormat> = listOf(
    BarcodeFormat.QR_CODE, BarcodeFormat.DATA_MATRIX, BarcodeFormat.AZTEC, BarcodeFormat.PDF_417,
    BarcodeFormat.EAN_13, BarcodeFormat.EAN_8, BarcodeFormat.UPC_A, BarcodeFormat.UPC_E,
    BarcodeFormat.CODE_39, BarcodeFormat.CODE_93, BarcodeFormat.CODE_128, BarcodeFormat.ITF,
    BarcodeFormat.CODABAR, BarcodeFormat.RSS_14, BarcodeFormat.RSS_EXPANDED
)

/** Reads QR codes and barcodes from live camera frames using ZXing. */
class BarcodeAnalyzer(
    private val onResult: (RecognitionResult) -> Unit
) : ImageAnalysis.Analyzer {

    @Volatile var enabled: Boolean = true

    private val fastReader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to ALL_FORMATS))
    }
    private val thoroughReader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to ALL_FORMATS, DecodeHintType.TRY_HARDER to true))
    }
    private var frame = 0

    override fun analyze(image: ImageProxy) {
        try {
            if (!enabled) return
            frame++
            val w = image.width
            val h = image.height
            val plane = image.planes[0]
            val buffer = plane.buffer
            val stride = plane.rowStride
            val luma = ByteArray(w * h)
            for (row in 0 until h) {
                buffer.position(row * stride)
                buffer.get(luma, row * w, w)
            }
            val rotation = image.imageInfo.rotationDegrees
            val (data, uw, uh) = rotate(luma, w, h, rotation)
            val source = PlanarYUVLuminanceSource(data, uw, uh, 0, 0, uw, uh, false)

            var result = decode(fastReader, source)
            if (result == null && frame % 3 == 0) result = decode(thoroughReader, source)
            if (result == null && frame % 5 == 0) result = decode(fastReader, source.invert())
            if (result != null && enabled) {
                val pts = result.resultPoints?.map { Pt((it.x / uw).coerceIn(0f, 1f), (it.y / uh).coerceIn(0f, 1f)) }.orEmpty()
                onResult(
                    RecognitionResult(
                        text = result.text.orEmpty(),
                        format = RecognitionResult.prettyFormat(result.barcodeFormat),
                        points = pts,
                        frameWidth = uw,
                        frameHeight = uh
                    )
                )
            }
        } catch (_: Throwable) {
            // Skip bad frames.
        } finally {
            image.close()
        }
    }

    private fun decode(reader: MultiFormatReader, source: LuminanceSource): Result? {
        for (binarizer in 0..1) {
            val bitmap = BinaryBitmap(if (binarizer == 0) HybridBinarizer(source) else GlobalHistogramBinarizer(source))
            try {
                return reader.decodeWithState(bitmap)
            } catch (_: Exception) {
                // not found / checksum / format: try next strategy
            } finally {
                reader.reset()
            }
        }
        return null
    }

    private fun rotate(src: ByteArray, w: Int, h: Int, degrees: Int): Triple<ByteArray, Int, Int> = when (degrees) {
        90 -> {
            val out = ByteArray(src.size)
            for (y in 0 until h) for (x in 0 until w) out[x * h + (h - 1 - y)] = src[y * w + x]
            Triple(out, h, w)
        }
        180 -> Triple(src.reversedArray(), w, h)
        270 -> {
            val out = ByteArray(src.size)
            for (y in 0 until h) for (x in 0 until w) out[(w - 1 - x) * h + y] = src[y * w + x]
            Triple(out, h, w)
        }
        else -> Triple(src, w, h)
    }
}

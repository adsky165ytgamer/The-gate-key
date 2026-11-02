package com.example.camera.imageinfo

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.exifinterface.media.ExifInterface
import com.example.camera.core.BitmapLoader
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Reads file facts, image properties and EXIF metadata from any image the user picks. No camera involved. */
object ImageMetadataReader {

    fun read(context: Context, uri: Uri): ImageInfo? {
        val resolver = context.contentResolver
        var name = uri.lastPathSegment ?: "Image"
        var size = -1L
        var modified = -1L
        runCatching {
            resolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = c.getString(it) ?: name }
                    c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { if (!c.isNull(it)) size = c.getLong(it) }
                    c.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED).takeIf { it >= 0 }?.let { if (!c.isNull(it)) modified = c.getLong(it) }
                }
            }
        }
        if (size < 0) size = runCatching { resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull() ?: -1L

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching { resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val mime = bounds.outMimeType ?: resolver.getType(uri) ?: "unknown"
        val exif = runCatching { resolver.openInputStream(uri)?.use { ExifInterface(it) } }.getOrNull()
        val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) ?: ExifInterface.ORIENTATION_NORMAL
        val sideways = orientation in listOf(
            ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSPOSE, ExifInterface.ORIENTATION_TRANSVERSE
        )
        val width = if (sideways) bounds.outHeight else bounds.outWidth
        val height = if (sideways) bounds.outWidth else bounds.outHeight

        val megapixels = width.toDouble() * height / 1_000_000.0
        val summary = listOf(
            Property("Dimensions", "$width × $height px"),
            Property("File size", if (size >= 0) Formatter.formatFileSize(context, size) else "Unknown"),
            Property("Format", formatName(mime))
        )

        val general = mutableListOf(
            Property("File name", name),
            Property("Resolution", String.format(Locale.getDefault(), "%.1f megapixels", megapixels)),
            Property("Aspect ratio", aspectRatio(width, height)),
            Property("Orientation", orientationName(orientation)),
            Property("MIME type", mime)
        )
        if (size >= 0) general += Property("Exact size", String.format(Locale.getDefault(), "%,d bytes", size))
        if (modified > 0) general += Property("File modified", DateFormat.getDateTimeInstance().format(Date(modified)))

        val sections = mutableListOf(PropertySection("General", general))
        val technical = mutableListOf<Property>()

        if (exif != null) {
            val make = exif.getAttribute(ExifInterface.TAG_MAKE)
            val model = exif.getAttribute(ExifInterface.TAG_MODEL)
            val camera = listOfNotNull(
                make?.let { Property("Make", it) },
                model?.let { Property("Model", it) },
                exif.getAttribute(ExifInterface.TAG_LENS_MODEL)?.let { Property("Lens", it) },
                exif.getAttribute(ExifInterface.TAG_SOFTWARE)?.let { Property("Software", it) }
            )
            if (camera.isNotEmpty()) sections += PropertySection("Camera", camera)

            val capture = listOfNotNull(
                exposureTime(exif)?.let { Property("Shutter speed", it) },
                exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0).takeIf { it > 0 }?.let { Property("Aperture", "f/${trim(it)}") },
                exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, -1).takeIf { it > 0 }?.let { Property("ISO", "$it") },
                exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0).takeIf { it > 0 }?.let { Property("Focal length", "${trim(it)} mm") },
                exif.getAttributeInt(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, -1).takeIf { it > 0 }?.let { Property("35 mm equivalent", "$it mm") },
                exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_BIAS_VALUE, Double.NaN).takeIf { !it.isNaN() }?.let { Property("Exposure compensation", "${if (it > 0) "+" else ""}${trim(it)} EV") },
                exif.getAttributeInt(ExifInterface.TAG_FLASH, -1).takeIf { it >= 0 }?.let { Property("Flash", if (it and 1 == 1) "Fired" else "Did not fire") },
                exif.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, -1).takeIf { it >= 0 }?.let { Property("White balance", if (it == 0) "Auto" else "Manual") },
                exif.getAttributeInt(ExifInterface.TAG_METERING_MODE, -1).takeIf { it > 0 }?.let { Property("Metering", meteringName(it)) }
            )
            if (capture.isNotEmpty()) sections += PropertySection("Exposure", capture)

            val taken = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            val dates = listOfNotNull(
                taken?.let { Property("Captured", prettyExifDate(it)) },
                exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)?.let { Property("Digitized", prettyExifDate(it)) },
                exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL)?.let { Property("Time zone offset", it) }
            )
            if (dates.isNotEmpty()) sections += PropertySection("Date", dates)

            @Suppress("DEPRECATION")
            val latLong = exif.latLong
            if (latLong != null) {
                val loc = mutableListOf(
                    Property("Latitude", String.format(Locale.US, "%.6f", latLong[0])),
                    Property("Longitude", String.format(Locale.US, "%.6f", latLong[1]))
                )
                val alt = exif.getAltitude(Double.NaN)
                if (!alt.isNaN()) loc += Property("Altitude", "${alt.roundToInt()} m")
                sections += PropertySection("Location", loc)
            }

            val xRes = exif.getAttributeDouble(ExifInterface.TAG_X_RESOLUTION, 0.0)
            val colorSpace = exif.getAttributeInt(ExifInterface.TAG_COLOR_SPACE, -1)
            val tech = listOfNotNull(
                xRes.takeIf { it > 0 }?.let { Property("Resolution density", "${trim(it)} per inch") },
                colorSpace.takeIf { it > 0 }?.let { Property("Colour space", if (it == 1) "sRGB" else "Uncalibrated") },
                exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)?.let { Property("Description", it) },
                exif.getAttribute(ExifInterface.TAG_ARTIST)?.let { Property("Artist", it) },
                exif.getAttribute(ExifInterface.TAG_COPYRIGHT)?.let { Property("Copyright", it) }
            )
            technical += tech
            technical += allTags(exif)
        }

        val analysis = runCatching {
            BitmapLoader.decode(context, uri, 256)?.let { bmp ->
                ImageColorAnalysis.analyze(bmp, bmp.hasAlpha()).also { bmp.recycle() }
            }
        }.getOrNull()

        return ImageInfo(uri, name, width, height, summary, sections, technical.distinctBy { it.label to it.value }, analysis)
    }

    private val rawTags = listOf(
        ExifInterface.TAG_IMAGE_WIDTH, ExifInterface.TAG_IMAGE_LENGTH, ExifInterface.TAG_PIXEL_X_DIMENSION,
        ExifInterface.TAG_PIXEL_Y_DIMENSION, ExifInterface.TAG_BITS_PER_SAMPLE, ExifInterface.TAG_COMPRESSION,
        ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION, ExifInterface.TAG_SAMPLES_PER_PIXEL, ExifInterface.TAG_Y_CB_CR_POSITIONING,
        ExifInterface.TAG_RESOLUTION_UNIT, ExifInterface.TAG_EXPOSURE_PROGRAM, ExifInterface.TAG_EXPOSURE_MODE,
        ExifInterface.TAG_SCENE_CAPTURE_TYPE, ExifInterface.TAG_SENSING_METHOD, ExifInterface.TAG_LIGHT_SOURCE,
        ExifInterface.TAG_DIGITAL_ZOOM_RATIO, ExifInterface.TAG_SUBJECT_DISTANCE_RANGE, ExifInterface.TAG_CONTRAST,
        ExifInterface.TAG_SATURATION, ExifInterface.TAG_SHARPNESS, ExifInterface.TAG_BRIGHTNESS_VALUE,
        ExifInterface.TAG_MAX_APERTURE_VALUE, ExifInterface.TAG_SHUTTER_SPEED_VALUE, ExifInterface.TAG_APERTURE_VALUE,
        ExifInterface.TAG_SUBSEC_TIME, ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, ExifInterface.TAG_EXIF_VERSION,
        ExifInterface.TAG_FLASHPIX_VERSION, ExifInterface.TAG_COMPONENTS_CONFIGURATION, ExifInterface.TAG_LENS_MAKE,
        ExifInterface.TAG_USER_COMMENT,
        ExifInterface.TAG_GPS_PROCESSING_METHOD, ExifInterface.TAG_GPS_DATESTAMP
    )

    private fun allTags(exif: ExifInterface): List<Property> = rawTags.mapNotNull { tag ->
        exif.getAttribute(tag)?.takeIf { it.isNotBlank() && it.length < 200 }?.let { Property(tag.replace(Regex("([a-z])([A-Z])"), "$1 $2"), it) }
    }

    private fun exposureTime(exif: ExifInterface): String? {
        val t = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
        if (t <= 0) return null
        return if (t >= 1) "${trim(t)} s" else "1/${(1.0 / t).roundToInt()} s"
    }

    private fun trim(v: Double): String =
        if (abs(v - v.roundToInt()) < 0.005) "${v.roundToInt()}" else String.format(Locale.getDefault(), "%.1f", v)

    private fun aspectRatio(w: Int, h: Int): String {
        fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
        val g = gcd(w, h).coerceAtLeast(1)
        val rw = w / g
        val rh = h / g
        return if (rw > 30 || rh > 30) String.format(Locale.getDefault(), "%.2f : 1", w.toDouble() / h) else "$rw : $rh"
    }

    private fun formatName(mime: String) = when (mime.lowercase()) {
        "image/jpeg" -> "JPEG"
        "image/png" -> "PNG"
        "image/webp" -> "WebP"
        "image/heic", "image/heif" -> "HEIF"
        "image/gif" -> "GIF"
        "image/bmp" -> "BMP"
        "image/avif" -> "AVIF"
        else -> mime.substringAfter('/').uppercase()
    }

    private fun orientationName(o: Int) = when (o) {
        ExifInterface.ORIENTATION_ROTATE_90 -> "Rotated 90°"
        ExifInterface.ORIENTATION_ROTATE_180 -> "Rotated 180°"
        ExifInterface.ORIENTATION_ROTATE_270 -> "Rotated 270°"
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> "Mirrored"
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> "Flipped"
        ExifInterface.ORIENTATION_TRANSPOSE, ExifInterface.ORIENTATION_TRANSVERSE -> "Transposed"
        else -> "Normal"
    }

    private fun meteringName(m: Int) = when (m) {
        1 -> "Average"; 2 -> "Center weighted"; 3 -> "Spot"; 4 -> "Multi-spot"; 5 -> "Pattern"; 6 -> "Partial"; else -> "Other"
    }

    private fun prettyExifDate(raw: String): String = runCatching {
        val parsed = java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(raw)
        DateFormat.getDateTimeInstance().format(parsed!!)
    }.getOrDefault(raw)
}

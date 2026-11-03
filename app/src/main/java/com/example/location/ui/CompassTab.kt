@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.location.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.location.compass.CompassAccuracy
import com.example.location.domain.GeoMath
import com.example.ui.components.BannerKind
import com.example.ui.components.KeyValueRow
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBanner
import com.example.ui.theme.emphasized
import com.example.ui.theme.mono
import java.util.Locale

@Composable
fun CompassTab(viewModel: LocationKitViewModel, contentPadding: PaddingValues) {
    val compass by viewModel.compass.collectAsStateWithLifecycle()
    // Keep location flowing so true north can be derived (does nothing without permission).
    val location by viewModel.location.collectAsStateWithLifecycle()
    var useTrue by rememberSaveable { mutableStateOf(false) }

    val reading = compass.reading
    val trueAvailable = compass.trueHeading != null
    val showTrue = useTrue && trueAvailable
    val heading = if (showTrue) compass.trueHeading else reading?.magneticHeading

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!viewModel.compassAvailable) {
            item(key = "none") {
                StatusBanner(BannerKind.Error, "This device has no usable compass sensor (rotation vector, or accelerometer with magnetometer).")
            }
            return@LazyColumn
        }

        if (reading?.needsCalibration == true) {
            item(key = "calibrate") {
                StatusBanner(
                    kind = BannerKind.Warning,
                    title = "Compass needs calibration",
                    message = if (reading.possibleInterference) {
                        "Strong magnetic interference detected. Move away from magnets, speakers and metal, then sweep the phone in a figure-eight."
                    } else {
                        "Sweep the phone in a figure-eight a few times. Readings may be off until accuracy improves."
                    }
                )
            }
        }

        item(key = "dial") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (heading == null) {
                    Text("Waiting for sensor data", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(48.dp))
                } else {
                    CompassDial(heading, Modifier.widthIn(max = 340.dp).fillMaxWidth().aspectRatio(1f))
                }
            }
        }

        item(key = "readout") {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                androidx.compose.foundation.layout.Column(
                    Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        heading?.let { String.format(Locale.getDefault(), "%.0f° %s", it, GeoMath.cardinal(it)) } ?: "--",
                        style = MaterialTheme.typography.displaySmall.emphasized().mono(),
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !showTrue, onClick = { useTrue = false }, label = { Text("Magnetic north") })
                        FilterChip(selected = showTrue, onClick = { useTrue = true }, enabled = trueAvailable, label = { Text("True north") })
                    }
                    if (!trueAvailable) {
                        Text(
                            "True north needs a location fix to look up the magnetic declination.",
                            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item(key = "sensor") {
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("Sensor")
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    androidx.compose.foundation.layout.Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        KeyValueRow("Source", reading?.source?.label ?: "None")
                        KeyValueRow("Accuracy", when (reading?.accuracy) {
                            CompassAccuracy.High -> "High"; CompassAccuracy.Medium -> "Medium"; CompassAccuracy.Low -> "Low"
                            CompassAccuracy.Unreliable -> "Unreliable"; null -> "Unknown"
                        })
                        KeyValueRow("Magnetic field", reading?.fieldStrengthMicroTesla?.let { String.format(Locale.getDefault(), "%.0f µT", it) } ?: "Not reported")
                        KeyValueRow("Declination", compass.declinationDegrees?.let { String.format(Locale.getDefault(), "%+.1f°", it) } ?: "Unavailable")
                        KeyValueRow("Magnetic heading", reading?.let { String.format(Locale.getDefault(), "%.1f°", it.magneticHeading) } ?: "--")
                        KeyValueRow("Tilt", reading?.let { String.format(Locale.getDefault(), "%.0f°", it.tiltDegrees) } ?: "--")
                    }
                }
            }
        }
    }
}

/** Rotating dial: the card turns by -heading so the letter under the top marker is the direction the phone faces. */
@Composable
private fun CompassDial(heading: Double, modifier: Modifier) {
    val ring = MaterialTheme.colorScheme.outline
    val tick = MaterialTheme.colorScheme.onSurfaceVariant
    val north = MaterialTheme.colorScheme.error
    val text = MaterialTheme.colorScheme.onSurface
    val marker = MaterialTheme.colorScheme.primary
    val face = MaterialTheme.colorScheme.surfaceContainerHigh
    val description = "Compass heading ${"%.0f".format(heading)} degrees ${GeoMath.cardinal(heading)}"

    Canvas(modifier.semantics { contentDescription = description }) {
        val c = center
        val radius = size.minDimension / 2f
        drawCircle(face, radius)
        drawCircle(ring, radius, style = Stroke(2.dp.toPx()))

        rotate(degrees = -heading.toFloat(), pivot = c) {
            for (deg in 0 until 360 step 10) {
                val major = deg % 90 == 0
                val mid = deg % 30 == 0
                val len = if (major) 22.dp.toPx() else if (mid) 16.dp.toPx() else 9.dp.toPx()
                val a = Math.toRadians(deg.toDouble() - 90.0)
                val start = Offset(c.x + (radius - 4.dp.toPx()) * Math.cos(a).toFloat(), c.y + (radius - 4.dp.toPx()) * Math.sin(a).toFloat())
                val end = Offset(c.x + (radius - 4.dp.toPx() - len) * Math.cos(a).toFloat(), c.y + (radius - 4.dp.toPx() - len) * Math.sin(a).toFloat())
                drawLine(if (deg == 0) north else tick, start, end, strokeWidth = if (major) 3.dp.toPx() else 1.5.dp.toPx())
            }
            val paint = Paint().apply { isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 24.dp.toPx(); isFakeBoldText = true }
            listOf("N" to 0, "E" to 90, "S" to 180, "W" to 270).forEach { (label, deg) ->
                paint.color = (if (label == "N") north else text).toArgb()
                val a = Math.toRadians(deg.toDouble() - 90.0)
                val r = radius - 48.dp.toPx()
                val x = c.x + r * Math.cos(a).toFloat()
                val y = c.y + r * Math.sin(a).toFloat() + paint.textSize / 3f
                // keep letters upright relative to the dial
                drawContext.canvas.nativeCanvas.save()
                drawContext.canvas.nativeCanvas.rotate(deg.toFloat(), x, y - paint.textSize / 3f)
                drawContext.canvas.nativeCanvas.drawText(label, x, y, paint)
                drawContext.canvas.nativeCanvas.restore()
            }
        }

        // fixed marker at the top = direction the phone is facing
        val tri = Path().apply {
            moveTo(c.x, c.y - radius + 4.dp.toPx())
            lineTo(c.x - 10.dp.toPx(), c.y - radius + 26.dp.toPx())
            lineTo(c.x + 10.dp.toPx(), c.y - radius + 26.dp.toPx())
            close()
        }
        drawPath(tri, marker)
        drawCircle(marker, 5.dp.toPx(), c)
    }
}

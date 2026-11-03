package com.example.location.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface
import com.example.location.domain.GeoMath
import com.example.location.domain.HeadingFilter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Turns raw Android sensor events into a magnetic heading.
 *
 * Sensors are registered only while the returned flow is collected and unregistered when collection stops,
 * so the UI controls the lifecycle. Nothing here touches Compose.
 */
class CompassEngine(context: Context) {

    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val displayManager = context.applicationContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val geomagneticRotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    val source: CompassSource? = when {
        rotationSensor != null -> CompassSource.RotationVector
        geomagneticRotationSensor != null -> CompassSource.GeomagneticRotationVector
        accelerometer != null && magnetometer != null -> CompassSource.AccelerometerMagnetometer
        else -> null
    }

    val isAvailable: Boolean get() = source != null

    fun readings(): Flow<CompassReading> = callbackFlow {
        val src = source
        if (src == null) {
            close()
            return@callbackFlow
        }

        val filter = HeadingFilter(alpha = 0.18)
        val rotationMatrix = FloatArray(9)
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var haveGravity = false
        var haveMagnetic = false
        var fieldStrength: Float? = null
        var magneticStatus = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
        var rotationStatus = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
        var lastEmitNanos = 0L

        fun emit(timestampNanos: Long) {
            if (timestampNanos - lastEmitNanos < MIN_INTERVAL_NANOS) return
            lastEmitNanos = timestampNanos
            val (azimuth, tilt) = headingFromMatrix(rotationMatrix, displayRotation())
            val statusInt = minOf(magneticStatus, rotationStatus)
            trySend(
                CompassReading(
                    magneticHeading = filter.filter(azimuth),
                    source = src,
                    accuracy = statusInt.toAccuracy(),
                    fieldStrengthMicroTesla = fieldStrength,
                    tiltDegrees = tilt
                )
            )
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> {
                        rotationStatus = event.accuracy
                        // Some devices deliver 5 values; the matrix routine only needs the first 4.
                        val v = if (event.values.size > 4) event.values.copyOf(4) else event.values
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, v)
                        emit(event.timestamp)
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        magneticStatus = event.accuracy
                        val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                        fieldStrength = sqrt(x * x + y * y + z * z)
                        if (src == CompassSource.AccelerometerMagnetometer) {
                            smooth(geomagnetic, event.values, haveMagnetic)
                            haveMagnetic = true
                            if (haveGravity && SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) emit(event.timestamp)
                        }
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        smooth(gravity, event.values, haveGravity)
                        haveGravity = true
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                when (sensor.type) {
                    Sensor.TYPE_MAGNETIC_FIELD -> magneticStatus = accuracy
                    Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> rotationStatus = accuracy
                }
            }
        }

        when (src) {
            CompassSource.RotationVector -> sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
            CompassSource.GeomagneticRotationVector -> sensorManager.registerListener(listener, geomagneticRotationSensor, SensorManager.SENSOR_DELAY_GAME)
            CompassSource.AccelerometerMagnetometer -> sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        // The magnetometer is always observed: it supplies field strength (interference) and calibration status.
        magnetometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }

        awaitClose { sensorManager.unregisterListener(listener) }
    }

    private fun displayRotation(): Int = displayManager.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0

    private fun smooth(target: FloatArray, input: FloatArray, primed: Boolean) {
        for (i in 0..2) target[i] = if (primed) target[i] + 0.15f * (input[i] - target[i]) else input[i]
    }

    private fun Int.toAccuracy() = when (this) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassAccuracy.High
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassAccuracy.Medium
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassAccuracy.Low
        else -> CompassAccuracy.Unreliable
    }

    /**
     * Magnetic heading of "where the phone points" plus tilt from flat.
     * Flat or slightly tilted: the top edge. Held upright: the back of the phone (camera direction),
     * which avoids the heading flipping as the phone is raised.
     */
    private fun headingFromMatrix(matrix: FloatArray, rotation: Int): Pair<Double, Float> {
        val remapped = FloatArray(9)
        val (axisX, axisY) = when (rotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        SensorManager.remapCoordinateSystem(matrix, axisX, axisY, remapped)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(remapped, orientation)
        val pitchDeg = Math.toDegrees(orientation[1].toDouble())
        var azimuth = orientation[0]
        if (abs(pitchDeg) > UPRIGHT_PITCH_DEG) {
            val upright = FloatArray(9)
            SensorManager.remapCoordinateSystem(remapped, SensorManager.AXIS_X, SensorManager.AXIS_Z, upright)
            val o2 = FloatArray(3)
            SensorManager.getOrientation(upright, o2)
            azimuth = o2[0]
        }
        return GeoMath.normalizeDegrees(Math.toDegrees(azimuth.toDouble())) to abs(pitchDeg).toFloat()
    }

    private companion object {
        const val MIN_INTERVAL_NANOS = 33_000_000L // ~30 updates per second is plenty for a dial
        const val UPRIGHT_PITCH_DEG = 55.0
    }
}

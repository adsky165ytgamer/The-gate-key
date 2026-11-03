package com.example.location.compass

enum class CompassAccuracy { Unreliable, Low, Medium, High }

enum class CompassSource(val label: String) {
    RotationVector("Fused rotation vector"),
    GeomagneticRotationVector("Geomagnetic rotation vector"),
    AccelerometerMagnetometer("Accelerometer + magnetometer")
}

/** One processed heading. All angles are degrees clockwise from MAGNETIC north, in [0, 360). */
data class CompassReading(
    val magneticHeading: Double,
    val source: CompassSource,
    val accuracy: CompassAccuracy,
    /** Magnetic field strength in microtesla, when the magnetometer reports it. */
    val fieldStrengthMicroTesla: Float?,
    /** How far the phone is tilted from flat, degrees. */
    val tiltDegrees: Float
) {
    /** Earth's field is roughly 25-65 uT; far outside that means a magnet, speaker, metal or a bad calibration. */
    val possibleInterference: Boolean
        get() = fieldStrengthMicroTesla?.let { it < 20f || it > 70f } ?: false

    val needsCalibration: Boolean
        get() = accuracy == CompassAccuracy.Unreliable || accuracy == CompassAccuracy.Low || possibleInterference
}

/** Heading as shown to the person: magnetic, plus true north when a declination is known. */
data class CompassState(
    val reading: CompassReading? = null,
    /** East-positive declination in degrees at the current location, or null when no location is available. */
    val declinationDegrees: Float? = null,
    val sensorAvailable: Boolean = true
) {
    val trueHeading: Double?
        get() = reading?.let { r -> declinationDegrees?.let { (r.magneticHeading + it + 360.0) % 360.0 } }
}

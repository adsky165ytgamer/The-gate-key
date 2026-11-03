package com.example.location.domain

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Low-pass filter for compass headings. It smooths the unit vector instead of the angle,
 * so 359° -> 1° does not swing through 180°.
 */
class HeadingFilter(private val alpha: Double = 0.2) {
    private var x = 0.0
    private var y = 0.0
    private var primed = false

    fun filter(degrees: Double): Double {
        val r = degrees * PI / 180.0
        if (!primed) {
            x = cos(r); y = sin(r); primed = true
        } else {
            x += alpha * (cos(r) - x)
            y += alpha * (sin(r) - y)
        }
        return GeoMath.normalizeDegrees(atan2(y, x) * 180.0 / PI)
    }

    fun reset() { primed = false }
}

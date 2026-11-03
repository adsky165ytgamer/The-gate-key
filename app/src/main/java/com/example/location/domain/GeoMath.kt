package com.example.location.domain

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Pure geographic maths. No Android, no network, no state. */
object GeoMath {

    /** Mean Earth radius (IUGG), metres. */
    const val EARTH_RADIUS_M = 6_371_008.8

    private fun rad(deg: Double) = deg * PI / 180.0
    private fun deg(rad: Double) = rad * 180.0 / PI

    /** Wraps any longitude into [-180, 180]. 180 and -180 both map to 180. */
    fun normalizeLongitude(lon: Double): Double {
        if (!lon.isFinite()) return lon
        var v = ((lon + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
        if (v == -180.0 && lon > 0) v = 180.0
        return v
    }

    /** Wraps degrees into [0, 360). */
    fun normalizeDegrees(degrees: Double): Double = ((degrees % 360.0) + 360.0) % 360.0

    /** Shortest signed difference to - from, in (-180, 180]. */
    fun angleDelta(from: Double, to: Double): Double {
        val d = normalizeDegrees(to - from)
        return if (d > 180.0) d - 360.0 else d
    }

    /** Great-circle distance in metres (Haversine). The antimeridian needs no special casing: sin²(Δλ/2) is periodic. */
    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val phi1 = rad(a.latitude)
        val phi2 = rad(b.latitude)
        val dPhi = phi2 - phi1
        val dLambda = rad(b.longitude - a.longitude)
        val h = sin(dPhi / 2).let { it * it } + cos(phi1) * cos(phi2) * sin(dLambda / 2).let { it * it }
        return 2.0 * EARTH_RADIUS_M * asin(min(1.0, sqrt(h)))
    }

    /** Initial great-circle bearing from a to b in degrees clockwise from true north, [0, 360). */
    fun initialBearing(a: GeoPoint, b: GeoPoint): Double {
        val phi1 = rad(a.latitude)
        val phi2 = rad(b.latitude)
        val dLambda = rad(b.longitude - a.longitude)
        val y = sin(dLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
        return normalizeDegrees(deg(atan2(y, x)))
    }

    /** "NE", "SSW"... for a compass heading. */
    fun cardinal(degrees: Double): String {
        val names = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        return names[((normalizeDegrees(degrees) / 22.5) + 0.5).toInt() % 16]
    }
}

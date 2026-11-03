package com.example.location.compass

import android.hardware.GeomagneticField
import com.example.location.domain.GeoPoint
import kotlin.math.abs

/** World Magnetic Model declination (from Android's GeomagneticField) used to convert magnetic to true north. */
class MagneticDeclination {
    private var cachedFor: GeoPoint? = null
    private var cachedAtMillis = 0L
    private var cachedValue = 0f

    /** East-positive declination in degrees for the position and time. Cached for nearby, recent queries. */
    fun at(point: GeoPoint, altitudeMeters: Double?, timeMillis: Long = System.currentTimeMillis()): Float {
        val c = cachedFor
        if (c != null && abs(c.latitude - point.latitude) < 0.1 && abs(c.longitude - point.longitude) < 0.1 &&
            abs(timeMillis - cachedAtMillis) < 6 * 60 * 60 * 1000L
        ) return cachedValue
        val field = GeomagneticField(point.latitude.toFloat(), point.longitude.toFloat(), (altitudeMeters ?: 0.0).toFloat(), timeMillis)
        cachedFor = point
        cachedAtMillis = timeMillis
        cachedValue = field.declination
        return cachedValue
    }
}

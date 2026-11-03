package com.example.location.domain

/** A WGS-84 coordinate in degrees. Longitude is kept in [-180, 180]. */
data class GeoPoint(val latitude: Double, val longitude: Double) {

    val isValid: Boolean
        get() = latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0

    companion object {
        /**
         * Builds a point from raw values, wrapping longitude into [-180, 180].
         * Returns null for NaN/infinite values or a latitude outside [-90, 90].
         */
        fun of(latitude: Double, longitude: Double): GeoPoint? {
            if (!latitude.isFinite() || !longitude.isFinite()) return null
            if (latitude !in -90.0..90.0) return null
            return GeoPoint(latitude, GeoMath.normalizeLongitude(longitude))
        }

        /** Parses "lat" / "lon" text typed by a person (accepts a decimal comma). */
        fun parse(latitudeText: String, longitudeText: String): GeoPoint? {
            fun num(t: String) = t.trim().replace(',', '.').toDoubleOrNull()
            val lat = num(latitudeText) ?: return null
            val lon = num(longitudeText) ?: return null
            if (lon !in -180.0..180.0) return null
            return of(lat, lon)
        }
    }
}

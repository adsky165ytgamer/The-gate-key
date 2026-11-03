package com.example.location.domain

import java.util.Locale

enum class UnitSystem { Metric, Imperial, Nautical }

/** A distance in metres with unit conversions and readable formatting. */
@JvmInline
value class Distance(val meters: Double) {
    val kilometers: Double get() = meters / 1000.0
    val miles: Double get() = meters / 1609.344
    val nauticalMiles: Double get() = meters / 1852.0
    val feet: Double get() = meters / 0.3048

    /** Picks the most readable unit for the system, e.g. "850 m", "12.4 km", "340 ft", "2.8 mi". */
    fun format(system: UnitSystem, locale: Locale = Locale.getDefault()): String = when (system) {
        UnitSystem.Metric ->
            if (meters < 1000.0) String.format(locale, "%.0f m", meters)
            else String.format(locale, if (kilometers < 100.0) "%.2f km" else "%.1f km", kilometers)
        UnitSystem.Imperial ->
            if (miles < 0.1) String.format(locale, "%.0f ft", feet)
            else String.format(locale, if (miles < 100.0) "%.2f mi" else "%.1f mi", miles)
        UnitSystem.Nautical -> String.format(locale, "%.2f nmi", nauticalMiles)
    }
}

sealed interface DistanceOutcome {
    /** One or both endpoints have not been chosen yet. */
    data class Incomplete(val hasA: Boolean, val hasB: Boolean) : DistanceOutcome

    /** An endpoint is not a valid coordinate. */
    data class Invalid(val reason: String) : DistanceOutcome

    /** [bearingDegrees] is null when both points coincide. */
    data class Success(val distance: Distance, val bearingDegrees: Double?, val identical: Boolean) : DistanceOutcome
}

/** Entry point for "how far is A from B". Offline, deterministic, independent from UI. */
object DistanceCalculator {

    fun calculate(a: GeoPoint?, b: GeoPoint?): DistanceOutcome {
        if (a == null || b == null) return DistanceOutcome.Incomplete(hasA = a != null, hasB = b != null)
        if (!a.isValid) return DistanceOutcome.Invalid("Point A is not a valid coordinate.")
        if (!b.isValid) return DistanceOutcome.Invalid("Point B is not a valid coordinate.")
        val meters = GeoMath.distanceMeters(a, b)
        val identical = meters < 0.01
        return DistanceOutcome.Success(
            distance = Distance(if (identical) 0.0 else meters),
            bearingDegrees = if (identical) null else GeoMath.initialBearing(a, b),
            identical = identical
        )
    }
}

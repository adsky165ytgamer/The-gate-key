package com.example.location.core

import android.os.SystemClock
import com.example.location.domain.GeoPoint

/** One position reading, exactly as the platform reported it. */
data class LocationFix(
    val point: GeoPoint,
    val accuracyMeters: Float?,
    val altitudeMeters: Double?,
    val speedMps: Float?,
    val bearingDegrees: Float?,
    val provider: String?,
    /** Wall-clock time the fix was computed (epoch ms). */
    val timeMillis: Long,
    /** Monotonic time of the fix; used for age so clock changes cannot make an old fix look new. */
    val elapsedRealtimeNanos: Long,
    val isMock: Boolean
) {
    fun ageMillis(nowElapsedNanos: Long = SystemClock.elapsedRealtimeNanos()): Long =
        ((nowElapsedNanos - elapsedRealtimeNanos) / 1_000_000L).coerceAtLeast(0L)

    fun freshness(nowElapsedNanos: Long = SystemClock.elapsedRealtimeNanos()): Freshness {
        val age = ageMillis(nowElapsedNanos)
        return when {
            age <= LIVE_MS -> Freshness.Live
            age <= STALE_MS -> Freshness.Stale
            else -> Freshness.Old
        }
    }

    fun accuracyQuality(): AccuracyQuality {
        val a = accuracyMeters ?: return AccuracyQuality.Unknown
        return when {
            a <= 10f -> AccuracyQuality.High
            a <= 30f -> AccuracyQuality.Good
            a <= 100f -> AccuracyQuality.Fair
            else -> AccuracyQuality.Poor
        }
    }

    private companion object {
        const val LIVE_MS = 10_000L
        const val STALE_MS = 60_000L
    }
}

enum class Freshness { Live, Stale, Old }
enum class AccuracyQuality { High, Good, Fair, Poor, Unknown }

/** Everything a feature needs to know about "where is the phone". */
sealed interface LocationState {
    /** Nobody is collecting; nothing has been requested. */
    data object Idle : LocationState

    /** Location permission is missing. */
    data object PermissionRequired : LocationState

    /** Permission is granted but device location is switched off. */
    data object ServicesDisabled : LocationState

    /** Updates are registered but no (fresh) fix has arrived; [lastKnown] may be old. */
    data class Searching(val lastKnown: LocationFix?) : LocationState

    data class Active(val fix: LocationFix) : LocationState

    data class Failed(val message: String) : LocationState
}

val LocationState.fixOrNull: LocationFix?
    get() = when (this) {
        is LocationState.Active -> fix
        is LocationState.Searching -> lastKnown
        else -> null
    }

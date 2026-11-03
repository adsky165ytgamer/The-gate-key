package com.example.location.core

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.location.domain.GeoPoint
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * The single place that talks to the Fused Location Provider.
 *
 * GPS Coordinates, the map's "my location" and the distance tool's "use my location" all read [state];
 * the platform registration exists only while at least one collector is active and is released 5 s after the last one leaves.
 * Foreground use only: no background location permission is requested.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocationRepository(context: Context) {

    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Bumped whenever permission or the system location switch may have changed. */
    private val restart = MutableStateFlow(0)

    val state: StateFlow<LocationState> = restart
        .flatMapLatest { updates() }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), LocationState.Idle)

    init {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = refresh()
        }
        ContextCompat.registerReceiver(
            appContext, receiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun hasPermission(): Boolean = hasFine() || hasCoarse()

    fun hasPrecisePermission(): Boolean = hasFine()

    fun isLocationEnabled(): Boolean = LocationManagerCompat.isLocationEnabled(locationManager)

    /** Re-evaluates permission and device settings and restarts updates. Call after a permission result. */
    fun refresh() = restart.update { it + 1 }

    private fun hasFine() = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    private fun hasCoarse() = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // guarded by hasPermission() and a SecurityException catch
    private fun updates(): Flow<LocationState> = callbackFlow {
        if (!hasPermission()) {
            trySend(LocationState.PermissionRequired)
            awaitClose { }
            return@callbackFlow
        }
        if (!isLocationEnabled()) {
            trySend(LocationState.ServicesDisabled)
            awaitClose { }
            return@callbackFlow
        }

        var latest: LocationFix? = null
        trySend(LocationState.Searching(null))

        val priority = if (hasFine()) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        val request = LocationRequest.Builder(priority, 1_000L)
            .setMinUpdateIntervalMillis(500L)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val fix = result.lastLocation?.toFix() ?: return
                latest = fix
                trySend(LocationState.Active(fix))
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) trySend(LocationState.Searching(latest))
            }
        }

        // Seed with the cached position so something appears quickly - its age is reported honestly.
        try {
            client.lastLocation.addOnSuccessListener { loc ->
                val fix = loc?.toFix() ?: return@addOnSuccessListener
                if (latest == null) {
                    latest = fix
                    trySend(LocationState.Searching(fix))
                }
            }
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnFailureListener { trySend(LocationState.Failed(it.message ?: "Location updates could not start.")) }
        } catch (e: SecurityException) {
            trySend(LocationState.PermissionRequired)
        }

        awaitClose { client.removeLocationUpdates(callback) }
    }

    private fun Location.toFix(): LocationFix? {
        val point = GeoPoint.of(latitude, longitude) ?: return null
        val mock = if (Build.VERSION.SDK_INT >= 31) isMock else @Suppress("DEPRECATION") isFromMockProvider
        return LocationFix(
            point = point,
            accuracyMeters = if (hasAccuracy()) accuracy else null,
            altitudeMeters = if (hasAltitude()) altitude else null,
            speedMps = if (hasSpeed()) speed else null,
            bearingDegrees = if (hasBearing()) bearing else null,
            provider = provider,
            timeMillis = time,
            elapsedRealtimeNanos = elapsedRealtimeNanos,
            isMock = mock
        )
    }
}

/** Process-wide access to the shared location source. */
object LocationKit {
    @Volatile private var instance: LocationRepository? = null

    fun repository(context: Context): LocationRepository =
        instance ?: synchronized(this) { instance ?: LocationRepository(context).also { instance = it } }
}

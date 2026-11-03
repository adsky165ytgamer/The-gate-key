package com.example.location.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.location.compass.CompassEngine
import com.example.location.compass.CompassState
import com.example.location.compass.MagneticDeclination
import com.example.location.core.Freshness
import com.example.location.core.LocationKit
import com.example.location.core.LocationState
import com.example.location.core.fixOrNull
import com.example.location.domain.DistanceCalculator
import com.example.location.domain.DistanceOutcome
import com.example.location.domain.GeoPoint
import com.example.location.domain.UnitSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class EndpointSlot { A, B }

/**
 * Connects the Location Kit engines to the UI:
 *  - [location]  <- shared Fused Location source (also feeds the map dot and "use my location")
 *  - [compass]   <- sensor heading + declination derived from the current location
 *  - [distance]  <- pure calculation of the two chosen endpoints, recomputed whenever either changes
 */
class LocationKitViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = LocationKit.repository(app)
    private val compassEngine = CompassEngine(app)
    private val declination = MagneticDeclination()

    val location: StateFlow<LocationState> = repository.state

    val compass: StateFlow<CompassState> =
        combine(compassEngine.readings(), repository.state) { reading, loc ->
            val fix = loc.fixOrNull
            CompassState(
                reading = reading,
                declinationDegrees = fix?.let { declination.at(it.point, it.altitudeMeters) },
                sensorAvailable = true
            )
        }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(3_000),
            CompassState(sensorAvailable = compassEngine.isAvailable)
        )

    val compassAvailable: Boolean get() = compassEngine.isAvailable

    // ── distance ────────────────────────────────────────────────────────────

    private val _pointA = MutableStateFlow<GeoPoint?>(null)
    private val _pointB = MutableStateFlow<GeoPoint?>(null)
    private val _slot = MutableStateFlow(EndpointSlot.A)
    private val _units = MutableStateFlow(UnitSystem.Metric)
    private val _message = MutableStateFlow<String?>(null)

    val pointA: StateFlow<GeoPoint?> = _pointA.asStateFlow()
    val pointB: StateFlow<GeoPoint?> = _pointB.asStateFlow()
    val activeSlot: StateFlow<EndpointSlot> = _slot.asStateFlow()
    val unitSystem: StateFlow<UnitSystem> = _units.asStateFlow()
    val message: StateFlow<String?> = _message.asStateFlow()

    val distance: StateFlow<DistanceOutcome> =
        combine(_pointA, _pointB) { a, b -> DistanceCalculator.calculate(a, b) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, DistanceOutcome.Incomplete(hasA = false, hasB = false))

    fun selectSlot(slot: EndpointSlot) { _slot.value = slot }

    fun setUnits(system: UnitSystem) { _units.value = system }

    fun setPoint(slot: EndpointSlot, point: GeoPoint?) {
        when (slot) {
            EndpointSlot.A -> _pointA.value = point
            EndpointSlot.B -> _pointB.value = point
        }
        _message.value = null
    }

    /** A tap on the map assigns the active endpoint, then moves on to the empty one. */
    fun onMapTap(point: GeoPoint) {
        val slot = _slot.value
        setPoint(slot, point)
        if (slot == EndpointSlot.A && _pointB.value == null) _slot.value = EndpointSlot.B
    }

    fun swapPoints() {
        val a = _pointA.value
        _pointA.value = _pointB.value
        _pointB.value = a
    }

    fun useMyLocation(slot: EndpointSlot) {
        val fix = location.value.fixOrNull
        when {
            fix == null -> _message.value = "No GPS position yet. Wait for a fix, then try again."
            fix.freshness() == Freshness.Old -> _message.value = "The last GPS position is too old. Wait for a current fix."
            else -> setPoint(slot, fix.point)
        }
    }

    fun clearMessage() = _message.update { null }

    /** Call after a permission result or after returning from system settings. */
    fun refreshLocation() = repository.refresh()

    val hasLocationPermission: Boolean get() = repository.hasPermission()
}

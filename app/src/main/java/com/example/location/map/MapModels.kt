package com.example.location.map

import com.example.location.domain.GeoPoint

sealed interface MapStatus {
    data object Loading : MapStatus
    data object Ready : MapStatus
    data class Error(val message: String) : MapStatus
}

/** One-shot camera command. [nonce] makes two identical requests distinct. */
sealed interface CameraRequest {
    val nonce: Long

    data class MoveTo(val point: GeoPoint, val zoom: Double? = null, override val nonce: Long = System.nanoTime()) : CameraRequest
    data class ZoomBy(val delta: Double, override val nonce: Long = System.nanoTime()) : CameraRequest
    data class Fit(val a: GeoPoint, val b: GeoPoint, override val nonce: Long = System.nanoTime()) : CameraRequest
}

/** What to draw on top of the base map. */
data class MapMarkers(
    val user: GeoPoint? = null,
    val pointA: GeoPoint? = null,
    val pointB: GeoPoint? = null,
    val connectAB: Boolean = false
)

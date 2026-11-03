package com.example.location.map

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.location.domain.GeoMath
import com.example.location.domain.GeoPoint
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private class MapHandle(val map: MapLibreMap, val style: Style)

private const val SRC_USER = "lk-user-src"
private const val SRC_A = "lk-a-src"
private const val SRC_B = "lk-b-src"
private const val SRC_LINE = "lk-line-src"
private const val STYLE_TIMEOUT_MS = 20_000L

/**
 * Interactive MapLibre map. Owns the MapView lifecycle, loads the provider's style, reports status/errors,
 * applies camera requests, draws the user dot / endpoints / connecting line and forwards taps.
 * The attribution button built into MapLibre stays enabled; screens must also show [MapProvider.attribution].
 */
@Composable
fun LocationMap(
    provider: MapProvider,
    markers: MapMarkers,
    cameraRequest: CameraRequest?,
    retryKey: Int,
    onStatus: (MapStatus) -> Unit,
    modifier: Modifier = Modifier,
    initialCenter: GeoPoint? = null,
    onMapTap: ((GeoPoint) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnStatus by rememberUpdatedState(onStatus)
    val currentOnTap by rememberUpdatedState(onMapTap)
    var handle by remember { mutableStateOf<MapHandle?>(null) }
    var loaded by remember { mutableStateOf(false) }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context)
    }

    // Lifecycle: the MapView must receive every lifecycle callback or it leaks GL resources / crashes.
    DisposableEffect(mapView, lifecycleOwner) {
        var destroyed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> { destroyed = true; mapView.onDestroy() }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!destroyed) {
                mapView.onPause()
                mapView.onStop()
                mapView.onDestroy()
            }
        }
    }

    // Style loading (re-runs on retry).
    DisposableEffect(mapView, provider.styleUrl, retryKey) {
        loaded = false
        handle = null
        currentOnStatus(MapStatus.Loading)

        val failListener = MapView.OnDidFailLoadingMapListener { message ->
            if (!loaded) currentOnStatus(MapStatus.Error(friendlyError(context, message)))
        }
        mapView.addOnDidFailLoadingMapListener(failListener)

        mapView.getMapAsync { map ->
            map.uiSettings.isAttributionEnabled = true
            map.uiSettings.isLogoEnabled = true
            map.uiSettings.isCompassEnabled = true
            if (map.cameraPosition.zoom < 1.0 || retryKey == 0) {
                val target = initialCenter?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(20.0, 0.0)
                val zoom = if (initialCenter != null) 13.0 else 1.5
                map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target(target).zoom(zoom).build()))
            }
            map.addOnMapClickListener { latLng ->
                val tap = currentOnTap
                if (tap == null) false else {
                    GeoPoint.of(latLng.latitude, latLng.longitude)?.let(tap)
                    true
                }
            }
            map.setStyle(Style.Builder().fromUri(provider.styleUrl)) { style ->
                MapOverlays.install(style)
                loaded = true
                handle = MapHandle(map, style)
                currentOnStatus(MapStatus.Ready)
            }
        }
        onDispose { mapView.removeOnDidFailLoadingMapListener(failListener) }
    }

    // Watchdog: some failures (blocked DNS, captive portal) never raise a callback.
    LaunchedEffect(provider.styleUrl, retryKey) {
        delay(STYLE_TIMEOUT_MS)
        if (!loaded) currentOnStatus(MapStatus.Error(friendlyError(context, null)))
    }

    LaunchedEffect(handle, markers) {
        handle?.let { MapOverlays.update(it.style, markers) }
    }

    LaunchedEffect(handle, cameraRequest?.nonce) {
        val h = handle ?: return@LaunchedEffect
        when (val r = cameraRequest) {
            is CameraRequest.MoveTo -> {
                val target = LatLng(r.point.latitude, r.point.longitude)
                val update = if (r.zoom != null) CameraUpdateFactory.newLatLngZoom(target, r.zoom) else CameraUpdateFactory.newLatLng(target)
                h.map.animateCamera(update, 700)
            }
            is CameraRequest.ZoomBy -> h.map.animateCamera(CameraUpdateFactory.zoomBy(r.delta), 300)
            is CameraRequest.Fit -> fit(h.map, r.a, r.b)
            null -> Unit
        }
    }

    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
    }
}

private fun fit(map: MapLibreMap, a: GeoPoint, b: GeoPoint) {
    // Across the antimeridian a lat/lng box would span the whole world, so centre between the points instead.
    if (kotlin.math.abs(a.longitude - b.longitude) > 180.0) {
        val midLon = GeoMath.normalizeLongitude((a.longitude + b.longitude + 360.0) / 2.0)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng((a.latitude + b.latitude) / 2.0, midLon), 2.0), 700)
        return
    }
    val bounds = LatLngBounds.Builder()
        .include(LatLng(a.latitude, a.longitude))
        .include(LatLng(b.latitude, b.longitude))
        .build()
    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120), 700)
}

private fun friendlyError(context: Context, raw: String?): String {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val online = cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    return if (!online) "No internet connection. The map needs a network connection to load tiles."
    else "The map could not be loaded.${raw?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""}"
}

/** GeoJSON sources + circle/line layers for the user position, endpoints A/B and the line between them. */
private object MapOverlays {

    fun install(style: Style) {
        if (style.getSource(SRC_USER) != null) return
        style.addSource(GeoJsonSource(SRC_LINE))
        style.addSource(GeoJsonSource(SRC_USER))
        style.addSource(GeoJsonSource(SRC_A))
        style.addSource(GeoJsonSource(SRC_B))

        style.addLayer(
            LineLayer("lk-line", SRC_LINE).withProperties(
                PropertyFactory.lineColor(Color.parseColor("#1565C0")),
                PropertyFactory.lineWidth(4f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
            )
        )
        style.addLayer(dot("lk-user-halo", SRC_USER, 16f, "#2979FF", 0.25f, 0f))
        style.addLayer(dot("lk-user", SRC_USER, 7f, "#2979FF", 1f, 2.5f))
        style.addLayer(dot("lk-a", SRC_A, 10f, "#2E7D32", 1f, 3f))
        style.addLayer(dot("lk-b", SRC_B, 10f, "#C62828", 1f, 3f))
    }

    private fun dot(id: String, source: String, radius: Float, hex: String, opacity: Float, stroke: Float) =
        CircleLayer(id, source).withProperties(
            PropertyFactory.circleRadius(radius),
            PropertyFactory.circleColor(Color.parseColor(hex)),
            PropertyFactory.circleOpacity(opacity),
            PropertyFactory.circleStrokeColor(Color.WHITE),
            PropertyFactory.circleStrokeWidth(stroke)
        )

    fun update(style: Style, m: MapMarkers) {
        set(style, SRC_USER, m.user)
        set(style, SRC_A, m.pointA)
        set(style, SRC_B, m.pointB)
        val line = style.getSourceAs<GeoJsonSource>(SRC_LINE)
        val a = m.pointA
        val b = m.pointB
        if (m.connectAB && a != null && b != null) {
            // Take the short way round the globe when the points straddle the antimeridian.
            val d = b.longitude - a.longitude
            val lonB = when {
                d > 180.0 -> b.longitude - 360.0
                d < -180.0 -> b.longitude + 360.0
                else -> b.longitude
            }
            line?.setGeoJson(
                Feature.fromGeometry(LineString.fromLngLats(listOf(Point.fromLngLat(a.longitude, a.latitude), Point.fromLngLat(lonB, b.latitude))))
            )
        } else {
            line?.setGeoJson(FeatureCollection.fromFeatures(emptyList<Feature>()))
        }
    }

    private fun set(style: Style, sourceId: String, point: GeoPoint?) {
        val source = style.getSourceAs<GeoJsonSource>(sourceId) ?: return
        if (point == null) source.setGeoJson(FeatureCollection.fromFeatures(emptyList<Feature>()))
        else source.setGeoJson(Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude)))
    }
}

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.location.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.location.core.fixOrNull
import com.example.location.domain.Distance
import com.example.location.domain.DistanceOutcome
import com.example.location.domain.GeoMath
import com.example.location.domain.GeoPoint
import com.example.location.domain.UnitSystem
import com.example.location.map.CameraRequest
import com.example.location.map.LocationMap
import com.example.location.map.MapMarkers
import com.example.location.map.MapProviders
import com.example.location.map.MapStatus
import com.example.ui.components.BannerKind
import com.example.ui.components.InfoChip
import com.example.ui.components.KeyValueRow
import com.example.ui.components.LoadingIndicator
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBanner
import com.example.ui.components.VaultTonalButton
import com.example.ui.theme.emphasized
import com.example.ui.theme.mono
import java.util.Locale

@Composable
fun DistanceTab(viewModel: LocationKitViewModel, contentPadding: PaddingValues) {
    val a by viewModel.pointA.collectAsStateWithLifecycle()
    val b by viewModel.pointB.collectAsStateWithLifecycle()
    val slot by viewModel.activeSlot.collectAsStateWithLifecycle()
    val units by viewModel.unitSystem.collectAsStateWithLifecycle()
    val outcome by viewModel.distance.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val requestPermission = rememberLocationPermissionRequest(viewModel)

    var status by remember { mutableStateOf<MapStatus>(MapStatus.Loading) }
    var retry by remember { mutableIntStateOf(0) }
    var camera by remember { mutableStateOf<CameraRequest?>(null) }
    val provider = MapProviders.Default

    // Re-frame the map whenever an endpoint changes.
    LaunchedEffect(a, b) {
        val pa = a
        val pb = b
        camera = when {
            pa != null && pb != null -> CameraRequest.Fit(pa, pb)
            pa != null -> CameraRequest.MoveTo(pa, 12.0)
            pb != null -> CameraRequest.MoveTo(pb, 12.0)
            else -> camera
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item(key = "map") {
            Box(Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(28.dp))) {
                LocationMap(
                    provider = provider,
                    markers = MapMarkers(user = location.fixOrNull?.point, pointA = a, pointB = b, connectAB = true),
                    cameraRequest = camera,
                    retryKey = retry,
                    onStatus = { status = it },
                    initialCenter = location.fixOrNull?.point,
                    onMapTap = viewModel::onMapTap,
                    modifier = Modifier.fillMaxSize()
                )
                MapAttribution(provider, Modifier.align(Alignment.TopStart).padding(8.dp))
                when (val s = status) {
                    MapStatus.Loading -> LoadingIndicator(Modifier.align(Alignment.Center).size(40.dp))
                    MapStatus.Ready -> Unit
                    is MapStatus.Error -> Column(Modifier.align(Alignment.Center).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusBanner(BannerKind.Error, s.message)
                        VaultTonalButton("Retry", { retry++ }, Modifier.fillMaxWidth())
                    }
                }
            }
        }

        item(key = "slot") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("Tap the map to place", subtitle = "Choose which point the next tap sets.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = slot == EndpointSlot.A, onClick = { viewModel.selectSlot(EndpointSlot.A) }, label = { Text("Point A") })
                    FilterChip(selected = slot == EndpointSlot.B, onClick = { viewModel.selectSlot(EndpointSlot.B) }, label = { Text("Point B") })
                }
            }
        }

        item(key = "a") {
            EndpointCard("A", a, viewModel, EndpointSlot.A, requestPermission)
        }
        item(key = "swap") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = viewModel::swapPoints, enabled = a != null || b != null) {
                    androidx.compose.material3.Icon(Icons.Rounded.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Swap A and B")
                }
            }
        }
        item(key = "b") {
            EndpointCard("B", b, viewModel, EndpointSlot.B, requestPermission)
        }

        message?.let { item(key = "message") { StatusBanner(BannerKind.Warning, it) } }

        item(key = "result") { ResultCard(outcome, units, viewModel) }
    }
}

@Composable
private fun EndpointCard(
    label: String,
    point: GeoPoint?,
    viewModel: LocationKitViewModel,
    slot: EndpointSlot,
    requestPermission: () -> Unit
) {
    var expanded by rememberSaveable(label) { mutableStateOf(false) }
    var latText by rememberSaveable(label, "lat") { mutableStateOf("") }
    var lonText by rememberSaveable(label, "lon") { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val tint = if (slot == EndpointSlot.A) androidx.compose.ui.graphics.Color(0xFF2E7D32) else androidx.compose.ui.graphics.Color(0xFFC62828)

    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(14.dp).clip(androidx.compose.foundation.shape.CircleShape).background(tint))
                Text("  Point $label", style = MaterialTheme.typography.titleMedium.emphasized(), modifier = Modifier.weight(1f))
                if (point != null) TextButton(onClick = { viewModel.setPoint(slot, null) }) { Text("Clear") }
            }
            Text(
                point?.let { String.format(Locale.US, "%.6f, %.6f", it.latitude, it.longitude) } ?: "Not set",
                style = MaterialTheme.typography.bodyLarge.mono()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VaultTonalButton("My location", {
                    if (!viewModel.hasLocationPermission) requestPermission()
                    viewModel.useMyLocation(slot)
                }, Modifier.weight(1f), Icons.Rounded.MyLocation)
                VaultTonalButton(if (expanded) "Hide entry" else "Enter manually", { expanded = !expanded }, Modifier.weight(1f))
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText, onValueChange = { latText = it; error = null },
                        label = { Text("Latitude (-90 to 90)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = error != null
                    )
                    OutlinedTextField(
                        value = lonText, onValueChange = { lonText = it; error = null },
                        label = { Text("Longitude (-180 to 180)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = error != null,
                        supportingText = error?.let { { Text(it) } }
                    )
                    VaultTonalButton("Set point $label", {
                        val parsed = GeoPoint.parse(latText, lonText)
                        if (parsed == null) error = "Enter a valid latitude and longitude."
                        else { viewModel.setPoint(slot, parsed); error = null }
                    }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun ResultCard(outcome: DistanceOutcome, units: UnitSystem, viewModel: LocationKitViewModel) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (outcome) {
                is DistanceOutcome.Incomplete -> Text(
                    when {
                        !outcome.hasA && !outcome.hasB -> "Set point A and point B to measure the distance."
                        !outcome.hasA -> "Set point A."
                        else -> "Set point B."
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                is DistanceOutcome.Invalid -> StatusBanner(BannerKind.Error, outcome.reason)
                is DistanceOutcome.Success -> {
                    Text("Distance", style = MaterialTheme.typography.labelLarge)
                    Text(outcome.distance.format(units), style = MaterialTheme.typography.displaySmall.emphasized())
                    if (outcome.identical) {
                        Text("Both points are at the same location.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        outcome.bearingDegrees?.let {
                            Text(
                                String.format(Locale.getDefault(), "Initial bearing from A: %.0f° %s (true north)", it, GeoMath.cardinal(it)),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UnitSystem.entries.forEach {
                            FilterChip(
                                selected = units == it, onClick = { viewModel.setUnits(it) },
                                label = { Text(when (it) { UnitSystem.Metric -> "Metric"; UnitSystem.Imperial -> "Imperial"; UnitSystem.Nautical -> "Nautical" }) }
                            )
                        }
                    }
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) { AllUnits(outcome.distance) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AllUnits(d: Distance) {
    KeyValueRow("Meters", String.format(Locale.getDefault(), "%,.1f m", d.meters))
    KeyValueRow("Kilometers", String.format(Locale.getDefault(), "%,.3f km", d.kilometers))
    KeyValueRow("Miles", String.format(Locale.getDefault(), "%,.3f mi", d.miles))
    KeyValueRow("Nautical miles", String.format(Locale.getDefault(), "%,.3f nmi", d.nauticalMiles))
    KeyValueRow("Feet", String.format(Locale.getDefault(), "%,.0f ft", d.feet))
}

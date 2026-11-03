@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.location.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.location.core.AccuracyQuality
import com.example.location.core.Freshness
import com.example.location.core.LocationFix
import com.example.location.core.LocationState
import com.example.location.core.fixOrNull
import com.example.location.domain.GeoMath
import com.example.ui.components.BannerKind
import com.example.ui.components.InfoChip
import com.example.ui.components.KeyValueRow
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBanner
import com.example.ui.components.VaultTonalButton
import com.example.ui.theme.emphasized
import com.example.ui.theme.mono
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GpsTab(viewModel: LocationKitViewModel, contentPadding: PaddingValues) {
    val state by viewModel.location.collectAsStateWithLifecycle()
    // Re-evaluates age every second so "Live / Stale / Old" never sticks.
    val nowNanos by produceState(SystemClock.elapsedRealtimeNanos()) {
        while (true) { delay(1_000); value = SystemClock.elapsedRealtimeNanos() }
    }
    val fix = state.fixOrNull

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item(key = "status") { LocationStatusPanel(state, viewModel) }

        if (fix != null) {
            item(key = "coords") { CoordinatesCard(fix, nowNanos) }
            item(key = "details") { DetailsCard(fix, nowNanos) }
        }
        if (state is LocationState.Active && fix != null && fix.isMock) {
            item(key = "mock") { StatusBanner(BannerKind.Warning, "This position comes from a mock location provider.") }
        }
    }
}

@Composable
private fun CoordinatesCard(fix: LocationFix, nowNanos: Long) {
    val context = LocalContext.current
    val fresh = fix.freshness(nowNanos)
    val text = String.format(Locale.US, "%.6f, %.6f", fix.point.latitude, fix.point.longitude)
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        androidx.compose.foundation.layout.Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(
                    when (fresh) { Freshness.Live -> "Live"; Freshness.Stale -> "Stale"; Freshness.Old -> "Old position" },
                    containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface
                )
                InfoChip(
                    when (fix.accuracyQuality()) {
                        AccuracyQuality.High -> "High accuracy"; AccuracyQuality.Good -> "Good accuracy"
                        AccuracyQuality.Fair -> "Fair accuracy"; AccuracyQuality.Poor -> "Low accuracy"; AccuracyQuality.Unknown -> "Accuracy unknown"
                    },
                    containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
            Text("Latitude", style = MaterialTheme.typography.labelMedium)
            Text(String.format(Locale.US, "%.6f°", fix.point.latitude), style = MaterialTheme.typography.headlineMedium.emphasized().mono())
            Text("Longitude", style = MaterialTheme.typography.labelMedium)
            Text(String.format(Locale.US, "%.6f°", fix.point.longitude), style = MaterialTheme.typography.headlineMedium.emphasized().mono())
            VaultTonalButton("Copy coordinates", {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Coordinates", text))
            }, Modifier.fillMaxWidth(), Icons.Rounded.ContentCopy)
        }
    }
}

@Composable
private fun DetailsCard(fix: LocationFix, nowNanos: Long) {
    val age = fix.ageMillis(nowNanos) / 1000
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Details")
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            androidx.compose.foundation.layout.Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                KeyValueRow("Accuracy", fix.accuracyMeters?.let { String.format(Locale.getDefault(), "±%.0f m", it) } ?: "Not reported")
                KeyValueRow("Updated", if (age < 2) "Just now" else "$age s ago")
                KeyValueRow("Fix time", DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(fix.timeMillis)))
                KeyValueRow("Altitude", fix.altitudeMeters?.let { String.format(Locale.getDefault(), "%.0f m", it) } ?: "Not reported")
                KeyValueRow("Speed", fix.speedMps?.let { String.format(Locale.getDefault(), "%.1f km/h", it * 3.6f) } ?: "Not reported")
                KeyValueRow("Course", fix.bearingDegrees?.let { String.format(Locale.getDefault(), "%.0f° %s", it, GeoMath.cardinal(it.toDouble())) } ?: "Not reported")
                KeyValueRow("Provider", fix.provider ?: "Unknown")
            }
        }
    }
}

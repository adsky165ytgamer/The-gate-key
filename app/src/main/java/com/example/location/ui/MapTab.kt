@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.location.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.location.core.LocationState
import com.example.location.core.fixOrNull
import com.example.location.map.CameraRequest
import com.example.location.map.LocationMap
import com.example.location.map.MapMarkers
import com.example.location.map.MapProviders
import com.example.location.map.MapStatus
import com.example.ui.components.BannerKind
import com.example.ui.components.LoadingIndicator
import com.example.ui.components.StatusBanner
import com.example.ui.components.VaultTonalButton

@Composable
fun MapTab(viewModel: LocationKitViewModel, contentPadding: PaddingValues) {
    val location by viewModel.location.collectAsStateWithLifecycle()
    val fix = location.fixOrNull
    var status by remember { mutableStateOf<MapStatus>(MapStatus.Loading) }
    var retry by remember { mutableIntStateOf(0) }
    var camera by remember { mutableStateOf<CameraRequest?>(null) }
    var showLocationPanel by remember { mutableStateOf(false) }
    val provider = MapProviders.Default

    Box(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .clip(RoundedCornerShape(32.dp))
    ) {
        LocationMap(
            provider = provider,
            markers = MapMarkers(user = fix?.point),
            cameraRequest = camera,
            retryKey = retry,
            onStatus = { status = it },
            initialCenter = fix?.point,
            modifier = Modifier.fillMaxSize()
        )
        MapAttribution(provider, Modifier.align(Alignment.TopStart).padding(10.dp))

        when (val s = status) {
            MapStatus.Loading -> LoadingIndicator(Modifier.align(Alignment.Center).size(48.dp))
            MapStatus.Ready -> Unit
            is MapStatus.Error -> Column(
                Modifier.align(Alignment.Center).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusBanner(BannerKind.Error, s.message)
                VaultTonalButton("Retry", { retry++ }, Modifier.fillMaxWidth())
            }
        }

        if (showLocationPanel && fix == null) {
            LocationStatusPanel(location, viewModel, Modifier.align(Alignment.TopCenter).padding(top = 52.dp, start = 16.dp, end = 16.dp))
        }

        Column(
            Modifier.align(Alignment.BottomEnd).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalIconButton(onClick = { camera = CameraRequest.ZoomBy(1.0) }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = "Zoom in")
            }
            FilledTonalIconButton(onClick = { camera = CameraRequest.ZoomBy(-1.0) }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Remove, contentDescription = "Zoom out")
            }
            FilledTonalIconButton(
                onClick = {
                    val f = fix
                    if (f != null) camera = CameraRequest.MoveTo(f.point, 15.0) else showLocationPanel = true
                    if (location is LocationState.Idle || location is LocationState.PermissionRequired) viewModel.refreshLocation()
                },
                modifier = Modifier.size(56.dp)
            ) { Icon(Icons.Rounded.MyLocation, contentDescription = "Go to my location") }
        }
    }
}

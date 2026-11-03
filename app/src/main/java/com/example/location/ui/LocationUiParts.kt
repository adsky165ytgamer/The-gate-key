@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.location.ui

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.location.core.LocationState
import com.example.location.map.MapProvider
import com.example.ui.components.BannerKind
import com.example.ui.components.LoadingIndicator
import com.example.ui.components.StatusBanner
import com.example.ui.components.VaultButton
import com.example.ui.components.VaultTonalButton

/** Returns a function that asks for location permission (precise + approximate) and refreshes the engine afterwards. */
@Composable
fun rememberLocationPermissionRequest(viewModel: LocationKitViewModel): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.refreshLocation()
    }
    return {
        launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
}

/**
 * Explains and resolves every non-positioned location state (no permission, location off, searching, error).
 * Shows nothing when a fix is available.
 */
@Composable
fun LocationStatusPanel(
    state: LocationState,
    viewModel: LocationKitViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val requestPermission = rememberLocationPermissionRequest(viewModel)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (state) {
            LocationState.Idle -> Unit
            LocationState.PermissionRequired -> {
                StatusBanner(BannerKind.Info, "Location permission is needed to show your position.")
                VaultButton("Allow location", requestPermission, Modifier.fillMaxWidth(), Icons.Rounded.MyLocation)
            }
            LocationState.ServicesDisabled -> {
                StatusBanner(BannerKind.Warning, "Location is turned off on this device.")
                VaultTonalButton(
                    "Open location settings",
                    {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    },
                    Modifier.fillMaxWidth(), Icons.Rounded.LocationOff
                )
                VaultTonalButton("Check again", { viewModel.refreshLocation() }, Modifier.fillMaxWidth())
            }
            is LocationState.Searching -> {
                if (state.lastKnown == null) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                        androidx.compose.foundation.layout.Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            LoadingIndicator(Modifier.size(24.dp))
                            Text("  Searching for GPS signal", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            is LocationState.Failed -> StatusBanner(BannerKind.Error, state.message)
            is LocationState.Active -> Unit
        }
    }
}

@Composable
fun MapAttribution(provider: MapProvider, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    ) {
        Text(
            provider.attribution,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

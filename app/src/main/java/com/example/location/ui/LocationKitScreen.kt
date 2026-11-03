@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.location.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class LocationTool(val title: String, val icon: ImageVector, val tag: String) {
    Map("Map Explorer", Icons.Rounded.Map, "tab_map"),
    Compass("Compass", Icons.Rounded.Explore, "tab_compass"),
    Gps("GPS Coordinates", Icons.Rounded.GpsFixed, "tab_gps"),
    Distance("Distance", Icons.Rounded.Straighten, "tab_distance")
}

/** Location Kit: one tool selector, one working area. Only the selected tool is composed, so sensors and updates stop when you leave a tool. */
@Composable
fun LocationKitScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: LocationKitViewModel = viewModel()
) {
    val ld = LocalLayoutDirection.current
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val tools = LocationTool.entries

    Column(modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { positions ->
                if (selectedIndex in positions.indices) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(positions[selectedIndex]).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                        height = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            divider = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = contentPadding.calculateStartPadding(ld),
                    top = contentPadding.calculateTopPadding(),
                    end = contentPadding.calculateEndPadding(ld)
                )
                .testTag("location_tools_tabs")
        ) {
            tools.forEachIndexed { index, tool ->
                val isSelected = index == selectedIndex
                Tab(
                    selected = isSelected,
                    onClick = { selectedIndex = index },
                    text = {
                        Text(
                            tool.title,
                            style = MaterialTheme.typography.labelLarge.let { if (isSelected) it.copy(fontWeight = FontWeight.Bold) else it },
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = { Icon(tool.icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(tool.tag)
                )
            }
        }

        val toolPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(ld),
            top = 12.dp,
            end = contentPadding.calculateEndPadding(ld),
            bottom = contentPadding.calculateBottomPadding()
        )
        Box(Modifier.fillMaxSize()) {
            when (tools[selectedIndex]) {
                LocationTool.Map -> MapTab(viewModel, toolPadding)
                LocationTool.Compass -> CompassTab(viewModel, toolPadding)
                LocationTool.Gps -> GpsTab(viewModel, toolPadding)
                LocationTool.Distance -> DistanceTab(viewModel, toolPadding)
            }
        }
    }
}

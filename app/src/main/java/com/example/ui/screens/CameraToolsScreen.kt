@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui.screens

import androidx.compose.animation.core.Animatable
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
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.camera.imageinfo.ImageInfoScreen
import com.example.camera.magnifier.MagnifierScreen
import com.example.camera.recognition.QrScreen
import com.example.camera.scanner.ScannerScreen

private enum class CameraTool(val title: String, val icon: ImageVector, val tag: String) {
    Scanner("Document Scanner", Icons.Rounded.DocumentScanner, "tab_scanner"),
    Codes("QR & Barcode", Icons.Rounded.QrCodeScanner, "tab_codes"),
    Magnifier("Magnifier", Icons.Rounded.ZoomIn, "tab_magnifier"),
    ImageInfo("Image Information", Icons.Rounded.Image, "tab_image_info")
}

/**
 * Camera Tools: one horizontal tool selector, one working area.
 * Only the selected tool is composed, so only one camera session ever exists; the camera permission is
 * requested by the tool that needs it (Image Information never asks).
 */
@Composable
fun CameraToolsScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val tools = CameraTool.entries
    val selected = tools[selectedIndex]

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
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    top = contentPadding.calculateTopPadding(),
                    end = contentPadding.calculateEndPadding(layoutDirection)
                )
                .testTag("camera_tools_tabs")
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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
            start = contentPadding.calculateStartPadding(layoutDirection),
            top = 12.dp,
            end = contentPadding.calculateEndPadding(layoutDirection),
            bottom = contentPadding.calculateBottomPadding()
        )

        // Fade the new tool in. The old tool is removed immediately so two camera sessions never overlap.
        val alpha = remember(selected) { Animatable(0f) }
        LaunchedEffect(selected) { alpha.animateTo(1f, com.example.ui.theme.VaultMotion.defaultEffectsSpec()) }

        Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha.value }) {
            when (selected) {
                CameraTool.Scanner -> ScannerScreen(toolPadding)
                CameraTool.Codes -> QrScreen(toolPadding)
                CameraTool.Magnifier -> MagnifierScreen(toolPadding)
                CameraTool.ImageInfo -> ImageInfoScreen(toolPadding)
            }
        }
    }
}

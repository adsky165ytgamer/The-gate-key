package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.camera.imageinfo.ImageInfoScreen
import com.example.camera.magnifier.MagnifierScreen
import com.example.camera.recognition.QrScreen
import com.example.camera.scanner.ScannerScreen
import com.example.location.ui.CompassTab
import com.example.location.ui.DistanceTab
import com.example.location.ui.GpsTab
import com.example.location.ui.LocationKitViewModel
import com.example.location.ui.MapTab
import com.example.ui.VaultViewModel
import com.example.ui.components.ToolGrid
import com.example.ui.components.ToolHelpSheet
import com.example.ui.registry.ToolCategory
import com.example.ui.registry.ToolId
import com.example.ui.theme.VaultMotion

/**
 * One screen for every type (Security & Encryption, Camera Tools, Location Kit).
 *
 * It shows the type's tools as a 2-column grid. Opening a tile zooms into that tool; back zooms out to the grid.
 * Only the open tool is composed, so only one camera session or sensor stream is ever active, and the camera
 * permission is requested by the tool that needs it (Image Information never asks).
 */
@Composable
fun ToolHubScreen(
    category: ToolCategory,
    viewModel: VaultViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val activeTool by viewModel.activeTool.collectAsStateWithLifecycle()
    val tool = activeTool?.takeIf { it.category == category }
    var helpTool by remember { mutableStateOf<ToolId?>(null) }

    // The tiles rise in once, the first time this type is opened; coming back from a tool only zooms.
    var gridSeen by rememberSaveable(category.name) { mutableStateOf(false) }
    LaunchedEffect(tool) { if (tool != null) gridSeen = true }

    val layoutDirection = LocalLayoutDirection.current
    val toolPadding = PaddingValues(
        start = contentPadding.calculateStartPadding(layoutDirection),
        top = contentPadding.calculateTopPadding() + 4.dp,
        end = contentPadding.calculateEndPadding(layoutDirection),
        bottom = contentPadding.calculateBottomPadding()
    )

    AnimatedContent(
        targetState = tool,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            val opening = targetState != null
            val enterFrom = if (opening) 0.92f else 1.06f
            val exitTo = if (opening) 1.06f else 0.92f
            (fadeIn(VaultMotion.defaultEffectsSpec<Float>()) +
                scaleIn(VaultMotion.defaultSpatialSpec<Float>(), initialScale = enterFrom)) togetherWith
                (fadeOut(VaultMotion.fastEffectsSpec<Float>()) +
                    scaleOut(VaultMotion.fastSpatialSpec<Float>(), targetScale = exitTo))
        },
        label = "hub-${category.name}"
    ) { target ->
        if (target == null) {
            ToolGrid(
                category = category,
                contentPadding = contentPadding,
                staggered = !gridSeen,
                onOpen = viewModel::openTool,
                onHelp = { helpTool = it }
            )
        } else {
            ToolContent(target, viewModel, toolPadding)
        }
    }

    helpTool?.let { ToolHelpSheet(tool = it, onDismiss = { helpTool = null }, onOpen = viewModel::openTool) }
}

@Composable
private fun ToolContent(tool: ToolId, vault: VaultViewModel, padding: PaddingValues) {
    when (tool) {
        ToolId.ENCRYPT -> EncryptScreen(vault, padding)
        ToolId.DECRYPT -> DecryptScreen(vault, padding)
        ToolId.BATCH -> BatchScreen(vault, padding)
        ToolId.RECOVERY -> RecoveryKeyScreen(vault, padding)
        ToolId.MIGRATION -> MigrationScreen(vault, padding)
        ToolId.AUDIT -> DeviceSecurityScreen(vault, padding)
        ToolId.SCANNER -> ScannerScreen(padding)
        ToolId.CODES -> QrScreen(padding)
        ToolId.MAGNIFIER -> MagnifierScreen(padding)
        ToolId.IMAGE_INFO -> ImageInfoScreen(padding)
        ToolId.MAP, ToolId.COMPASS, ToolId.GPS, ToolId.DISTANCE -> LocationToolContent(tool, padding)
    }
}

@Composable
private fun LocationToolContent(tool: ToolId, padding: PaddingValues) {
    // Shared across the four location tools; created when the first one opens.
    val kit: LocationKitViewModel = viewModel()
    when (tool) {
        ToolId.MAP -> MapTab(kit, padding)
        ToolId.COMPASS -> CompassTab(kit, padding)
        ToolId.GPS -> GpsTab(kit, padding)
        else -> DistanceTab(kit, padding)
    }
}

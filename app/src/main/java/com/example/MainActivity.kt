@file:OptIn(ExperimentalMaterial3Api::class)

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.VaultScreen
import com.example.ui.VaultViewModel
import com.example.ui.components.InfoChip
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ToolHubScreen
import com.example.ui.components.CategoryHelpSheet
import com.example.ui.components.ToolHelpSheet
import com.example.ui.registry.ToolCategory
import com.example.ui.theme.ADSkyVaultTheme
import com.example.ui.theme.VaultMotion
import com.example.ui.theme.VaultTheme
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class MainActivity : ComponentActivity() {
    private val viewModel: VaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ADSkyVaultTheme {
                VaultApp(viewModel)
            }
        }
    }
}

// ───────────────────────────── navigation helpers ─────────────────────────────

private fun VaultScreen.title() = when (this) {
    VaultScreen.HOME -> "AD Sky Vault"
    VaultScreen.ABOUT -> "About AD Sky Vault"
    else -> ToolCategory.forScreen(this)?.title ?: "AD Sky Vault"
}

private fun VaultScreen.subtitle() = when (this) {
    VaultScreen.HOME -> ""
    VaultScreen.ABOUT -> "Specifications and security model"
    else -> ToolCategory.forScreen(this)?.tagline ?: ""
}

private const val MAX_CONTENT_WIDTH_DP = 880

// ───────────────────────────── app shell ─────────────────────────────

@Composable
private fun VaultApp(viewModel: VaultViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeTool by viewModel.activeTool.collectAsStateWithLifecycle()
    val category = ToolCategory.forScreen(currentScreen)
    val openTool = activeTool?.takeIf { it.category == category }
    var helpOpen by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val layoutDirection = LocalLayoutDirection.current
    val fontScale = LocalDensity.current.fontScale

    // Every screen starts with a fully expanded title.
    LaunchedEffect(currentScreen, openTool) {
        scrollBehavior.state.heightOffset = 0f
        scrollBehavior.state.contentOffset = 0f
    }

    // Back goes one level up: open tool -> its grid -> Home.
    val goBack: () -> Unit = {
        if (openTool != null) viewModel.closeTool() else viewModel.navigateTo(VaultScreen.HOME)
    }

    // Predictive back: content shrinks under the finger, then returns home.
    val backScale = remember { Animatable(1f) }
    PredictiveBackHandler(enabled = currentScreen != VaultScreen.HOME) { progress ->
        try {
            progress.collect { backScale.snapTo(1f - 0.1f * it.progress) }
            goBack()
            backScale.animateTo(1f, VaultMotion.defaultSpatialSpec())
        } catch (e: CancellationException) {
            withContext(NonCancellable) { backScale.animateTo(1f, VaultMotion.fastSpatialSpec()) }
            throw e
        }
    }

    val safeInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = safeInsets,
        topBar = {
            val backButton: @Composable () -> Unit = {
                if (currentScreen != VaultScreen.HOME) {
                    IconButton(
                        onClick = goBack,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier.testTag("top_bar_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = if (openTool != null) "Back to ${category?.title}" else "Back to home"
                        )
                    }
                }
            }
            val helpButton: @Composable () -> Unit = {
                if (category != null) {
                    IconButton(
                        onClick = { helpOpen = true },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier.testTag("top_bar_help_button")
                    ) { Icon(Icons.Rounded.Info, contentDescription = "What is this?") }
                }
            }
            val title = openTool?.title ?: currentScreen.title()
            val subtitle = if (openTool != null) openTool.category.title else currentScreen.subtitle()
            val titleContent: @Composable () -> Unit = {
                Column {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (subtitle.isNotEmpty()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (openTool != null) {
                // Inside a tool the bar stays compact so cameras and maps get the room.
                TopAppBar(
                    title = titleContent,
                    navigationIcon = backButton,
                    actions = { helpButton() },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            } else {
                LargeTopAppBar(
                    title = titleContent,
                    navigationIcon = backButton,
                    actions = { helpButton() },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            val gutter = ((maxWidth - MAX_CONTENT_WIDTH_DP.dp) / 2).coerceAtLeast(0.dp)
            val contentPadding = PaddingValues(
                start = innerPadding.calculateStartPadding(layoutDirection) + gutter,
                top = innerPadding.calculateTopPadding() + 8.dp,
                end = innerPadding.calculateEndPadding(layoutDirection) + gutter,
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = MAX_CONTENT_WIDTH_DP.dp)
                        .graphicsLayer {
                            val s = backScale.value
                            scaleX = s
                            scaleY = s
                            shape = RoundedCornerShape(28.dp)
                            clip = s < 1f
                        },
                    transitionSpec = {
                        val dir = if (targetState == VaultScreen.HOME) -1 else 1
                        (slideInHorizontally(VaultMotion.defaultSpatialSpec<IntOffset>()) { (it * 0.16f).toInt() * dir } +
                            fadeIn(VaultMotion.defaultEffectsSpec<Float>())) togetherWith
                            (slideOutHorizontally(VaultMotion.defaultSpatialSpec<IntOffset>()) { -(it * 0.16f).toInt() * dir } +
                                fadeOut(VaultMotion.fastEffectsSpec<Float>())) using
                            SizeTransform(clip = false)
                    },
                    label = "screen-transition"
                ) { screen ->
                    when (screen) {
                        VaultScreen.HOME -> HomeScreen(viewModel, contentPadding)
                        VaultScreen.ABOUT -> AboutScreen(viewModel, contentPadding)
                        else -> ToolHubScreen(
                            category = ToolCategory.forScreen(screen) ?: ToolCategory.SECURITY,
                            viewModel = viewModel,
                            contentPadding = contentPadding
                        )
                    }
                }
            }
        }
    }

    if (helpOpen) {
        val onDismiss = { helpOpen = false }
        if (openTool != null) {
            ToolHelpSheet(tool = openTool, onDismiss = onDismiss)
        } else if (category != null) {
            CategoryHelpSheet(category = category, onDismiss = onDismiss, onOpenTool = viewModel::openTool)
        }
    }
}

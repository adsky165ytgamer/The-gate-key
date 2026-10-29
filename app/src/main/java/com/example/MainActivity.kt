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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PublicOff
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.screens.BatchScreen
import com.example.ui.screens.DecryptScreen
import com.example.ui.screens.DeviceSecurityScreen
import com.example.ui.screens.EncryptScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RecoveryKeyScreen
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

// ───────────────────────────── navigation model ─────────────────────────────

private data class NavDestination(
    val screen: VaultScreen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private val destinations = listOf(
    NavDestination(VaultScreen.HOME, "Vault", Icons.Rounded.Home, Icons.Outlined.Home, "nav_item_home"),
    NavDestination(VaultScreen.ENCRYPT, "Encrypt", Icons.Rounded.Lock, Icons.Outlined.Lock, "nav_item_encrypt"),
    NavDestination(VaultScreen.DECRYPT, "Decrypt", Icons.Rounded.LockOpen, Icons.Outlined.LockOpen, "nav_item_decrypt"),
    NavDestination(VaultScreen.DEVICE_SECURITY, "Security", Icons.Rounded.Security, Icons.Outlined.Security, "nav_item_security"),
)

private fun VaultScreen.isChild() =
    this == VaultScreen.BATCH || this == VaultScreen.RECOVERY_KEY || this == VaultScreen.ABOUT

private fun VaultScreen.depth() = when (this) {
    VaultScreen.HOME -> 0
    VaultScreen.ENCRYPT -> 1
    VaultScreen.DECRYPT -> 2
    VaultScreen.DEVICE_SECURITY -> 3
    else -> 4
}

private fun transitionDirection(from: VaultScreen, to: VaultScreen): Int = when {
    to.isChild() && !from.isChild() -> 1
    from.isChild() && !to.isChild() -> -1
    else -> if (to.depth() >= from.depth()) 1 else -1
}

private fun VaultScreen.title() = when (this) {
    VaultScreen.HOME -> "AD Sky Vault"
    VaultScreen.ENCRYPT -> "Encrypt file"
    VaultScreen.DECRYPT -> "Decrypt & verify"
    VaultScreen.BATCH -> "Batch queue"
    VaultScreen.RECOVERY_KEY -> "Recovery secret"
    VaultScreen.DEVICE_SECURITY -> "Device & security"
    VaultScreen.ABOUT -> "About"
}

private fun VaultScreen.subtitle() = when (this) {
    VaultScreen.HOME -> "Envelope encryption & key recovery"
    VaultScreen.ENCRYPT -> "Seal any file into an .adsky container"
    VaultScreen.DECRYPT -> "Unwrap, authenticate and verify"
    VaultScreen.BATCH -> "Encrypt many files in one pass"
    VaultScreen.RECOVERY_KEY -> "Your master key for emergencies"
    VaultScreen.DEVICE_SECURITY -> "Keystore integrity and audit log"
    VaultScreen.ABOUT -> "Specifications and security model"
}

private const val MAX_CONTENT_WIDTH_DP = 880

// ───────────────────────────── app shell ─────────────────────────────

@Composable
private fun VaultApp(viewModel: VaultViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val layoutDirection = LocalLayoutDirection.current
    val fontScale = LocalDensity.current.fontScale

    // Every screen starts with a fully expanded title.
    LaunchedEffect(currentScreen) {
        scrollBehavior.state.heightOffset = 0f
        scrollBehavior.state.contentOffset = 0f
    }

    // Predictive back: content shrinks under the finger, then returns home.
    val backScale = remember { Animatable(1f) }
    PredictiveBackHandler(enabled = currentScreen != VaultScreen.HOME) { progress ->
        try {
            progress.collect { backScale.snapTo(1f - 0.1f * it.progress) }
            viewModel.navigateTo(VaultScreen.HOME)
            backScale.animateTo(1f, VaultMotion.defaultSpatialSpec())
        } catch (e: CancellationException) {
            withContext(NonCancellable) { backScale.animateTo(1f, VaultMotion.fastSpatialSpec()) }
            throw e
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val useRail = maxWidth >= 600.dp
        val selectedTab = if (currentScreen.isChild()) VaultScreen.HOME else currentScreen
        val safeInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)

        Row(Modifier.fillMaxSize()) {
            if (useRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    windowInsets = safeInsets.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    destinations.forEach { dest ->
                        val selected = selectedTab == dest.screen
                        NavigationRailItem(
                            selected = selected,
                            onClick = { viewModel.navigateTo(dest.screen) },
                            icon = { Icon(if (selected) dest.selectedIcon else dest.unselectedIcon, contentDescription = null) },
                            label = { Text(dest.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.testTag(dest.tag)
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier
                    .weight(1f)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = if (useRail) {
                    safeInsets.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                } else {
                    safeInsets
                },
                topBar = {
                    LargeTopAppBar(
                        title = {
                            Column {
                                Text(
                                    currentScreen.title(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    currentScreen.subtitle(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        navigationIcon = {
                            if (currentScreen != VaultScreen.HOME) {
                                IconButton(
                                    onClick = { viewModel.navigateTo(VaultScreen.HOME) },
                                    modifier = Modifier.testTag("top_bar_back_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
                        actions = { OfflineBadge(compact = fontScale > 1.3f) },
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        scrollBehavior = scrollBehavior
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(modifier = Modifier.testTag("bottom_navigation_bar")) {
                            destinations.forEach { dest ->
                                val selected = selectedTab == dest.screen
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.navigateTo(dest.screen) },
                                    icon = { Icon(if (selected) dest.selectedIcon else dest.unselectedIcon, contentDescription = null) },
                                    label = { Text(dest.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.testTag(dest.tag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .consumeWindowInsets(innerPadding)
                        .imePadding()
                ) {
                    val gutter = ((maxWidth - MAX_CONTENT_WIDTH_DP.dp) / 2).coerceAtLeast(16.dp)
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
                                val dir = transitionDirection(initialState, targetState)
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
                                VaultScreen.ENCRYPT -> EncryptScreen(viewModel, contentPadding)
                                VaultScreen.DECRYPT -> DecryptScreen(viewModel, contentPadding)
                                VaultScreen.BATCH -> BatchScreen(viewModel, contentPadding)
                                VaultScreen.RECOVERY_KEY -> RecoveryKeyScreen(viewModel, contentPadding)
                                VaultScreen.DEVICE_SECURITY -> DeviceSecurityScreen(viewModel, contentPadding)
                                VaultScreen.ABOUT -> AboutScreen(viewModel, contentPadding)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** "Offline" status. Collapses to a dot-sized icon badge when the system font is very large. */
@Composable
private fun OfflineBadge(compact: Boolean) {
    val ext = VaultTheme.colors
    val description = "Offline. This app has no network access."
    if (compact) {
        Surface(
            shape = CircleShape,
            color = ext.successContainer,
            contentColor = ext.onSuccessContainer,
            modifier = Modifier.padding(end = 8.dp).semantics { contentDescription = description }
        ) {
            Icon(Icons.Rounded.PublicOff, contentDescription = null, modifier = Modifier.padding(8.dp).size(18.dp))
        }
    } else {
        InfoChip(
            text = "Offline",
            leading = Icons.Rounded.PublicOff,
            containerColor = ext.successContainer,
            contentColor = ext.onSuccessContainer,
            modifier = Modifier.padding(end = 8.dp).semantics { contentDescription = description }
        )
    }
}

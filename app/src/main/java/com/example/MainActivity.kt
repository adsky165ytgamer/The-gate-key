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
import androidx.compose.material.icons.rounded.PublicOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.screens.SecurityAndEncryptionScreen
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
    VaultScreen.SECURITY_AND_ENCRYPTION -> "Security & Encryption"
    VaultScreen.ABOUT -> "About AD Sky Vault"
    else -> "Security & Encryption"
}

private fun VaultScreen.subtitle() = when (this) {
    VaultScreen.HOME -> "Offline file encryption & recovery"
    VaultScreen.SECURITY_AND_ENCRYPTION -> "Protect files and manage your security tools"
    VaultScreen.ABOUT -> "Specifications and security model"
    else -> "Protect files and manage your security tools"
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

    val safeInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = safeInsets,
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
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to home")
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
                        else -> SecurityAndEncryptionScreen(viewModel, contentPadding)
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

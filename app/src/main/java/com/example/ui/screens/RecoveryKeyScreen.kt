@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.VaultViewModel
import com.example.ui.components.SectionHeader
import com.example.ui.components.ShapeBadge
import com.example.ui.components.VaultButton
import com.example.ui.components.VaultOutlinedButton
import com.example.ui.components.connectedShape
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.VaultBadgeShape
import com.example.ui.theme.VaultMotion
import com.example.ui.theme.VaultTheme
import com.example.ui.theme.emphasized
import com.example.ui.theme.mono

@Composable
fun RecoveryKeyScreen(
    viewModel: VaultViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val ext = VaultTheme.colors
    val isRevealed by viewModel.isRecoverySecretRevealed.collectAsStateWithLifecycle()
    val recoverySecret by viewModel.revealedRecoverySecret.collectAsStateWithLifecycle()
    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }

    // While secret is revealed, block screen capture
    DisposableEffect(isRevealed) {
        val window = (context as? ComponentActivity)?.window
        if (isRevealed) window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = ext.warning) },
            title = { Text("Reveal master recovery secret?") },
            text = {
                Text("Make sure nobody is watching or recording your screen. Anyone with this 256-bit secret can decrypt any .adsky file that carries a recovery envelope.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        viewModel.revealRecoverySecret()
                    },
                    modifier = Modifier.testTag("confirm_reveal_secret_button")
                ) { Text("I understand, reveal") }
            },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") } }
        )
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        if (isWide) {
            // Adaptive 2-column wide layout
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    RecoverySecretCard(
                        isRevealed = isRevealed,
                        recoverySecret = recoverySecret,
                        ext = ext,
                        context = context,
                        onRevealRequest = { showConfirmDialog = true },
                        onHideSecret = { viewModel.hideRecoverySecret() }
                    )
                }

                Column(Modifier.weight(1f)) {
                    RecoveryGuidanceSection()
                }
            }
        } else {
            // Standard single-column flow on compact phone
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item(key = "secret") {
                    RecoverySecretCard(
                        isRevealed = isRevealed,
                        recoverySecret = recoverySecret,
                        ext = ext,
                        context = context,
                        onRevealRequest = { showConfirmDialog = true },
                        onHideSecret = { viewModel.hideRecoverySecret() }
                    )
                }

                item(key = "guidance") {
                    RecoveryGuidanceSection()
                }
            }
        }
    }
}

@Composable
private fun RecoverySecretCard(
    isRevealed: Boolean,
    recoverySecret: String?,
    ext: com.example.ui.theme.VaultExtendedColors,
    context: Context,
    onRevealRequest: () -> Unit,
    onHideSecret: () -> Unit
) {
    Surface(
        Modifier
            .fillMaxWidth()
            .staggeredEntrance(0)
            .testTag("recovery_secret_card"),
        shape = RoundedCornerShape(32.dp),
        color = if (isRevealed) ext.warningContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (isRevealed) ext.onWarningContainer else MaterialTheme.colorScheme.onSurface
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ShapeBadge(
                icon = if (isRevealed) Icons.Rounded.Key else Icons.Rounded.Lock,
                polygon = if (isRevealed) VaultBadgeShape.Sunny else VaultBadgeShape.Cookie9Sided,
                size = 72.dp,
                rotate = isRevealed,
                containerColor = if (isRevealed) ext.warning else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (isRevealed) ext.onWarning else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(16.dp))
            AnimatedContent(
                targetState = isRevealed,
                transitionSpec = { fadeIn(VaultMotion.defaultEffectsSpec()) togetherWith fadeOut(VaultMotion.fastEffectsSpec()) },
                label = "secret-state"
            ) { revealed ->
                if (!revealed) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Secret concealed", style = MaterialTheme.typography.titleLarge.emphasized(FontWeight.Bold), textAlign = TextAlign.Center)
                        Text(
                            "Stored encrypted at rest by Android Keystore. Reveal it to copy or write it down.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
                        )
                        VaultButton(
                            text = "Reveal recovery secret",
                            icon = Icons.Rounded.Visibility,
                            onClick = onRevealRequest,
                            colors = ButtonDefaults.buttonColors(containerColor = ext.warning, contentColor = ext.onWarning),
                            modifier = Modifier.fillMaxWidth().testTag("reveal_recovery_secret_button")
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Master secret", style = MaterialTheme.typography.titleLarge.emphasized(FontWeight.Bold))
                        Surface(
                            Modifier.fillMaxWidth().padding(vertical = 16.dp).testTag("revealed_secret_box"),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            val parts = (recoverySecret ?: "Generating…").split("-")
                            FlowRow(
                                Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                parts.forEach {
                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.titleSmall.mono().emphasized(),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                        FlowRow(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            VaultButton(
                                text = "Copy secret",
                                icon = Icons.Rounded.ContentCopy,
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("AD Sky Recovery Secret", recoverySecret ?: "")
                                    if (Build.VERSION.SDK_INT >= 33) {
                                        clip.description.extras = PersistableBundle().apply {
                                            putBoolean("android.content.extra.IS_SENSITIVE", true)
                                        }
                                    }
                                    clipboard.setPrimaryClip(clip)
                                    if (Build.VERSION.SDK_INT < 33) {
                                        Toast.makeText(context, "Recovery secret copied", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("copy_recovery_secret_button")
                            )
                            VaultOutlinedButton(
                                text = "Conceal",
                                icon = Icons.Rounded.VisibilityOff,
                                onClick = onHideSecret,
                                contentColor = ext.onWarningContainer,
                                modifier = Modifier.testTag("hide_recovery_secret_button")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecoveryGuidanceSection() {
    Column(Modifier.staggeredEntrance(1).testTag("recovery_guidance_card"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Offline security principles")
        val items = listOf(
            Triple(Icons.Rounded.Warning, "Permanent loss", "Lose this phone and this secret and your files cannot be recovered by anyone. There are no servers, backdoors, or accounts."),
            Triple(Icons.Rounded.SwapHoriz, "Switching phones", "Install AD Sky Vault on the new device and use this secret while decrypting. It re-binds your files to the new Keystore."),
            Triple(Icons.Rounded.EditNote, "Where to keep it", "Write it on paper and store it somewhere physically safe, or keep it in an offline password manager.")
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items.forEachIndexed { i, (icon, title, text) -> GuidanceItem(icon, title, text, connectedShape(i, items.size)) }
        }
    }
}

@Composable
fun GuidanceItem(icon: ImageVector, title: String, description: String, shape: Shape, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = shape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall.emphasized())
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

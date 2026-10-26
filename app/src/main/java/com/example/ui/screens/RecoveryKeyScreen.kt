package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.VaultViewModel
import com.example.ui.theme.VaultAccentAmber
import com.example.ui.theme.VaultAccentGreen
import com.example.ui.theme.VaultAccentRose
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultCard
import com.example.ui.theme.VaultPrimary
import com.example.ui.theme.VaultSurfaceVariant
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

@Composable
fun RecoveryKeyScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRevealed by viewModel.isRecoverySecretRevealed.collectAsState()
    val recoverySecret by viewModel.revealedRecoverySecret.collectAsState()

    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = VaultAccentAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Reveal Master Recovery Secret?", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Ensure nobody is watching your screen or recording. Anyone with this 256-bit recovery secret can decrypt any .adsky file that has a recovery envelope attached.",
                    fontSize = 13.sp,
                    color = VaultTextSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.revealRecoverySecret()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultAccentAmber),
                    modifier = Modifier.testTag("confirm_reveal_secret_button")
                ) {
                    Text("I Understand, Reveal Secret", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = VaultTextSecondary)
                }
            },
            containerColor = VaultCard,
            shape = RoundedCornerShape(12.dp)
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Installation Recovery Secret",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary
            )
            Text(
                text = "Your high-entropy cryptographic master key for emergency recovery and device migration.",
                fontSize = 12.sp,
                color = VaultTextSecondary
            )
        }

        // Secret Container Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recovery_secret_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isRevealed) VaultAccentAmber else VaultBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(if (isRevealed) Color(0x33F59E0B) else VaultSurfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRevealed) Icons.Default.Key else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isRevealed) VaultAccentAmber else VaultTextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isRevealed) {
                        Text(
                            text = "Recovery Secret Concealed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = VaultTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Stored encrypted at rest with Android Keystore. Tap below to inspect and securely export your recovery secret.",
                            fontSize = 12.sp,
                            color = VaultTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showConfirmDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reveal_recovery_secret_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = VaultAccentAmber),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reveal Recovery Secret", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "Cryptographic Master Secret",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = VaultAccentAmber
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Secret Token Display Box
                        Surface(
                            color = VaultSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder),
                            modifier = Modifier.fillMaxWidth().testTag("revealed_secret_box")
                        ) {
                            Text(
                                text = recoverySecret ?: "Generating...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = VaultPrimary,
                                modifier = Modifier.padding(14.dp),
                                textAlign = TextAlign.Center,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("AD Sky Recovery Secret", recoverySecret ?: "")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Recovery secret copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("copy_recovery_secret_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = VaultPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Secret", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.hideRecoverySecret() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hide_recovery_secret_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultTextSecondary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.VisibilityOff, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Conceal", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Educational Guidance on Offline Security
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recovery_guidance_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = VaultPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Offline Security Principles & Warnings",
                            fontWeight = FontWeight.Bold,
                            color = VaultTextPrimary,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    GuidanceBullet(
                        title = "Permanent Loss Warning",
                        description = "If you uninstall the application, wipe device data, lose this phone, and lose your recovery secret, your encrypted files CANNOT be recovered by anyone. There are no backdoors, servers, or account resets."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    GuidanceBullet(
                        title = "Multi-Device Migration",
                        description = "When switching to a new phone, install AD Sky Vault on Device B, and use this Recovery Secret during decryption or the Migration Assistant to unlock your files and bind them to Device B's Keystore."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    GuidanceBullet(
                        title = "Storage Recommendation",
                        description = "Write down this recovery secret on paper and keep it in a physically secure place (e.g. fireproof safe), or store it in an encrypted password manager."
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun GuidanceBullet(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .background(VaultPrimary, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = VaultTextPrimary
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = VaultTextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}

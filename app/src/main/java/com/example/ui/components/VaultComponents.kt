package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CipherAlgorithm
import com.example.crypto.HardwareSecurityInfo
import com.example.ui.theme.VaultAccentAmber
import com.example.ui.theme.VaultAccentGreen
import com.example.ui.theme.VaultAccentRose
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultCard
import com.example.ui.theme.VaultPrimary
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceVariant
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

@Composable
fun HardwareSecurityPill(
    info: HardwareSecurityInfo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val badgeColor = if (info.isInsideSecureHardware) VaultAccentGreen else VaultAccentAmber
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("hardware_security_pill"),
        color = VaultSurfaceVariant,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(badgeColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = info.securityLevelName,
                color = VaultTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Security Level",
                tint = badgeColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun AlgorithmSelectionCard(
    algorithm: CipherAlgorithm,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) VaultPrimary else VaultBorder
    val backgroundColor = if (isSelected) VaultSurfaceVariant else VaultCard

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("algo_card_${algorithm.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(2.dp, if (isSelected) VaultPrimary else VaultTextSecondary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(VaultPrimary, CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = algorithm.displayName,
                        fontWeight = FontWeight.Bold,
                        color = VaultTextPrimary,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (algorithm == CipherAlgorithm.AES_256_GCM) Color(0xFF0369A1) else Color(0xFF4C1D95),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "0x0${algorithm.id}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = algorithm.description,
                    fontSize = 12.sp,
                    color = VaultTextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun WeakPinWarningBox(
    input: String,
    modifier: Modifier = Modifier
) {
    val isFourDigits = input.length == 4 && input.all { it.isDigit() }
    AnimatedVisibility(visible = isFourDigits, modifier = modifier) {
        Surface(
            color = Color(0x33F59E0B),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, VaultAccentAmber),
            modifier = Modifier.fillMaxWidth().testTag("weak_pin_warning")
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = VaultAccentAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Security Notice: A 4-digit PIN is susceptible to brute-force attacks if access to the container is obtained. A strong passphrase (8+ characters) is strongly recommended.",
                    fontSize = 12.sp,
                    color = VaultAccentAmber,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun OperationProgressCard(
    isProcessing: Boolean,
    progress: Float,
    statusMessage: String,
    currentChunk: Int,
    bytesProcessed: Long,
    totalBytes: Long,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isProcessing) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("operation_progress_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = VaultSurfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, VaultPrimary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Processing Cryptographic Stream",
                    fontWeight = FontWeight.Bold,
                    color = VaultPrimary,
                    fontSize = 14.sp
                )
                if (currentChunk > 0) {
                    Text(
                        text = "Chunk #$currentChunk",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = VaultTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = VaultPrimary,
                trackColor = VaultBorder
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = statusMessage,
                    fontSize = 12.sp,
                    color = VaultTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (totalBytes > 0) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VaultPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cancel_operation_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultAccentRose),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultAccentRose)
            ) {
                Text("Cancel Operation", fontSize = 13.sp)
            }
        }
    }
}



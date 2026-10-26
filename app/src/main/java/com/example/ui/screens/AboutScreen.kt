package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.ContainerConstants
import com.example.ui.VaultViewModel
import com.example.ui.theme.VaultAccentGreen
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultCard
import com.example.ui.theme.VaultPrimary
import com.example.ui.theme.VaultSurfaceVariant
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

@Composable
fun AboutScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "About AD Sky Vault",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary
            )
            Text(
                text = "Applied cryptography and envelope encryption specifications.",
                fontSize = 12.sp,
                color = VaultTextSecondary
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("offline_manifesto_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PublicOff,
                            contentDescription = null,
                            tint = VaultAccentGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Zero Network Air-Gap Guarantee",
                            fontWeight = FontWeight.Bold,
                            color = VaultTextPrimary,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "AD Sky Vault contains zero internet permissions, zero remote analytics, and zero cloud databases. All cryptographic operations execute strictly on-device using Android Keystore and verified cryptographic primitives.",
                        fontSize = 12.sp,
                        color = VaultTextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("container_spec_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = VaultPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = ".adsky Binary Container Specification",
                            fontWeight = FontWeight.Bold,
                            color = VaultTextPrimary,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    SpecRow(field = "Magic Bytes", spec = "ADSKY-VAULT (11 bytes ASCII)")
                    SpecRow(field = "Version", spec = "v1 (uint16 big-endian)")
                    SpecRow(field = "Algorithm IDs", spec = "0x01: AES-256-GCM, 0x02: XChaCha20-Poly1305")
                    SpecRow(field = "Envelope Architecture", spec = "Custom PIN/Pass (Argon2id), Keystore, Recovery Secret")
                    SpecRow(field = "Streaming Chunks", spec = "Nominal 1 MiB chunk size with monotonic index & unique nonces")
                    SpecRow(field = "Final Record", spec = "Total chunk count & byte count verification record")
                    SpecRow(field = "Digital Signature", spec = "Android Keystore ECDSA P-256 over container SHA-256")
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("crypto_libraries_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Vetted Cryptographic Stack",
                        fontWeight = FontWeight.Bold,
                        color = VaultPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Google Tink: Authenticated Encryption with Associated Data (AEAD) & XChaCha20-Poly1305\n" +
                                "• Bouncy Castle: Argon2id memory-hard key derivation\n" +
                                "• Android Keymaster: Hardware TEE / StrongBox-isolated non-exportable master keys\n" +
                                "• Storage Access Framework: Granular document access without broad storage permissions",
                        fontSize = 12.sp,
                        color = VaultTextSecondary,
                        lineHeight = 18.sp
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
fun SpecRow(
    field: String,
    spec: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(
            text = field,
            fontWeight = FontWeight.SemiBold,
            color = VaultTextPrimary,
            fontSize = 12.sp
        )
        Text(
            text = spec,
            color = VaultPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
    }
}

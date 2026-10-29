@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PublicOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.VaultViewModel
import com.example.ui.components.SectionHeader
import com.example.ui.components.ShapeBadge
import com.example.ui.components.connectedShape
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.VaultBadgeShape
import com.example.ui.theme.VaultTheme
import com.example.ui.theme.emphasized
import com.example.ui.theme.mono

private val specRows = listOf(
    "Magic bytes" to "ADSKY-VAULT (11 bytes ASCII)",
    "Version" to "v1 (uint16 big-endian)",
    "Algorithm IDs" to "0x01 AES-256-GCM · 0x02 XChaCha20-Poly1305",
    "Envelopes" to "PIN/passphrase (Argon2id), Keystore, recovery secret",
    "Streaming chunks" to "Nominal 1 MiB, monotonic index, unique nonces",
    "Final record" to "Total chunk and byte count verification",
    "Signature" to "Keystore ECDSA P-256 over container SHA-256",
)

private val stackRows = listOf(
    "Google Tink" to "AEAD and XChaCha20-Poly1305",
    "Bouncy Castle" to "Argon2id memory-hard key derivation",
    "Android Keymaster" to "TEE / StrongBox non-exportable master keys",
    "Storage Access Framework" to "File access without broad storage permissions",
)

@Composable
fun AboutScreen(
    viewModel: VaultViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val ext = VaultTheme.colors

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
                // Left Column: Air gap manifesto + Vetted stack
                Column(
                    Modifier
                        .weight(1f)
                        .padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    AirgapCard(ext = ext)
                    CryptoStackSection()
                }

                // Right Column: Specification details
                Column(
                    Modifier
                        .weight(1f)
                        .padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    ContainerSpecSection()
                }
            }
        } else {
            // Single-column flow on compact phone
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item(key = "airgap") {
                    AirgapCard(ext = ext)
                }

                item(key = "spec") {
                    ContainerSpecSection()
                }

                item(key = "stack") {
                    CryptoStackSection()
                }
            }
        }
    }
}

@Composable
private fun AirgapCard(ext: com.example.ui.theme.VaultExtendedColors) {
    Surface(
        Modifier
            .fillMaxWidth()
            .staggeredEntrance(0)
            .testTag("offline_manifesto_card"),
        shape = RoundedCornerShape(32.dp),
        color = ext.successContainer,
        contentColor = ext.onSuccessContainer
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShapeBadge(
                    Icons.Rounded.PublicOff,
                    polygon = VaultBadgeShape.Cookie12Sided,
                    size = 60.dp,
                    rotate = true,
                    containerColor = ext.success,
                    contentColor = ext.onSuccess
                )
                Spacer(Modifier.width(16.dp))
                Text("Zero-network air gap", style = MaterialTheme.typography.titleLarge.emphasized(), modifier = Modifier.weight(1f))
            }
            Text(
                "No internet permission, no analytics, no cloud database. Every cryptographic operation runs on this device using Android Keystore and vetted primitives.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}

@Composable
private fun ContainerSpecSection() {
    Column(Modifier.staggeredEntrance(1).testTag("container_spec_card"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(".adsky container specification")
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            specRows.forEachIndexed { i, (field, spec) ->
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = connectedShape(i, specRows.size),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                        Text(field, style = MaterialTheme.typography.labelLarge.emphasized(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(spec, style = MaterialTheme.typography.bodyMedium.mono(), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun CryptoStackSection() {
    Column(Modifier.staggeredEntrance(2).testTag("crypto_libraries_card"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Vetted cryptographic stack")
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            stackRows.forEachIndexed { i, (name, role) ->
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = connectedShape(i, stackRows.size),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                        Text(name, style = MaterialTheme.typography.titleSmall.emphasized())
                        Text(role, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

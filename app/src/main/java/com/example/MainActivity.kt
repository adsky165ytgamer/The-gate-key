package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.VaultScreen
import com.example.ui.VaultViewModel
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.BatchScreen
import com.example.ui.screens.DecryptScreen
import com.example.ui.screens.DeviceSecurityScreen
import com.example.ui.screens.EncryptScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RecoveryKeyScreen
import com.example.ui.theme.ADSkyVaultTheme
import com.example.ui.theme.VaultAccentGreen
import com.example.ui.theme.VaultBackground
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultPrimary
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceVariant
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

class MainActivity : ComponentActivity() {
    private val viewModel: VaultViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ADSkyVaultTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()

                BackHandler(enabled = currentScreen != VaultScreen.HOME) {
                    viewModel.navigateTo(VaultScreen.HOME)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = VaultBackground,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Vault Logo",
                                        tint = VaultPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when (currentScreen) {
                                            VaultScreen.HOME -> "AD SKY VAULT"
                                            VaultScreen.ENCRYPT -> "ENCRYPT PAYLOAD"
                                            VaultScreen.DECRYPT -> "DECRYPT CONTAINER"
                                            VaultScreen.BATCH -> "BATCH QUEUE"
                                            VaultScreen.RECOVERY_KEY -> "RECOVERY SECRET"
                                            VaultScreen.DEVICE_SECURITY -> "DEVICE & SECURITY"
                                            VaultScreen.ABOUT -> "ABOUT AD SKY"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        letterSpacing = 1.sp,
                                        color = VaultTextPrimary
                                    )
                                }
                            },
                            navigationIcon = {
                                if (currentScreen != VaultScreen.HOME) {
                                    IconButton(
                                        onClick = { viewModel.navigateTo(VaultScreen.HOME) },
                                        modifier = Modifier.testTag("top_bar_back_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = VaultTextPrimary
                                        )
                                    }
                                }
                            },
                            actions = {
                                Surface(
                                    color = Color(0x3310B981),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, VaultAccentGreen),
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(VaultAccentGreen, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "OFFLINE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = VaultAccentGreen
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = VaultSurface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = VaultSurface,
                            modifier = Modifier.navigationBarsPadding().testTag("bottom_navigation_bar")
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == VaultScreen.HOME,
                                onClick = { viewModel.navigateTo(VaultScreen.HOME) },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Vault") },
                                modifier = Modifier.testTag("nav_item_home"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = VaultPrimary,
                                    selectedTextColor = VaultPrimary,
                                    unselectedIconColor = VaultTextSecondary,
                                    unselectedTextColor = VaultTextSecondary,
                                    indicatorColor = VaultSurfaceVariant
                                )
                            )
                            NavigationBarItem(
                                selected = currentScreen == VaultScreen.ENCRYPT,
                                onClick = { viewModel.navigateTo(VaultScreen.ENCRYPT) },
                                icon = { Icon(Icons.Default.Lock, contentDescription = "Encrypt") },
                                label = { Text("Encrypt") },
                                modifier = Modifier.testTag("nav_item_encrypt"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = VaultPrimary,
                                    selectedTextColor = VaultPrimary,
                                    unselectedIconColor = VaultTextSecondary,
                                    unselectedTextColor = VaultTextSecondary,
                                    indicatorColor = VaultSurfaceVariant
                                )
                            )
                            NavigationBarItem(
                                selected = currentScreen == VaultScreen.DECRYPT,
                                onClick = { viewModel.navigateTo(VaultScreen.DECRYPT) },
                                icon = { Icon(Icons.Default.LockOpen, contentDescription = "Decrypt") },
                                label = { Text("Decrypt") },
                                modifier = Modifier.testTag("nav_item_decrypt"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = VaultAccentGreen,
                                    selectedTextColor = VaultAccentGreen,
                                    unselectedIconColor = VaultTextSecondary,
                                    unselectedTextColor = VaultTextSecondary,
                                    indicatorColor = VaultSurfaceVariant
                                )
                            )
                            NavigationBarItem(
                                selected = currentScreen == VaultScreen.DEVICE_SECURITY,
                                onClick = { viewModel.navigateTo(VaultScreen.DEVICE_SECURITY) },
                                icon = { Icon(Icons.Default.Security, contentDescription = "Security") },
                                label = { Text("Security") },
                                modifier = Modifier.testTag("nav_item_security"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = VaultPrimary,
                                    selectedTextColor = VaultPrimary,
                                    unselectedIconColor = VaultTextSecondary,
                                    unselectedTextColor = VaultTextSecondary,
                                    indicatorColor = VaultSurfaceVariant
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            label = "ScreenTransition"
                        ) { targetScreen ->
                            when (targetScreen) {
                                VaultScreen.HOME -> HomeScreen(viewModel = viewModel)
                                VaultScreen.ENCRYPT -> EncryptScreen(viewModel = viewModel)
                                VaultScreen.DECRYPT -> DecryptScreen(viewModel = viewModel)
                                VaultScreen.BATCH -> BatchScreen(viewModel = viewModel)
                                VaultScreen.RECOVERY_KEY -> RecoveryKeyScreen(viewModel = viewModel)
                                VaultScreen.DEVICE_SECURITY -> DeviceSecurityScreen(viewModel = viewModel)
                                VaultScreen.ABOUT -> AboutScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

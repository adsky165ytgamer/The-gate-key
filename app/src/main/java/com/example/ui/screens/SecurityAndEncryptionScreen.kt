@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PublishedWithChanges
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.SecurityTab
import com.example.ui.VaultScreen
import com.example.ui.VaultViewModel
import com.example.ui.theme.VaultMotion

private data class SecurityTabItem(
    val tab: SecurityTab,
    val icon: ImageVector,
    val tag: String
)

private val tabs = listOf(
    SecurityTabItem(SecurityTab.ENCRYPT, Icons.Rounded.Lock, "tab_encrypt"),
    SecurityTabItem(SecurityTab.DECRYPT, Icons.Rounded.LockOpen, "tab_decrypt"),
    SecurityTabItem(SecurityTab.BATCH, Icons.Rounded.FolderZip, "tab_batch"),
    SecurityTabItem(SecurityTab.RECOVERY, Icons.Rounded.Key, "tab_recovery"),
    SecurityTabItem(SecurityTab.MIGRATION, Icons.Rounded.PublishedWithChanges, "tab_migration"),
    SecurityTabItem(SecurityTab.AUDIT, Icons.Rounded.Security, "tab_audit"),
)

@Composable
fun SecurityAndEncryptionScreen(
    viewModel: VaultViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedSecurityTab.collectAsStateWithLifecycle()
    val layoutDirection = LocalLayoutDirection.current

    BackHandler {
        viewModel.navigateTo(VaultScreen.HOME)
    }

    val selectedIndex = tabs.indexOfFirst { it.tab == selectedTab }.coerceAtLeast(0)

    Column(modifier = modifier.fillMaxSize()) {
        // Expressive Tab Navigation Bar
        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                if (selectedIndex in tabPositions.indices) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier
                            .tabIndicatorOffset(tabPositions[selectedIndex])
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                        height = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            divider = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    end = contentPadding.calculateEndPadding(layoutDirection)
                )
                .testTag("security_tabs_row")
        ) {
            tabs.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                Tab(
                    selected = isSelected,
                    onClick = { viewModel.setSelectedSecurityTab(item.tab) },
                    text = {
                        Text(
                            item.tab.title,
                            style = if (isSelected) {
                                MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            } else {
                                MaterialTheme.typography.labelLarge
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(item.tag)
                )
            }
        }

        // Active Tab Screen Content
        val tabContentPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(layoutDirection),
            top = 12.dp,
            end = contentPadding.calculateEndPadding(layoutDirection),
            bottom = contentPadding.calculateBottomPadding()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(VaultMotion.fastEffectsSpec()) togetherWith fadeOut(VaultMotion.fastEffectsSpec())
                },
                label = "security-tab-content"
            ) { activeTab ->
                when (activeTab) {
                    SecurityTab.ENCRYPT -> EncryptScreen(viewModel, tabContentPadding)
                    SecurityTab.DECRYPT -> DecryptScreen(viewModel, tabContentPadding)
                    SecurityTab.BATCH -> BatchScreen(viewModel, tabContentPadding)
                    SecurityTab.RECOVERY -> RecoveryKeyScreen(viewModel, tabContentPadding)
                    SecurityTab.MIGRATION -> MigrationScreen(viewModel, tabContentPadding)
                    SecurityTab.AUDIT -> DeviceSecurityScreen(viewModel, tabContentPadding)
                }
            }
        }
    }
}

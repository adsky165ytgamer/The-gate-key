@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.registry.ToolCategory
import com.example.ui.registry.ToolId
import com.example.ui.registry.ToolRequirement
import com.example.ui.registry.ToolTone
import com.example.ui.theme.VaultBadgeShape
import com.example.ui.theme.VaultMotion
import com.example.ui.theme.emphasized

// ───────────────────────────── tone helpers ─────────────────────────────

/** Solid accent + its on-color for a tone, matching the Home cards. */
@Composable
fun ToolTone.accent(): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (this) {
        ToolTone.PRIMARY -> c.primary to c.onPrimary
        ToolTone.SECONDARY -> c.secondary to c.onSecondary
        ToolTone.TERTIARY -> c.tertiary to c.onTertiary
    }
}

/** Small "beta" marker, same idea as the beta tag in Essentials. */
@Composable
fun BetaTag(modifier: Modifier = Modifier, label: String? = null) {
    Row(
        modifier
            .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = if (label != null) 6.dp else 3.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            Icons.Rounded.Science,
            contentDescription = if (label == null) "Beta" else null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(16.dp)
        )
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

// ───────────────────────────── segmented menu ─────────────────────────────

/** Dropdown whose items are separate rounded chips (the Essentials "segmented" menu look). */
@Composable
fun SegmentedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            Modifier.clip(RoundedCornerShape(16.dp)),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content
        )
    }
}

@Composable
fun SegmentedDropdownMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    DropdownMenuItem(
        text = { Text(text) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        },
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    )
}

/** Shared by the tiles of one grid: while one tile's menu is open, the others dim and blur. */
@Stable
class MenuFocus {
    var active by mutableStateOf<ToolId?>(null)
}

// ───────────────────────────── tool tile ─────────────────────────────

/**
 * One box in a hub grid. Tap opens the tool; long-press opens a menu (Open / What is this?).
 * Press squishes the box and tightens its corners with the expressive spatial spring.
 */
@Composable
fun ToolTile(
    tool: ToolId,
    focus: MenuFocus,
    onOpen: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var menuOpen by remember { mutableStateOf(false) }

    DisposableEffect(menuOpen) {
        if (menuOpen) focus.active = tool
        onDispose { if (focus.active == tool) focus.active = null }
    }

    val dimmed = focus.active != null && focus.active != tool
    val corner by animateDpAsState(if (pressed) 16.dp else 28.dp, VaultMotion.fastSpatialSpec(), label = "tile-corner")
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, VaultMotion.fastSpatialSpec(), label = "tile-scale")
    val dimAlpha by animateFloatAsState(if (dimmed) 0.5f else 1f, tween(300), label = "tile-dim")
    val blurRadius by animateDpAsState(if (dimmed) 8.dp else 0.dp, tween(300), label = "tile-blur")

    val (accent, onAccent) = tool.category.tone.accent()
    val shape = RoundedCornerShape(corner.coerceAtLeast(0.dp))

    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = dimAlpha
            }
            .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .combinedClickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Button,
                onClickLabel = "Open ${tool.title}",
                onLongClickLabel = "More about ${tool.title}",
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuOpen = true
                },
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onOpen()
                }
            )
            .testTag("tool_tile_${tool.name.lowercase()}")
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .heightIn(min = 160.dp)
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                ShapeBadge(icon = tool.icon, polygon = tool.badge, size = 52.dp, containerColor = accent, contentColor = onAccent)
                if (tool.category.isBeta) BetaTag()
            }
            Column(Modifier.padding(top = 14.dp)) {
                Text(
                    tool.title,
                    style = MaterialTheme.typography.titleMedium.emphasized(),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    tool.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        SegmentedDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            SegmentedDropdownMenuItem("Open", Icons.AutoMirrored.Rounded.ArrowForward, onClick = {
                menuOpen = false
                onOpen()
            })
            SegmentedDropdownMenuItem("What is this?", Icons.Rounded.Info, onClick = {
                menuOpen = false
                onHelp()
            })
        }
    }
}

// ───────────────────────────── grid ─────────────────────────────

/**
 * The inside of a type: a short description, a few facts, then the tools as a 2-column grid.
 * Falls back to a single column at very large font sizes so text is never squeezed.
 */
@Composable
fun ToolGrid(
    category: ToolCategory,
    contentPadding: PaddingValues,
    staggered: Boolean,
    onOpen: (ToolId) -> Unit,
    onHelp: (ToolId) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = if (LocalDensity.current.fontScale > 1.4f) 1 else 2
    val focus = remember { MenuFocus() }
    val tools = category.tools

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            category.about,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        FlowRow(
            Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoChip("${tools.size} tools")
            category.requirements.forEach { InfoChip(it.label, leading = it.icon) }
            if (category.isBeta) InfoChip("Beta", leading = Icons.Rounded.Science)
        }

        tools.chunked(columns).forEachIndexed { rowIndex, row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEachIndexed { i, tool ->
                    val entrance = if (staggered) Modifier.staggeredEntrance(rowIndex * columns + i) else Modifier
                    ToolTile(
                        tool = tool,
                        focus = focus,
                        onOpen = { onOpen(tool) },
                        onHelp = { onHelp(tool) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(entrance)
                    )
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ───────────────────────────── help sheets ─────────────────────────────

@Composable
private fun HelpSheet(
    title: String,
    about: String,
    icon: ImageVector,
    badge: VaultBadgeShape,
    accent: Pair<Color, Color>,
    chips: List<ToolRequirement>,
    beta: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ShapeBadge(icon = icon, polygon = badge, size = 56.dp, containerColor = accent.first, contentColor = accent.second)
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.emphasized(),
                    modifier = Modifier.weight(1f)
                )
                if (beta) BetaTag(label = "Beta")
            }
            if (chips.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chips.forEach { InfoChip(it.label, leading = it.icon) }
                }
            }
            Text(about, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            content()
        }
    }
}

/** "What is this?" for a single tool. [onOpen] adds an Open button (used from the grid, not from inside the tool). */
@Composable
fun ToolHelpSheet(tool: ToolId, onDismiss: () -> Unit, onOpen: ((ToolId) -> Unit)? = null) {
    HelpSheet(
        title = tool.title,
        about = tool.about,
        icon = tool.icon,
        badge = tool.badge,
        accent = tool.category.tone.accent(),
        chips = tool.requirements,
        beta = tool.category.isBeta,
        onDismiss = onDismiss
    ) {
        if (onOpen != null) {
            VaultButton(
                text = "Open ${tool.title}",
                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                onClick = {
                    onDismiss()
                    onOpen(tool)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** "What is this?" for a whole type, with its tools listed as shortcuts. */
@Composable
fun CategoryHelpSheet(category: ToolCategory, onDismiss: () -> Unit, onOpenTool: (ToolId) -> Unit) {
    HelpSheet(
        title = category.title,
        about = category.about,
        icon = category.icon,
        badge = category.badge,
        accent = category.tone.accent(),
        chips = category.requirements,
        beta = category.isBeta,
        onDismiss = onDismiss
    ) {
        val (accent, onAccent) = category.tone.accent()
        Column(Modifier.clip(RoundedCornerShape(24.dp)), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            category.tools.forEach { tool ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .combinedClickable(onClick = {
                            onDismiss()
                            onOpenTool(tool)
                        })
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ShapeBadge(icon = tool.icon, polygon = tool.badge, size = 40.dp, containerColor = accent, contentColor = onAccent)
                    Column(Modifier.weight(1f)) {
                        Text(tool.title, style = MaterialTheme.typography.titleSmall.emphasized())
                        Text(
                            tool.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

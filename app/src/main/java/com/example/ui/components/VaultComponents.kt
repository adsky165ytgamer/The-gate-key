@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)

package com.example.ui.components

import android.provider.Settings
import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.crypto.CipherAlgorithm
import com.example.crypto.HardwareSecurityInfo
import com.example.ui.theme.VaultBadgeShape
import com.example.ui.theme.VaultMotion
import com.example.ui.theme.VaultTheme
import com.example.ui.theme.emphasized
import com.example.ui.theme.mono
import kotlinx.coroutines.delay

// ───────────────────────────── helpers ─────────────────────────────

/** False when the user turned system animations off ("Remove animations"). */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
}

/** Corner shape for an item inside a connected list: big outer corners, tight inner corners. */
fun connectedShape(index: Int, count: Int, outer: Dp = 28.dp, inner: Dp = 6.dp): Shape {
    val top = if (index == 0) outer else inner
    val bottom = if (index == count - 1) outer else inner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/** Fade + rise entrance, staggered by [index]. Plays once, then stays put when scrolling back. */
@Composable
fun Modifier.staggeredEntrance(index: Int): Modifier {
    if (!rememberAnimationsEnabled()) return this
    var played by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (played) 1f else 0f) }
    val spec = VaultMotion.defaultSpatialSpec<Float>()
    LaunchedEffect(Unit) {
        if (!played) {
            delay(index.coerceIn(0, 8) * 45L)
            progress.animateTo(1f, spec)
            played = true
        }
    }
    return this.graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * 28.dp.toPx()
    }
}

operator fun PaddingValues.plus(other: PaddingValues): PaddingValues = object : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection) =
        this@plus.calculateLeftPadding(layoutDirection) + other.calculateLeftPadding(layoutDirection)
    override fun calculateTopPadding() = this@plus.calculateTopPadding() + other.calculateTopPadding()
    override fun calculateRightPadding(layoutDirection: LayoutDirection) =
        this@plus.calculateRightPadding(layoutDirection) + other.calculateRightPadding(layoutDirection)
    override fun calculateBottomPadding() = this@plus.calculateBottomPadding() + other.calculateBottomPadding()
}

// ───────────────────────────── layout ─────────────────────────────

/**
 * Screen body with a primary action docked at the bottom (edge-to-edge aware).
 * The list gets bottom padding equal to the measured height of the action bar,
 * so nothing is ever hidden behind it - whatever the font size.
 */
@Composable
fun ScreenWithActionBar(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    actionBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    var barHeight by remember { mutableStateOf(0.dp) }
    val background = MaterialTheme.colorScheme.background

    Box(modifier.fillMaxSize()) {
        content(
            PaddingValues(
                start = contentPadding.calculateStartPadding(layoutDirection),
                top = contentPadding.calculateTopPadding(),
                end = contentPadding.calculateEndPadding(layoutDirection),
                bottom = maxOf(barHeight, contentPadding.calculateBottomPadding())
            )
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { barHeight = with(density) { it.height.toDp() } }
                .background(Brush.verticalGradient(listOf(Color.Transparent, background.copy(alpha = 0.94f), background)))
                .padding(
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    end = contentPadding.calculateEndPadding(layoutDirection),
                    top = 16.dp,
                    bottom = contentPadding.calculateBottomPadding()
                )
        ) { actionBar() }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    step: Int? = null,
    subtitle: String? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.Top
    ) {
        if (step != null) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                Box(Modifier.sizeIn(minWidth = 28.dp, minHeight = 28.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "$step",
                        style = MaterialTheme.typography.labelMedium.emphasized(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.emphasized(),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ───────────────────────────── chips & badges ─────────────────────────────

@Composable
fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                Icon(leading, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, style = MaterialTheme.typography.labelMedium.emphasized())
        }
    }
}

@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    valueStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium
) {
    val style = if (monospace) valueStyle.mono() else valueStyle
    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = style,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

// ───────────────────────────── shape badge ─────────────────────────────

/** Icon inside an expressive shape (cookie, sunny, clover, etc.). Optionally rotates. */
@Composable
fun ShapeBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    polygon: VaultBadgeShape = VaultBadgeShape.Cookie9Sided,
    size: Dp = 56.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    rotate: Boolean = false,
    contentDescription: String? = null
) {
    val shape = remember(polygon) { polygon.toShape() }
    val animate = rotate && rememberAnimationsEnabled()
    val angle = if (animate) {
        rememberInfiniteTransition(label = "badge-rotation").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(36_000, easing = LinearEasing), RepeatMode.Restart),
            label = "badge-angle"
        )
    } else null

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = angle?.value ?: 0f }
                .background(containerColor, shape)
        )
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(size * 0.46f)
        )
    }
}

// ───────────────────────────── buttons ─────────────────────────────

private val ButtonMinHeight = 56.dp
private val ButtonPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)

@Composable
private fun RowScope.ButtonLabel(text: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
    }
    Text(
        text,
        style = MaterialTheme.typography.titleMedium.emphasized(),
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(1f, fill = false)
    )
}

@Composable
fun VaultButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors()
) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.ContextClick); onClick() },
        modifier = modifier.heightIn(min = ButtonMinHeight),
        enabled = enabled,
        colors = colors,
        shape = RoundedCornerShape(20.dp),
        contentPadding = ButtonPadding
    ) { ButtonLabel(text, icon) }
}

@Composable
fun VaultTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors()
) {
    val haptic = LocalHapticFeedback.current
    FilledTonalButton(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.ContextClick); onClick() },
        modifier = modifier.heightIn(min = ButtonMinHeight),
        enabled = enabled,
        colors = colors,
        shape = RoundedCornerShape(20.dp),
        contentPadding = ButtonPadding
    ) { ButtonLabel(text, icon) }
}

@Composable
fun VaultOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = ButtonMinHeight),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
        shape = RoundedCornerShape(20.dp),
        contentPadding = ButtonPadding
    ) { ButtonLabel(text, icon) }
}

// ───────────────────────────── tiles & chips ─────────────────────────────

/** Home tile. Interactive press state with smooth spring animation. */
@Composable
fun ExpressiveTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    badgeColor: Color,
    onBadgeColor: Color,
    badgePolygon: VaultBadgeShape,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (pressed) 16.dp else 28.dp,
        animationSpec = VaultMotion.fastSpatialSpec(),
        label = "tile-corner"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = VaultMotion.fastSpatialSpec(),
        label = "tile-scale"
    )
    val haptic = LocalHapticFeedback.current

    Surface(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.ContextClick); onClick() },
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .testTag(testTag),
        shape = RoundedCornerShape(corner.coerceAtLeast(0.dp)),
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interaction
    ) {
        Column(
            Modifier
                .padding(20.dp)
                .heightIn(min = 148.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                ShapeBadge(
                    icon = icon,
                    polygon = badgePolygon,
                    size = 52.dp,
                    containerColor = badgeColor,
                    contentColor = onBadgeColor
                )
            }
            Column(Modifier.padding(top = 12.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.emphasized(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun HardwareSecurityPill(
    info: HardwareSecurityInfo,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val ext = VaultTheme.colors
    val (label, container, content) = when {
        info.isInsideSecureHardware && info.securityLevelName.contains("StrongBox", ignoreCase = true) ->
            Triple("StrongBox Keymaster", ext.successContainer, ext.onSuccessContainer)
        info.isInsideSecureHardware ->
            Triple("TEE Hardware-backed", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        else ->
            Triple("Software Keystore", ext.warningContainer, ext.onWarningContainer)
    }
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(12.dp),
        color = container,
        contentColor = content,
        modifier = modifier.testTag("hardware_security_pill")
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium.emphasized())
        }
    }
}

// ───────────────────────────── options ─────────────────────────────

@Composable
fun AlgorithmSelectionCard(
    algorithm: CipherAlgorithm,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp)
) {
    val container by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        VaultMotion.defaultEffectsSpec(), label = "algo-container"
    )
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.ToggleOn); onSelect()
            })
            .testTag("algo_card_${algorithm.name}"),
        shape = shape,
        color = container,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isSelected, onClick = null)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        algorithm.displayName,
                        style = MaterialTheme.typography.titleMedium.emphasized(),
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Text(
                            "0x0${algorithm.id}",
                            style = MaterialTheme.typography.labelSmall.mono(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    algorithm.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun RadioOptionRow(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    shape: Shape = RoundedCornerShape(28.dp),
    testTag: String? = null
) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        VaultMotion.defaultEffectsSpec(), label = "radio-container"
    )
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.ToggleOn); onClick()
            })
            .let { if (testTag != null) it.testTag(testTag) else it },
        shape = shape,
        color = container,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall.emphasized())
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
fun ToggleOptionRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    switchTag: String? = null
) {
    val container by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        VaultMotion.defaultEffectsSpec(), label = "toggle-container"
    )
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .toggleable(value = checked, role = Role.Switch, onValueChange = {
                haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                onCheckedChange(it)
            }),
        shape = shape,
        color = container,
        contentColor = if (checked) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall.emphasized())
                Text(description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(Modifier.width(16.dp))
            Switch(
                checked = checked,
                onCheckedChange = null,
                modifier = if (switchTag != null) Modifier.testTag(switchTag) else Modifier,
                thumbContent = if (checked) {
                    { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                } else null
            )
        }
    }
}

// ───────────────────────────── inputs ─────────────────────────────

@Composable
fun SecretTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    obscured: Boolean = true
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        visualTransformation = if (obscured && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (obscured) KeyboardType.Password else KeyboardType.Ascii,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done
        ),
        trailingIcon = if (obscured) {
            {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (visible) "Hide" else "Show"
                    )
                }
            }
        } else null
    )
}

// ───────────────────────────── feedback ─────────────────────────────

enum class BannerKind { Info, Success, Warning, Error }

@Composable
fun StatusBanner(
    kind: BannerKind,
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: (@Composable () -> Unit)? = null
) {
    val ext = VaultTheme.colors
    val scheme = MaterialTheme.colorScheme
    val (container, onContainer, icon) = when (kind) {
        BannerKind.Info -> Triple(scheme.secondaryContainer, scheme.onSecondaryContainer, Icons.Rounded.Info)
        BannerKind.Success -> Triple(ext.successContainer, ext.onSuccessContainer, Icons.Rounded.CheckCircle)
        BannerKind.Warning -> Triple(ext.warningContainer, ext.onWarningContainer, Icons.Rounded.Warning)
        BannerKind.Error -> Triple(scheme.errorContainer, scheme.onErrorContainer, Icons.Rounded.Error)
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = if (kind == BannerKind.Error) LiveRegionMode.Assertive else LiveRegionMode.Polite },
        shape = RoundedCornerShape(28.dp),
        color = container,
        contentColor = onContainer
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(icon, null, Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    if (title != null) Text(title, style = MaterialTheme.typography.titleMedium.emphasized())
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                }
            }
            content?.let { Spacer(Modifier.size(14.dp)); it() }
        }
    }
}

@Composable
fun WeakPinWarningBox(
    input: String,
    modifier: Modifier = Modifier
) {
    val isFourDigits = input.length == 4 && input.all { it.isDigit() }
    AnimatedVisibility(
        visible = isFourDigits,
        modifier = modifier,
        enter = expandVertically(VaultMotion.defaultSpatialSpec()) + fadeIn(VaultMotion.defaultEffectsSpec()),
        exit = shrinkVertically(VaultMotion.fastSpatialSpec()) + fadeOut(VaultMotion.fastEffectsSpec())
    ) {
        StatusBanner(
            kind = BannerKind.Warning,
            message = "A 4-digit PIN can be brute-forced if someone obtains the container. A passphrase of 8+ characters is strongly recommended.",
            modifier = Modifier.testTag("weak_pin_warning")
        )
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
    modifier: Modifier = Modifier,
    headline: String = "Processing cryptographic stream",
    cancelLabel: String = "Cancel operation"
) {
    val context = LocalContext.current
    val animatedProgress by animateFloatAsState(progress.coerceIn(0f, 1f), VaultMotion.defaultEffectsSpec(), label = "progress")

    AnimatedVisibility(
        visible = isProcessing,
        modifier = modifier,
        enter = expandVertically(VaultMotion.defaultSpatialSpec()) + fadeIn(VaultMotion.defaultEffectsSpec()),
        exit = shrinkVertically(VaultMotion.fastSpatialSpec()) + fadeOut(VaultMotion.fastEffectsSpec())
    ) {
        Surface(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("operation_progress_card"),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shadowElevation = 6.dp
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LoadingIndicator(Modifier.size(40.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(headline, style = MaterialTheme.typography.titleMedium.emphasized())
                        if (currentChunk > 0) {
                            Text("Chunk #$currentChunk", style = MaterialTheme.typography.labelMedium.mono())
                        }
                    }
                    Text("${(animatedProgress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge.emphasized())
                }
                LinearWavyProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp)
                )
                Text(
                    statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
                if (totalBytes > 0) {
                    val processedFormatted = Formatter.formatFileSize(context, bytesProcessed)
                    val totalFormatted = Formatter.formatFileSize(context, totalBytes)
                    Text(
                        "$processedFormatted / $totalFormatted",
                        style = MaterialTheme.typography.bodySmall.mono(),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                VaultOutlinedButton(
                    text = cancelLabel,
                    onClick = onCancel,
                    contentColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cancel_operation_button")
                )
            }
        }
    }
}

/** Standard circular progress indicator */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    CircularProgressIndicator(
        modifier = modifier,
        color = color,
        strokeWidth = 3.5.dp
    )
}

/** Modern rounded linear progress indicator */
@Composable
fun LinearWavyProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    LinearProgressIndicator(
        progress = { progress() },
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = color,
        trackColor = trackColor
    )
}

/** Final success / result card that carries one primary action (save). */
@Composable
fun ResultCard(
    title: String,
    modifier: Modifier = Modifier,
    lines: List<String> = emptyList(),
    badge: String? = null,
    actionText: String,
    actionIcon: ImageVector,
    onAction: () -> Unit,
    cardTag: String,
    actionTag: String
) {
    val ext = VaultTheme.colors
    Surface(
        modifier
            .fillMaxWidth()
            .testTag(cardTag),
        shape = RoundedCornerShape(32.dp),
        color = ext.successContainer,
        contentColor = ext.onSuccessContainer
    ) {
        Column(
            Modifier
                .padding(20.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShapeBadge(
                    Icons.Rounded.Check,
                    polygon = VaultBadgeShape.Cookie9Sided,
                    size = 48.dp,
                    containerColor = ext.success,
                    contentColor = ext.onSuccess
                )
                Spacer(Modifier.width(14.dp))
                Text(title, style = MaterialTheme.typography.titleLarge.emphasized(), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.size(10.dp))
            lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (badge != null) {
                Spacer(Modifier.size(10.dp))
                InfoChip(badge, containerColor = ext.success, contentColor = ext.onSuccess)
            }
            Spacer(Modifier.size(16.dp))
            VaultButton(
                text = actionText,
                icon = actionIcon,
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = ext.success, contentColor = ext.onSuccess),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(actionTag)
            )
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShapeBadge(
                icon = icon,
                polygon = VaultBadgeShape.Sunny,
                size = 64.dp,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(14.dp))
            Text(title, style = MaterialTheme.typography.titleMedium.emphasized(), textAlign = TextAlign.Center)
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

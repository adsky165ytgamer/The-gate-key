@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.camera.imageinfo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.camera.core.BitmapLoader
import com.example.ui.components.BannerKind
import com.example.ui.components.EmptyState
import com.example.ui.components.InfoChip
import com.example.ui.components.KeyValueRow
import com.example.ui.components.LoadingIndicator
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBanner
import com.example.ui.components.VaultButton
import com.example.ui.components.VaultTonalButton
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.emphasized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun ImageInfoScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: ImageInfoViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.load(uri)
    }
    val pick = { picker.launch(arrayOf("image/*")) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (val s = state) {
            ImageInfoState.Idle -> item(key = "idle") {
                Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    EmptyState(Icons.Rounded.Image, "Select an image", "Dimensions, file details, camera data and colours appear here.")
                    VaultButton("Choose image", pick, Modifier.fillMaxWidth(), Icons.Rounded.PhotoLibrary)
                }
            }
            ImageInfoState.Loading -> item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { LoadingIndicator(Modifier.size(56.dp)) }
            }
            is ImageInfoState.Failed -> {
                item(key = "failed") { StatusBanner(BannerKind.Error, s.message) }
                item(key = "retry") { VaultButton("Choose another image", pick, Modifier.fillMaxWidth(), Icons.Rounded.PhotoLibrary) }
            }
            is ImageInfoState.Loaded -> loadedContent(s.info, pick)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.loadedContent(info: ImageInfo, pick: () -> Unit) {
    item(key = "preview") { Preview(info, Modifier.staggeredEntrance(0)) }

    item(key = "summary") {
        FlowRow(Modifier.fillMaxWidth().staggeredEntrance(1), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            info.summary.forEach { InfoChip("${it.value}") }
        }
    }

    info.sections.forEachIndexed { i, section ->
        item(key = "section-${section.title}") {
            Column(Modifier.staggeredEntrance(i + 2), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(section.title)
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        section.items.forEach { KeyValueRow(it.label, it.value) }
                    }
                }
            }
        }
    }

    info.analysis?.let { analysis ->
        item(key = "colours") { ColourSection(analysis) }
    }

    if (info.technical.isNotEmpty()) {
        item(key = "technical") { TechnicalSection(info.technical) }
    }

    item(key = "actions") { Actions(info, pick) }
}

@Composable
private fun Preview(info: ImageInfo, modifier: Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, info.uri) {
        value = withContext(Dispatchers.IO) { BitmapLoader.decode(context, info.uri, 1400) }
    }
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Box(Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 420.dp), contentAlignment = Alignment.Center) {
            bitmap?.let {
                Image(
                    it.asImageBitmap(), contentDescription = "Preview of ${info.displayName}",
                    contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
                )
            } ?: LoadingIndicator(Modifier.size(40.dp))
        }
    }
}

@Composable
private fun ColourSection(analysis: ImageAnalysisResult) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var copied by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Colour")
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (analysis.palette.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp))) {
                        analysis.palette.forEach { c ->
                            Box(Modifier.weight(c.share.coerceAtLeast(0.08f)).fillMaxSize().background(Color(c.argb)))
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        analysis.palette.forEach { c ->
                            Surface(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Colour", c.hex))
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    copied = c.hex
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Copy colour ${c.hex}" }
                            ) {
                                Row(Modifier.padding(start = 8.dp, end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(28.dp).clip(CircleShape).background(Color(c.argb)))
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (copied == c.hex) "Copied" else c.hex, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
                Column {
                    KeyValueRow("Average brightness", "${(analysis.averageBrightness * 100).toInt()}%")
                    KeyValueRow("Average saturation", "${(analysis.averageSaturation * 100).toInt()}%")
                    KeyValueRow("Transparency", if (analysis.hasTransparency) "Yes" else "No")
                }
                Text("Brightness distribution", style = MaterialTheme.typography.labelLarge)
                Histogram(analysis.histogram)
            }
        }
    }
}

@Composable
private fun Histogram(bins: List<Float>) {
    val bar = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(
        Modifier.fillMaxWidth().height(72.dp).semantics { contentDescription = "Brightness histogram from dark to light" }
    ) {
        val n = bins.size.coerceAtLeast(1)
        val gap = 3.dp.toPx()
        val w = (size.width - gap * (n - 1)) / n
        bins.forEachIndexed { i, v ->
            val x = i * (w + gap)
            drawRoundRect(track, Offset(x, 0f), Size(w, size.height), CornerRadius(w / 3f))
            val h = (size.height * v).coerceAtLeast(2.dp.toPx())
            drawRoundRect(bar, Offset(x, size.height - h), Size(w, h), CornerRadius(w / 3f))
        }
    }
}

@Composable
private fun TechnicalSection(items: List<Property>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Technical details", style = MaterialTheme.typography.titleMedium.emphasized(), modifier = Modifier.weight(1f))
                    Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = if (expanded) "Collapse" else "Expand")
                }
                AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Column(Modifier.padding(bottom = 8.dp)) { items.forEach { KeyValueRow(it.label, it.value) } }
                }
            }
        }
    }
}

@Composable
private fun Actions(info: ImageInfo, pick: () -> Unit) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VaultTonalButton("Copy", {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Image information", info.asText()))
            }, Modifier.weight(1f), Icons.Rounded.ContentCopy)
            VaultTonalButton("Share", {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, info.asText())
                context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }, Modifier.weight(1f), Icons.Rounded.Share)
        }
        VaultButton("Choose another image", pick, Modifier.fillMaxWidth(), Icons.Rounded.PhotoLibrary)
    }
}

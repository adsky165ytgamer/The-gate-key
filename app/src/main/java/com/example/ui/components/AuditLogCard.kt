package com.example.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AuditLogEntry
import com.example.ui.theme.VaultTheme
import com.example.ui.theme.emphasized

/** One row of the security audit log. Pass a [connectedShape] to stack rows into a connected group. */
@Composable
fun AuditLogCard(
    log: AuditLogEntry,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large
) {
    val ext = VaultTheme.colors
    val isSuccess = log.status == "SUCCESS"
    val badgeContainer = if (isSuccess) ext.successContainer else ext.warningContainer
    val badgeContent = if (isSuccess) ext.onSuccessContainer else ext.onWarningContainer
    val relativeTime = remember(log.timestamp) {
        DateUtils.getRelativeTimeSpanString(log.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = CircleShape, color = badgeContainer, contentColor = badgeContent) {
                Icon(
                    if (isSuccess) Icons.Rounded.Check else Icons.Rounded.Close,
                    contentDescription = log.status,
                    modifier = Modifier.padding(6.dp).size(18.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${log.operation} · ${log.filename}",
                    style = MaterialTheme.typography.titleSmall.emphasized(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${log.algorithm} · ${log.envelopeUsed}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (log.details.isNotBlank()) {
                    Text(
                        log.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Text(
                    relativeTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeContent,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

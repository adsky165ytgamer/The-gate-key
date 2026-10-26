package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuditLogEntry
import com.example.ui.theme.VaultAccentAmber
import com.example.ui.theme.VaultAccentGreen
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultCard
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

@Composable
fun AuditLogCard(
    log: AuditLogEntry,
    modifier: Modifier = Modifier
) {
    val isSuccess = log.status == "SUCCESS"
    val statusColor = if (isSuccess) VaultAccentGreen else VaultAccentAmber

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = VaultCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(statusColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${log.operation}: ${log.filename}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VaultTextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = log.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${log.algorithm} | ${log.envelopeUsed} - ${log.details}",
                    fontSize = 11.sp,
                    color = VaultTextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

package com.share.with.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.share.with.AppState
import com.share.with.ServerManager
import com.share.with.PendingConnection
import com.share.with.ActiveSession
import com.share.with.R

@Composable
fun ClientConnectionRow(
    title: String,
    subtitle: String?,
    backgroundColor: Color,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    actions: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = titleColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

@Composable
fun PendingApprovalRow(pending: PendingConnection) {
    ClientConnectionRow(
        title = pending.ipAddress,
        subtitle = pending.userAgent,
        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF10B981))
                .clickable {
                    AppState.approvePending(pending.token)
                    AppState.addLog("Host APPROVED access to client IP: ${pending.ipAddress}")
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.approve_button),
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFEF4444))
                .clickable {
                    ServerManager.rejectPending(pending.token)
                    AppState.addLog("Host REJECTED access to client IP: ${pending.ipAddress}")
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.reject_button),
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun ActiveSessionRow(active: ActiveSession) {
    ClientConnectionRow(
        title = active.ipAddress,
        subtitle = active.userAgent,
        backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
    ) {
        IconButton(
            onClick = {
                AppState.removeActive(active.ipAddress)
                AppState.addLog("Disconnecting active client IP: ${active.ipAddress}")
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Disconnect", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun BlockedIpRow(ip: String) {
    ClientConnectionRow(
        title = ip,
        subtitle = null,
        backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
        titleColor = MaterialTheme.colorScheme.error
    ) {
        TextButton(
            onClick = {
                AppState.unblockIp(ip)
                AppState.addLog("Host UNBLOCKED IP: $ip")
            },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(stringResource(R.string.unblock_button))
        }
    }
}

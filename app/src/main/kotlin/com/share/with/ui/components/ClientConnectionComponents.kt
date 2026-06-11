package com.share.with.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.share.with.ActiveSession
import com.share.with.AppState
import com.share.with.PendingConnection
import com.share.with.R
import com.share.with.ServerManager

@Composable
fun ClientConnectionRow(
    title: String,
    subtitle: String?,
    backgroundColor: Color,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    actions: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

@Composable
fun PendingApprovalRow(pending: PendingConnection) {
    ClientConnectionRow(
        title = pending.ipAddress,
        subtitle = pending.userAgent,
        backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
    ) {
        IconButton(
            onClick = {
                AppState.approvePending(pending.token)
                AppState.addLog("Host APPROVED access to client IP: ${pending.ipAddress}")
            },
            modifier = Modifier.size(40.dp),
            colors =
                IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White,
                ),
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.approve_button),
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(
            onClick = {
                ServerManager.rejectPending(pending.token)
                AppState.addLog("Host REJECTED access to client IP: ${pending.ipAddress}")
            },
            modifier = Modifier.size(40.dp),
            colors =
                IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.reject_button),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun ActiveSessionRow(active: ActiveSession) {
    ClientConnectionRow(
        title = active.ipAddress,
        subtitle = active.userAgent,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        TextButton(
            onClick = {
                AppState.removeActive(active.ipAddress)
                AppState.addLog("Disconnecting active client IP: ${active.ipAddress}")
            },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text(stringResource(R.string.notif_action_stop))
        }
    }
}

@Composable
fun BlockedIpRow(ip: String) {
    ClientConnectionRow(
        title = ip,
        subtitle = null,
        backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
        titleColor = MaterialTheme.colorScheme.error,
    ) {
        FilledTonalButton(
            onClick = {
                AppState.unblockIp(ip)
                AppState.addLog("Host UNBLOCKED IP: $ip")
            },
            modifier = Modifier.height(36.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
        ) {
            Text(
                stringResource(R.string.unblock_button),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

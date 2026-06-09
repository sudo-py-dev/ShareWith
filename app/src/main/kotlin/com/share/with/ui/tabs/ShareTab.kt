package com.share.with.ui.tabs

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.share.with.AppState
import com.share.with.FileSharingService
import com.share.with.R
import com.share.with.SecurityMode
import com.share.with.ui.components.ActiveSessionRow
import com.share.with.ui.components.BlockedIpRow
import com.share.with.ui.components.PendingApprovalRow
import com.share.with.ui.components.SharedFileRow
import com.share.with.ui.utils.generateQrCodeBitmap

@Composable
fun ShareTab(
    onAddFile: () -> Unit,
    onAddFolder: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val serverUrl = if (AppState.localIp != null) "http://${AppState.localIp}:${AppState.serverPort}" else null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.server_status),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (AppState.serverRunning) Color(0xFF10B981) else Color(0xFFEF4444),
                                            ),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text =
                                        if (AppState.serverRunning) {
                                            stringResource(
                                                R.string.status_running,
                                            )
                                        } else {
                                            stringResource(R.string.status_stopped)
                                        },
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (AppState.serverRunning) {
                                    FileSharingService.stopService(context)
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val hasPermission =
                                            androidx.core.content.ContextCompat.checkSelfPermission(
                                                context,
                                                android.Manifest.permission.POST_NOTIFICATIONS,
                                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                        if (!hasPermission) {
                                            onRequestNotificationPermission()
                                        } else {
                                            AppState.detectLocalIp()
                                            FileSharingService.startService(context)
                                        }
                                    } else {
                                        AppState.detectLocalIp()
                                        FileSharingService.startService(context)
                                    }
                                }
                            },
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = if (AppState.serverRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                ),
                        ) {
                            Text(
                                if (AppState.serverRunning) stringResource(R.string.stop_server) else stringResource(R.string.start_server),
                            )
                        }
                    }

                    if (AppState.serverRunning && serverUrl != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(R.string.server_address),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = serverUrl,
                                style =
                                    MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(serverUrl))
                                        AppState.addLog(context.getString(R.string.log_url_copied))
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = stringResource(R.string.copy_link_button),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val sendIntent: Intent =
                                            Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, serverUrl)
                                                type = "text/plain"
                                            }
                                        val shareIntent = Intent.createChooser(sendIntent, null)
                                        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(shareIntent)
                                        AppState.addLog(context.getString(R.string.log_url_shared))
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = stringResource(R.string.share_link_button),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // QR Code Card
        if (AppState.serverRunning && serverUrl != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.scan_qr_code),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val qrBitmap = remember(serverUrl) { generateQrCodeBitmap(serverUrl) }
                        if (qrBitmap != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                color = Color.White,
                                modifier =
                                    Modifier
                                        .size(180.dp)
                                        .padding(8.dp),
                            ) {
                                androidx.compose.foundation.Image(
                                    bitmap = qrBitmap,
                                    contentDescription = "QR Code",
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Shared File List Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.shared_files_header),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = onAddFile) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = stringResource(R.string.add_file_button),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            IconButton(onClick = onAddFolder) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = stringResource(R.string.add_folder_button),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (AppState.sharedItems.isEmpty()) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.empty_shared_list),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            for (item in AppState.sharedItems) {
                                SharedFileRow(
                                    item = item,
                                    onRemove = {
                                        AppState.removeSharedItem(item)
                                        AppState.addLog(context.getString(R.string.log_path_removed, item.name))
                                    },
                                    onShare = {
                                        if (serverUrl != null) {
                                            val itemUrl = if (item.isDirectory) "$serverUrl/?id=${item.id}" else "$serverUrl/download?id=${item.id}"
                                            val sendIntent: Intent =
                                                Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, itemUrl)
                                                    type = "text/plain"
                                                }
                                            val shareIntent = Intent.createChooser(sendIntent, null)
                                            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(shareIntent)
                                            AppState.addLog(context.getString(R.string.log_item_shared, item.name))
                                        } else {
                                            AppState.addLog(context.getString(R.string.log_warning_stopped))
                                        }
                                    },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                AppState.clearSharedItems()
                                AppState.addLog(context.getString(R.string.log_all_cleared))
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(stringResource(R.string.clear_all_button))
                        }
                    }
                }
            }
        }

        // Clients List Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.clients_header),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Pending Approvals Section
                    if (AppState.securityMode == SecurityMode.MANUAL_APPROVAL) {
                        Text(
                            text = "${stringResource(R.string.pending_approvals)} (${AppState.pendingApprovals.size})",
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                ),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (AppState.pendingApprovals.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_pending_approvals),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(bottom = 12.dp),
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                for (pending in AppState.pendingApprovals) {
                                    PendingApprovalRow(pending)
                                }
                            }
                        }
                    }

                    // Blocked Devices Section
                    if (AppState.blockedIps.isNotEmpty()) {
                        Text(
                            text = "${stringResource(R.string.blocked_ips_header)} (${AppState.blockedIps.size})",
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                ),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            for (blockedIp in AppState.blockedIps) {
                                BlockedIpRow(blockedIp)
                            }
                        }
                    }

                    // Active Sessions Section
                    Text(
                        text = "${stringResource(R.string.active_sessions)} (${AppState.activeSessions.size})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (AppState.activeSessions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_active_sessions),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            for (active in AppState.activeSessions) {
                                ActiveSessionRow(active)
                            }
                        }
                    }
                }
            }
        }
    }
}

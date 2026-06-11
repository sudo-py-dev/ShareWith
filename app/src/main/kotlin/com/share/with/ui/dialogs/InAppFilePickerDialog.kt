package com.share.with.ui.dialogs

import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.share.with.R
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppFilePickerDialog(
    isDirectoryMode: Boolean,
    onDismiss: () -> Unit,
    onPathSelected: (File) -> Unit,
) {
    var currentDir by remember { mutableStateOf(Environment.getExternalStorageDirectory() ?: File("/")) }
    val filesList =
        remember(currentDir) {
            try {
                currentDir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (isDirectoryMode) {
                Button(onClick = {
                    onPathSelected(currentDir)
                    onDismiss()
                }) {
                    Text(stringResource(R.string.picker_select_folder))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        },
        title = {
            Text(if (isDirectoryMode) stringResource(R.string.picker_select_folder) else stringResource(R.string.picker_select_file))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                Text(
                    text = currentDir.absolutePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val parentFile = currentDir.parentFile
                if (parentFile != null && parentFile.canRead()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { currentDir = parentFile }
                                .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.picker_go_up),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filesList) { file ->
                        val isDir = file.isDirectory
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isDir) {
                                            currentDir = file
                                        } else if (!isDirectoryMode) {
                                            onPathSelected(file)
                                            onDismiss()
                                        }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (isDir) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                                tint = if (isDir) Color(0xFFFBBF24) else Color(0xFF60A5FA),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
    )
}

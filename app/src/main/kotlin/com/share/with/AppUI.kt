package com.share.with

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.share.with.ui.components.LocalizedContext
import com.share.with.ui.dialogs.InAppFilePickerDialog
import com.share.with.ui.tabs.LogsTab
import com.share.with.ui.tabs.SettingsTab
import com.share.with.ui.tabs.ShareTab
import com.share.with.ui.utils.hasStoragePermission
import com.share.with.ui.utils.requestStoragePermission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUI() {
    var currentTab by remember { mutableStateOf("Share") }
    val context = LocalContext.current

    var showPickerMode by remember { mutableStateOf<Boolean?>(null) }

    val manageStorageLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = {
                if (hasStoragePermission(context)) {
                    AppState.addLog("Storage permission granted")
                } else {
                    AppState.addLog("Warning: Storage permission not granted")
                }
            },
        )

    val legacyStorageLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (isGranted) {
                    AppState.addLog("Storage permission granted")
                } else {
                    AppState.addLog("Warning: Storage permission not granted")
                }
            },
        )

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (isGranted) {
                    AppState.addLog("Notification permission granted")
                } else {
                    AppState.addLog("Warning: Notification permission denied. Background service status might not be visible.")
                }
                AppState.detectLocalIp()
                FileSharingService.startService(context)
            },
        )

    val checkAndOpenPicker = { isDir: Boolean ->
        if (hasStoragePermission(context)) {
            showPickerMode = isDir
        } else {
            requestStoragePermission(context, manageStorageLauncher, legacyStorageLauncher)
        }
    }

    val layoutDirection =
        if (AppState.selectedLanguage == "he" || AppState.selectedLanguage == "ar") {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }

    showPickerMode?.let { isDir ->
        InAppFilePickerDialog(
            isDirectoryMode = isDir,
            onDismiss = { showPickerMode = null },
            onPathSelected = { file ->
                AppState.addSharedItem(file, isDir)
            },
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        LocalizedContext(AppState.selectedLanguage) {
            key(AppState.selectedLanguage) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style =
                                        MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        ),
                                )
                            },
                            colors =
                                TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                        ) {
                            NavigationBarItem(
                                icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                                label = { Text(stringResource(R.string.tab_share)) },
                                selected = currentTab == "Share",
                                onClick = { currentTab = "Share" },
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text(stringResource(R.string.tab_settings)) },
                                selected = currentTab == "Settings",
                                onClick = { currentTab = "Settings" },
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                                label = { Text(stringResource(R.string.tab_logs)) },
                                selected = currentTab == "Logs",
                                onClick = { currentTab = "Logs" },
                            )
                        }
                    },
                ) { paddingValues ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .background(MaterialTheme.colorScheme.background)
                                .padding(16.dp),
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                        ) { tab ->
                            when (tab) {
                                "Share" ->
                                    ShareTab(
                                        onAddFile = { checkAndOpenPicker(false) },
                                        onAddFolder = { checkAndOpenPicker(true) },
                                        onRequestNotificationPermission = {
                                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        },
                                    )
                                "Settings" -> SettingsTab()
                                "Logs" -> LogsTab()
                            }
                        }
                    }
                }
            }
        }
    }
}

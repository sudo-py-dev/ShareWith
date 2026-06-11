package com.share.with.ui.tabs

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.share.with.AppState
import com.share.with.R
import com.share.with.SecurityMode
import com.share.with.ui.utils.isPortAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(onSelectCertificate: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var showPassword by remember { mutableStateOf(false) }
    var portError by remember { mutableStateOf<String?>(null) }
    var httpsPortError by remember { mutableStateOf<String?>(null) }
    var showHttpsPassword by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val checkBatteryOptimizations =
        remember(context) {
            {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (pm != null) {
                    pm.isIgnoringBatteryOptimizations(context.packageName)
                } else {
                    true
                }
            }
        }
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(checkBatteryOptimizations()) }
    var isWarningDismissed by remember { mutableStateOf(false) }
    val isStableBackground = isIgnoringBatteryOptimizations || isWarningDismissed

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    isIgnoringBatteryOptimizations = checkBatteryOptimizations()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val portValidationInvalidStr = stringResource(R.string.port_validation_invalid)
    val portValidationCollisionStr = stringResource(R.string.port_validation_collision)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Battery Optimization Warning Card
        if (!isStableBackground) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.battery_optimization_header),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.battery_optimization_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.battery_optimization_status_enabled),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f),
                                )
                                Button(
                                    onClick = { openBatteryOptimizationSettings(context) },
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                        ),
                                ) {
                                    Text(stringResource(R.string.battery_optimization_button))
                                }
                            }
                        }
                        IconButton(
                            onClick = { isWarningDismissed = true },
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss warning",
                                tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
        }
        // Network settings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.connection_settings_header),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.port_label),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    )
                    OutlinedTextField(
                        value = AppState.portInput,
                        onValueChange = { input ->
                            if (!AppState.serverRunning) {
                                AppState.updatePortInput(input)
                                val parsedPort = input.toIntOrNull()
                                if (parsedPort == null || parsedPort < 1 || parsedPort > 65535) {
                                    portError = portValidationInvalidStr
                                } else {
                                    coroutineScope.launch {
                                        val available =
                                            withContext(Dispatchers.IO) {
                                                isPortAvailable(parsedPort)
                                            }
                                        if (!available) {
                                            portError = portValidationCollisionStr
                                        } else {
                                            portError = null
                                            AppState.updateServerPort(parsedPort)
                                        }
                                    }
                                }
                            }
                        },
                        isError = portError != null,
                        supportingText = {
                            if (portError != null) {
                                Text(portError!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        enabled = !AppState.serverRunning,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        singleLine = true,
                    )
                }
            }
        }

        // Security Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.security_header),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )

                    Text(
                        text = stringResource(R.string.security_mode_label),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    )

                    var securityExpanded by remember { mutableStateOf(false) }
                    val securityOptions =
                        listOf(
                            SecurityMode.NONE to stringResource(R.string.security_mode_none),
                            SecurityMode.PASSWORD to stringResource(R.string.security_mode_password),
                            SecurityMode.MANUAL_APPROVAL to stringResource(R.string.security_mode_approval),
                        )
                    val currentSecurityName = securityOptions.find { it.first == AppState.securityMode }?.second ?: stringResource(R.string.security_mode_none)

                    ExposedDropdownMenuBox(
                        expanded = securityExpanded,
                        onExpandedChange = { securityExpanded = !securityExpanded },
                    ) {
                        OutlinedTextField(
                            value = currentSecurityName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = securityExpanded) },
                            modifier =
                                Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                    .fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        )
                        ExposedDropdownMenu(
                            expanded = securityExpanded,
                            onDismissRequest = { securityExpanded = false },
                        ) {
                            securityOptions.forEach { (mode, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        AppState.updateSecurityMode(mode)
                                        securityExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    val currentSecurityDesc =
                        when (AppState.securityMode) {
                            SecurityMode.NONE -> stringResource(R.string.security_mode_none_desc)
                            SecurityMode.PASSWORD -> stringResource(R.string.security_mode_password_desc)
                            SecurityMode.MANUAL_APPROVAL -> stringResource(R.string.security_mode_approval_desc)
                        }
                    Text(
                        text = currentSecurityDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )

                    if (AppState.securityMode == SecurityMode.PASSWORD) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.password_label),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        )
                        OutlinedTextField(
                            value = AppState.password,
                            onValueChange = { AppState.updatePassword(it) },
                            placeholder = { Text(stringResource(R.string.password_placeholder)) },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility",
                                    )
                                }
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                        )
                    }
                }
            }
        }

        // HTTPS Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.https_header),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.https_enabled_label),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Switch(
                            checked = AppState.isHttpsEnabled,
                            onCheckedChange = { AppState.updateHttpsEnabled(it) },
                            enabled = !AppState.serverRunning,
                        )
                    }

                    if (AppState.isHttpsEnabled) {
                        Text(
                            text = stringResource(R.string.https_port_label),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        )
                        OutlinedTextField(
                            value = AppState.httpsPortInput,
                            onValueChange = { input ->
                                if (!AppState.serverRunning) {
                                    AppState.updateHttpsPortInput(input)
                                    val parsedPort = input.toIntOrNull()
                                    if (parsedPort == null || parsedPort < 1 || parsedPort > 65535) {
                                        httpsPortError = portValidationInvalidStr
                                    } else {
                                        coroutineScope.launch {
                                            val available =
                                                withContext(Dispatchers.IO) {
                                                    isPortAvailable(parsedPort)
                                                }
                                            if (!available) {
                                                httpsPortError = portValidationCollisionStr
                                            } else {
                                                httpsPortError = null
                                                AppState.updateHttpsPort(parsedPort)
                                            }
                                        }
                                    }
                                }
                            },
                            isError = httpsPortError != null,
                            supportingText = {
                                if (httpsPortError != null) {
                                    Text(httpsPortError!!, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            enabled = !AppState.serverRunning,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        Text(
                            text = stringResource(R.string.https_certificate_label),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        )

                        val certName =
                            if (AppState.keystoreUri.isNotEmpty()) {
                                DocumentFile.fromSingleUri(context, AppState.keystoreUri.toUri())?.name
                                    ?: AppState.keystoreUri
                            } else {
                                stringResource(R.string.https_no_certificate)
                            }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = onSelectCertificate,
                                enabled = !AppState.serverRunning,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.https_select_certificate))
                            }
                        }
                        Text(
                            text =
                                if (AppState.keystoreUri.isNotEmpty()) {
                                    stringResource(R.string.https_certificate_selected, certName)
                                } else {
                                    certName
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )

                        Text(
                            text = stringResource(R.string.https_password_label),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        )
                        OutlinedTextField(
                            value = AppState.keystorePassword,
                            onValueChange = { AppState.updateKeystorePassword(it) },
                            placeholder = { Text(stringResource(R.string.https_password_placeholder)) },
                            singleLine = true,
                            enabled = !AppState.serverRunning,
                            visualTransformation = if (showHttpsPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showHttpsPassword = !showHttpsPassword }) {
                                    Icon(
                                        imageVector = if (showHttpsPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility",
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        // Appearance and Localization Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.appearance_interface_header),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )

                    // Theme selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.theme_label),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        )

                        var themeExpanded by remember { mutableStateOf(false) }
                        val themeOptions =
                            listOf(
                                "System" to stringResource(R.string.theme_system),
                                "Light" to stringResource(R.string.theme_light),
                                "Dark" to stringResource(R.string.theme_dark),
                            )
                        val currentThemeName = themeOptions.find { it.first == AppState.selectedTheme }?.second ?: stringResource(R.string.theme_system)

                        ExposedDropdownMenuBox(
                            expanded = themeExpanded,
                            onExpandedChange = { themeExpanded = !themeExpanded },
                        ) {
                            OutlinedTextField(
                                value = currentThemeName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                                modifier =
                                    Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                        .fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            )
                            ExposedDropdownMenu(
                                expanded = themeExpanded,
                                onDismissRequest = { themeExpanded = false },
                            ) {
                                themeOptions.forEach { (themeValue, themeName) ->
                                    DropdownMenuItem(
                                        text = { Text(themeName) },
                                        onClick = {
                                            AppState.updateTheme(themeValue)
                                            themeExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Language Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.language_label),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        )

                        var langExpanded by remember { mutableStateOf(false) }
                        val languageOptions =
                            listOf(
                                "en" to stringResource(R.string.language_en),
                                "he" to stringResource(R.string.language_he),
                                "fr" to stringResource(R.string.language_fr),
                                "ru" to stringResource(R.string.language_ru),
                                "es" to stringResource(R.string.language_es),
                                "ar" to stringResource(R.string.language_ar),
                            )
                        val currentLangName = languageOptions.find { it.first == AppState.selectedLanguage }?.second ?: stringResource(R.string.language_en)

                        ExposedDropdownMenuBox(
                            expanded = langExpanded,
                            onExpandedChange = { langExpanded = !langExpanded },
                        ) {
                            OutlinedTextField(
                                value = currentLangName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                                modifier =
                                    Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                        .fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            )
                            ExposedDropdownMenu(
                                expanded = langExpanded,
                                onDismissRequest = { langExpanded = false },
                            ) {
                                languageOptions.forEach { (code, name) ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            AppState.updateLanguage(code)
                                            val locale =
                                                when (code) {
                                                    "he" -> Locale.forLanguageTag("he")
                                                    else -> Locale.forLanguageTag(code)
                                                }
                                            Locale.setDefault(locale)
                                            langExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun openBatteryOptimizationSettings(context: Context) {
    val isXiaomi = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
    var opened = false

    if (isXiaomi) {
        // 1. Try MIUI HiddenAppsConfigActivity (App Battery Saver)
        try {
            val intent =
                Intent().apply {
                    component =
                        android.content.ComponentName(
                            "com.miui.powerkeeper",
                            "com.miui.powerkeeper.ui.HiddenAppsConfigActivity",
                        )
                    putExtra("package_name", context.packageName)
                    putExtra(
                        "package_label",
                        context.applicationInfo.loadLabel(context.packageManager).toString(),
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
            opened = true
        } catch (e: Exception) {
            AppState.addLog("Xiaomi battery saver intent failed, trying App Info: ${e.message}")
        }

        // 2. Try App Details / Info settings
        if (!opened) {
            try {
                val intent =
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = "package:${context.packageName}".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                context.startActivity(intent)
                opened = true
            } catch (e: Exception) {
                AppState.addLog("Xiaomi App Info intent failed, trying Autostart: ${e.message}")
            }
        }

        // 3. Try Autostart Settings
        if (!opened) {
            try {
                val intent =
                    Intent().apply {
                        component =
                            android.content.ComponentName(
                                "com.miui.securitycenter",
                                "com.miui.permcenter.autostart.AutoStartManagementActivity",
                            )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                context.startActivity(intent)
                opened = true
            } catch (e: Exception) {
                AppState.addLog("Xiaomi Autostart intent failed: ${e.message}")
            }
        }
    }

    // 4. Try standard Android ignore battery optimization settings
    if (!opened) {
        try {
            val intent =
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
            opened = true
        } catch (e: Exception) {
            AppState.addLog("Standard ignore battery optimization settings failed: ${e.message}")
        }
    }

    // 5. Try standard Android App Details / Info
    if (!opened) {
        try {
            val intent =
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:${context.packageName}".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
            opened = true
        } catch (e: Exception) {
            AppState.addLog("Standard App Info settings failed: ${e.message}")
        }
    }

    // 6. Try standard system settings
    if (!opened) {
        try {
            val intent =
                Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
            opened = true
        } catch (e: Exception) {
            AppState.addLog("Standard settings failed: ${e.message}")
        }
    }
}

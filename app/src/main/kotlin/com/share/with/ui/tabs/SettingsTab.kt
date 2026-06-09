package com.share.with.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.share.with.AppState
import com.share.with.R
import com.share.with.SecurityMode
import com.share.with.ui.components.SecurityOptionRow
import com.share.with.ui.components.SegmentedControl
import com.share.with.ui.utils.isPortAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab() {
    val coroutineScope = rememberCoroutineScope()
    var showPassword by remember { mutableStateOf(false) }
    var portError by remember { mutableStateOf<String?>(null) }
    
    val portValidationInvalidStr = stringResource(R.string.port_validation_invalid)
    val portValidationCollisionStr = stringResource(R.string.port_validation_collision)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Network settings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.connection_settings_header),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.port_label),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    OutlinedTextField(
                        value = AppState.portInput,
                        onValueChange = { input ->
                            if (!AppState.serverRunning) {
                                AppState.portInput = input
                                val parsedPort = input.toIntOrNull()
                                if (parsedPort == null || parsedPort < 1 || parsedPort > 65535) {
                                    portError = portValidationInvalidStr
                                } else {
                                    coroutineScope.launch {
                                        val available = withContext(Dispatchers.IO) {
                                            isPortAvailable(parsedPort)
                                        }
                                        if (!available) {
                                            portError = portValidationCollisionStr
                                        } else {
                                            portError = null
                                            AppState.serverPort = parsedPort
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        singleLine = true
                    )
                }
            }
        }

        // Security Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.security_header),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Text(
                        text = stringResource(R.string.security_mode_label),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecurityOptionRow(
                            title = stringResource(R.string.security_mode_none),
                            description = stringResource(R.string.security_mode_none_desc),
                            selected = AppState.securityMode == SecurityMode.NONE,
                            onClick = { AppState.securityMode = SecurityMode.NONE }
                        )
                        
                        SecurityOptionRow(
                            title = stringResource(R.string.security_mode_password),
                            description = stringResource(R.string.security_mode_password_desc),
                            selected = AppState.securityMode == SecurityMode.PASSWORD,
                            onClick = { AppState.securityMode = SecurityMode.PASSWORD }
                        )

                        SecurityOptionRow(
                            title = stringResource(R.string.security_mode_approval),
                            description = stringResource(R.string.security_mode_approval_desc),
                            selected = AppState.securityMode == SecurityMode.MANUAL_APPROVAL,
                            onClick = { AppState.securityMode = SecurityMode.MANUAL_APPROVAL }
                        )
                    }

                    if (AppState.securityMode == SecurityMode.PASSWORD) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.password_label),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        OutlinedTextField(
                            value = AppState.password,
                            onValueChange = { AppState.password = it },
                            placeholder = { Text(stringResource(R.string.password_placeholder)) },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.appearance_interface_header),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    // Theme selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.theme_label),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        
                        val themeOptions = listOf(
                            stringResource(R.string.theme_system),
                            stringResource(R.string.theme_light),
                            stringResource(R.string.theme_dark)
                        )
                        val themeIndex = when (AppState.selectedTheme) {
                            "System" -> 0
                            "Light" -> 1
                            "Dark" -> 2
                            else -> 0
                        }
                        SegmentedControl(
                            options = themeOptions,
                            selectedIndex = themeIndex,
                            onOptionSelected = { index ->
                                AppState.selectedTheme = when (index) {
                                    0 -> "System"
                                    1 -> "Light"
                                    2 -> "Dark"
                                    else -> "System"
                                }
                            }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Language Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.language_label),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        
                        var langExpanded by remember { mutableStateOf(false) }
                        val languageOptions = listOf(
                            "en" to stringResource(R.string.language_en),
                            "he" to stringResource(R.string.language_he),
                            "fr" to stringResource(R.string.language_fr),
                            "ru" to stringResource(R.string.language_ru),
                            "es" to stringResource(R.string.language_es),
                            "ar" to stringResource(R.string.language_ar)
                        )
                        val currentLangName = languageOptions.find { it.first == AppState.selectedLanguage }?.second ?: stringResource(R.string.language_en)

                        ExposedDropdownMenuBox(
                            expanded = langExpanded,
                            onExpandedChange = { langExpanded = !langExpanded }
                        ) {
                            OutlinedTextField(
                                value = currentLangName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = langExpanded,
                                onDismissRequest = { langExpanded = false }
                            ) {
                                languageOptions.forEach { (code, name) ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            AppState.selectedLanguage = code
                                            val locale = when (code) {
                                                "he" -> Locale.forLanguageTag("he")
                                                else -> Locale.forLanguageTag(code)
                                            }
                                            Locale.setDefault(locale)
                                            langExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Blocked IPs Card
        if (AppState.blockedIps.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.blocked_ips_header),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        
                        for (ip in AppState.blockedIps) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = ip, style = MaterialTheme.typography.bodyMedium)
                                androidx.compose.material3.TextButton(onClick = { AppState.unblockIp(ip) }) {
                                    Text(stringResource(R.string.unblock_button), color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package org.mlanau.project.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.notification.presentation.NotificationPermissionStatus
import org.mlanau.project.notification.presentation.rememberNotificationPermissions
import org.mlanau.project.settings.domain.ThemeMode
import org.mlanau.project.shared.ui.component.ScreenHeader
import org.mlanau.project.shared.ui.component.SectionLabel
import plantitas_app.shared.generated.resources.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    val notificationPermissions = rememberNotificationPermissions()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(modifier = Modifier.fillMaxSize().widthIn(max = 640.dp)) {
                ScreenHeader(
                    title = stringResource(Res.string.settings_title),
                    onNavigateBack = onBack,
                    backContentDescription = stringResource(Res.string.common_back)
                )

                SettingsGroup(stringResource(Res.string.settings_theme_section)) {
                    SettingsRow(
                        icon = Icons.Default.Palette,
                        title = stringResource(Res.string.settings_theme),
                        value = themeModeLabel(uiState.themeMode),
                        onClick = { showThemeDialog = true }
                    )
                }

                SettingsGroup(stringResource(Res.string.settings_language_section)) {
                    SettingsRow(
                        icon = Icons.Default.Language,
                        title = if (uiState.languageCode == "es") stringResource(Res.string.settings_language_es)
                        else stringResource(Res.string.settings_language_en),
                        onClick = { showLanguageDialog = true }
                    )
                }

                SettingsGroup(stringResource(Res.string.settings_notifications_section)) {
                    val granted = notificationPermissions.status == NotificationPermissionStatus.GRANTED
                    SettingsRow(
                        icon = Icons.Default.Notifications,
                        title = stringResource(Res.string.settings_notifications_permission),
                        value = if (granted) stringResource(Res.string.settings_notifications_granted)
                        else stringResource(Res.string.settings_notifications_denied),
                        onClick = if (granted) null else ({ notificationPermissions.openAppNotificationSettings() })
                    )
                    if (!notificationPermissions.exactAlarmsAllowed) {
                        SettingsRow(
                            icon = Icons.Default.Alarm,
                            title = stringResource(Res.string.settings_notifications_exact_alarms),
                            value = stringResource(Res.string.settings_notifications_exact_alarms_denied),
                            onClick = { notificationPermissions.openExactAlarmSettings() }
                        )
                    }
                }

                SettingsGroup(stringResource(Res.string.settings_about_section)) {
                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = stringResource(Res.string.settings_about_app),
                        onClick = onNavigateToAbout
                    )
                }
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguageCode = uiState.languageCode,
            onLanguageSelected = {
                viewModel.setLanguage(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            current = uiState.themeMode,
            onSelected = {
                viewModel.setThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    SectionLabel(text = title, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp))
    content()
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (value != null) {
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.LIGHT -> stringResource(Res.string.settings_theme_light)
    ThemeMode.DARK -> stringResource(Res.string.settings_theme_dark)
    ThemeMode.SYSTEM -> stringResource(Res.string.settings_theme_system)
}

@Composable
fun ThemeSelectionDialog(
    current: ThemeMode,
    onSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_theme)) },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(mode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = current == mode, onClick = { onSelected(mode) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = themeModeLabel(mode), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_close)) }
        }
    )
}

@Composable
fun LanguageSelectionDialog(
    currentLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_language_section)) },
        text = {
            Column {
                LanguageOption(
                    label = stringResource(Res.string.settings_language_es),
                    selected = currentLanguageCode == "es",
                    onClick = { onLanguageSelected("es") }
                )
                LanguageOption(
                    label = stringResource(Res.string.settings_language_en),
                    selected = currentLanguageCode == "en",
                    onClick = { onLanguageSelected("en") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_close)) }
        }
    )
}

@Composable
private fun LanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

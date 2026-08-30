package org.mlanau.project.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.notification.presentation.NotificationPermissionStatus
import org.mlanau.project.notification.presentation.rememberNotificationPermissions
import org.mlanau.project.settings.domain.ThemeMode
import plantitas_app.shared.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.common_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SettingsSectionTitle(stringResource(Res.string.settings_theme_section))
            ListItem(
                headlineContent = { Text(stringResource(Res.string.settings_theme)) },
                supportingContent = { Text(themeModeLabel(uiState.themeMode)) },
                leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
                modifier = Modifier.clickable { showThemeDialog = true }
            )

            HorizontalDivider()

            SettingsSectionTitle(stringResource(Res.string.settings_language_section))
            ListItem(
                headlineContent = {
                    Text(
                        if (uiState.languageCode == "es") stringResource(Res.string.settings_language_es)
                        else stringResource(Res.string.settings_language_en)
                    )
                },
                leadingContent = { Icon(Icons.Default.Language, contentDescription = null) },
                modifier = Modifier.clickable { showLanguageDialog = true }
            )

            HorizontalDivider()

            SettingsSectionTitle(stringResource(Res.string.settings_notifications_section))
            ListItem(
                headlineContent = { Text(stringResource(Res.string.settings_notifications_permission)) },
                supportingContent = {
                    Text(
                        if (notificationPermissions.status == NotificationPermissionStatus.GRANTED) {
                            stringResource(Res.string.settings_notifications_granted)
                        } else {
                            stringResource(Res.string.settings_notifications_denied)
                        }
                    )
                },
                leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                modifier = if (notificationPermissions.status == NotificationPermissionStatus.GRANTED) {
                    Modifier
                } else {
                    Modifier.clickable { notificationPermissions.openAppNotificationSettings() }
                }
            )
            if (!notificationPermissions.exactAlarmsAllowed) {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_notifications_exact_alarms)) },
                    supportingContent = { Text(stringResource(Res.string.settings_notifications_exact_alarms_denied)) },
                    leadingContent = { Icon(Icons.Default.Alarm, contentDescription = null) },
                    modifier = Modifier.clickable { notificationPermissions.openExactAlarmSettings() }
                )
            }

            HorizontalDivider()

            SettingsSectionTitle(stringResource(Res.string.settings_about_section))
            ListItem(
                headlineContent = { Text(stringResource(Res.string.settings_about_app)) },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.clickable { onNavigateToAbout() }
            )
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
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        RadioButton(selected = current == mode, onClick = { onSelected(mode) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = themeModeLabel(mode), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_close))
            }
        }
    )
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_close))
            }
        }
    )
}

@Composable
fun LanguageOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

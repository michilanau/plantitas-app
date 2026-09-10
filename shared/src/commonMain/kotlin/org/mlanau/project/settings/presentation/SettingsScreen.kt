package org.mlanau.project.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.notification.presentation.NotificationPermissionStatus
import org.mlanau.project.notification.presentation.rememberNotificationPermissions
import org.mlanau.project.settings.domain.ThemeMode
import org.mlanau.project.shared.ui.component.AppCard
import org.mlanau.project.shared.ui.component.AppChip
import org.mlanau.project.shared.ui.component.ScreenHeader
import org.mlanau.project.shared.ui.component.SectionLabel
import org.mlanau.project.shared.ui.theme.ContentMaxWidth
import org.mlanau.project.shared.ui.theme.LocalCareColors
import org.mlanau.project.shared.ui.theme.RowIconSize
import org.mlanau.project.shared.ui.theme.ScreenGutter
import plantitas_app.shared.generated.resources.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val notificationPermissions = rememberNotificationPermissions()
    val careColors = LocalCareColors.current
    val colors = MaterialTheme.colorScheme

    Scaffold(containerColor = colors.background) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = ContentMaxWidth)
                    .verticalScroll(rememberScrollState())
            ) {
                ScreenHeader(
                    title = stringResource(Res.string.settings_title),
                    onNavigateBack = onBack,
                    backContentDescription = stringResource(Res.string.common_back)
                )

                Column(modifier = Modifier.padding(start = ScreenGutter, end = ScreenGutter, bottom = 32.dp)) {
                    SettingsSection(stringResource(Res.string.settings_theme_section)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                AppChip(
                                    label = themeModeLabel(mode),
                                    selected = uiState.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) }
                                )
                            }
                        }
                    }

                    SettingsSection(stringResource(Res.string.settings_language_section)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppChip(
                                label = stringResource(Res.string.settings_language_es),
                                selected = uiState.languageCode == "es",
                                onClick = { viewModel.setLanguage("es") }
                            )
                            AppChip(
                                label = stringResource(Res.string.settings_language_en),
                                selected = uiState.languageCode == "en",
                                onClick = { viewModel.setLanguage("en") }
                            )
                        }
                    }

                    SettingsSection(stringResource(Res.string.settings_notifications_section)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val granted = notificationPermissions.status == NotificationPermissionStatus.GRANTED
                            SettingsRow(
                                icon = Res.drawable.ic_bell,
                                badgeColor = careColors.fertilizeContainer,
                                badgeContent = careColors.fertilize,
                                title = stringResource(Res.string.settings_notifications_permission),
                                value = if (granted) {
                                    stringResource(Res.string.settings_notifications_granted)
                                } else {
                                    stringResource(Res.string.settings_notifications_denied)
                                },
                                onClick = if (granted) null else ({ notificationPermissions.openAppNotificationSettings() })
                            )
                            if (!notificationPermissions.exactAlarmsAllowed) {
                                SettingsRow(
                                    icon = Res.drawable.ic_clock,
                                    badgeColor = careColors.waterContainer,
                                    badgeContent = careColors.water,
                                    title = stringResource(Res.string.settings_notifications_exact_alarms),
                                    value = stringResource(Res.string.settings_notifications_exact_alarms_denied),
                                    onClick = { notificationPermissions.openExactAlarmSettings() }
                                )
                            }
                        }
                    }

                    SettingsSection(stringResource(Res.string.settings_about_section)) {
                        SettingsRow(
                            icon = Res.drawable.ic_leaf,
                            badgeColor = colors.primaryContainer,
                            badgeContent = colors.onPrimaryContainer,
                            title = stringResource(Res.string.settings_about_app),
                            onClick = onNavigateToAbout
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    SectionLabel(text = title, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
    content()
}

@Composable
private fun SettingsRow(
    icon: DrawableResource,
    badgeColor: Color,
    badgeContent: Color,
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null
) {
    AppCard(onClick = onClick) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = badgeContent,
                    modifier = Modifier.size(RowIconSize)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (value != null) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onClick != null) {
                Icon(
                    painter = painterResource(Res.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(RowIconSize)
                )
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

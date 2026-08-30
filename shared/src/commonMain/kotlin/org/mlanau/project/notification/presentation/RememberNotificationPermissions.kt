package org.mlanau.project.notification.presentation

import androidx.compose.runtime.Composable

/**
 * Reads the live notification-permission state and keeps it fresh while the caller is composed
 * (it re-checks when the app returns to the foreground, so coming back from the system settings
 * updates the UI on its own).
 */
@Composable
expect fun rememberNotificationPermissions(): NotificationPermissions

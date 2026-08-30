package org.mlanau.project.notification.presentation

/**
 * Whether the OS currently lets this app post notifications.
 *
 * [NOT_DETERMINED] means the user has never been asked — only iOS reports it reliably; Android
 * collapses "never asked" into [DENIED] because it cannot be told apart without an Activity.
 */
enum class NotificationPermissionStatus { GRANTED, DENIED, NOT_DETERMINED }

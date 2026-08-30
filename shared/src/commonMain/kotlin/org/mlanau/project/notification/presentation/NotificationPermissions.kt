package org.mlanau.project.notification.presentation

/**
 * The notification-permission state of the running app plus the actions the UI can take on it.
 * Obtained from composition via [rememberNotificationPermissions]; a fresh instance is produced
 * whenever [status] or [exactAlarmsAllowed] changes.
 */
interface NotificationPermissions {
    val status: NotificationPermissionStatus

    /**
     * Whether the app may schedule exact alarms. Always `true` on iOS; on Android 12+ it depends
     * on a separate user grant, and when it is `false` reminders fall back to inexact alarms and
     * may fire late.
     */
    val exactAlarmsAllowed: Boolean

    /** Ask the OS for the notification permission (shows the system prompt when it still can). */
    fun request()

    /** Open this app's notification settings screen in the OS. */
    fun openAppNotificationSettings()

    /** Open the OS screen where the user grants "exact alarms". No-op where it does not apply. */
    fun openExactAlarmSettings()
}

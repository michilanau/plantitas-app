package org.mlanau.project.notification.infrastructure

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.mlanau.project.notification.domain.port.NotificationId
import org.mlanau.project.notification.domain.port.NotificationScheduler
import org.mlanau.project.notification.domain.port.ScheduledNotification
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

class IosNotificationScheduler : NotificationScheduler {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    init {
        center.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound,
            completionHandler = { _, _ -> }
        )
    }

    override suspend fun schedule(notification: ScheduledNotification) {
        // Replace whatever was previously scheduled under this id.
        center.removePendingNotificationRequestsWithIdentifiers(listOf(notification.id.value))

        val content = UNMutableNotificationContent().apply {
            setTitle(notification.title)
            setBody(notification.body)
            setSound(UNNotificationSound.defaultSound)
        }

        val timeZone = TimeZone.currentSystemDefault()
        val localDateTime = notification.at.toLocalDateTime(timeZone)
        val dateComponents = NSDateComponents().apply {
            year = localDateTime.year.toLong()
            month = localDateTime.monthNumber.toLong()
            day = localDateTime.day.toLong()
            hour = localDateTime.hour.toLong()
            minute = localDateTime.minute.toLong()
            second = localDateTime.second.toLong()
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents = dateComponents,
            repeats = false
        )

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = notification.id.value,
            content = content,
            trigger = trigger
        )
        center.addNotificationRequest(request, withCompletionHandler = null)
    }

    override fun cancel(id: NotificationId) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(id.value))
    }
}

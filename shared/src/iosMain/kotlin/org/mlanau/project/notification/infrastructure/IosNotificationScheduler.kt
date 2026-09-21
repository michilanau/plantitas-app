package org.mlanau.project.notification.infrastructure

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.mlanau.project.notification.domain.port.NotificationId
import org.mlanau.project.notification.domain.port.NotificationScheduler
import org.mlanau.project.notification.domain.port.ScheduledNotification
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

class IosNotificationScheduler : NotificationScheduler {

    // Notification permission is requested from the UI (see rememberNotificationPermissions),
    // which can react to the user's answer — not fire-and-forget from here.
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    /**
     * iOS delivers a local notification without running any of the app's code, so a reminder
     * cannot arm the one after it: the whole run has to be registered while the app is open, and
     * it keeps firing on its own until the user next opens the app and it is recomputed.
     *
     * The run is as long as the app's share of [PENDING_BUDGET] allows. iOS keeps only the 64
     * soonest pending requests per app and silently drops the rest, so the budget stays under that
     * with room to spare.
     */
    override fun seriesLengthFor(activeRuleCount: Int): Int {
        if (activeRuleCount <= 0) return 0
        return (PENDING_BUDGET / activeRuleCount)
            .coerceIn(1, NotificationScheduler.MAX_SERIES_LENGTH)
    }

    override suspend fun schedule(series: List<ScheduledNotification>) {
        // The first element carries the series' base id (see NotificationId.inSeries).
        val base = series.firstOrNull()?.id ?: return
        removeSeries(base)

        series.forEach { notification ->
            val content = UNMutableNotificationContent().apply {
                setTitle(notification.title)
                setBody(notification.body)
                setSound(UNNotificationSound.defaultSound)
                // One id per occurrence means these no longer replace each other in Notification
                // Center the way a single reminder per rule did. Sharing a thread identifier is
                // what keeps a rule's run of overdue nags collapsed into one group there instead
                // of piling up as separate notifications.
                setThreadIdentifier(base.value)
            }

            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                dateComponents = notification.at.dateComponents(),
                repeats = false
            )

            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = notification.id.value,
                content = content,
                trigger = trigger
            )
            center.addNotificationRequest(request, withCompletionHandler = null)
        }
    }

    override fun cancel(id: NotificationId) = removeSeries(id)

    /**
     * Clears every slot the series could occupy, not just the ones about to be written: a rule
     * edited to a longer interval yields a shorter run, and the tail of the previous one would
     * otherwise stay armed. Delivered notifications go too, so reopening the app doesn't leave
     * superseded nags sitting in Notification Center.
     */
    private fun removeSeries(base: NotificationId) {
        val ids = List(NotificationScheduler.MAX_SERIES_LENGTH) { base.inSeries(it).value }
        center.removePendingNotificationRequestsWithIdentifiers(ids)
        center.removeDeliveredNotificationsWithIdentifiers(ids)
    }

    private fun kotlin.time.Instant.dateComponents(): NSDateComponents {
        val localDateTime = toLocalDateTime(TimeZone.currentSystemDefault())
        return NSDateComponents().apply {
            year = localDateTime.year.toLong()
            month = localDateTime.monthNumber.toLong()
            day = localDateTime.day.toLong()
            hour = localDateTime.hour.toLong()
            minute = localDateTime.minute.toLong()
            second = localDateTime.second.toLong()
        }
    }

    private companion object {
        const val PENDING_BUDGET = 56
    }
}

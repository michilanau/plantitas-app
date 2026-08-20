package org.mlanau.project.plant.infrastructure.notification

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.service.CareNotificationScheduler
import org.mlanau.project.plant.domain.service.CareReminderRequest
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.notification_fertilize
import plantitas_app.shared.generated.resources.notification_fertilize_overdue
import plantitas_app.shared.generated.resources.notification_repot
import plantitas_app.shared.generated.resources.notification_repot_overdue
import plantitas_app.shared.generated.resources.notification_title
import plantitas_app.shared.generated.resources.notification_water
import plantitas_app.shared.generated.resources.notification_water_overdue
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

/**
 * Mirrors Android's scheduler: it also only ever has one request pending per rule, and is
 * re-scheduled after every mutation (toggle/skip/save) rather than relying on a native repeating
 * trigger — but unlike the previous version of this adapter, it no longer makes that scheduling
 * *decision* itself. It arrives already resolved in [CareReminderRequest] from
 * [org.mlanau.project.plant.application.RescheduleCareReminder]; this class only turns that into
 * platform strings and a `UNNotificationRequest`. That's why a single one-shot
 * [UNCalendarNotificationTrigger] — firing at the request's absolute date/time — is used for both
 * `Once` and `Periodic` rules; there's no separate use for `UNTimeIntervalNotificationTrigger` (a
 * relative delay) here.
 *
 * Unlike Android, there's no receiver hook to run code exactly when a notification fires, so a
 * `Periodic` rule can't chain its next alarm the moment the previous one is shown — iOS only
 * catches up the next time `RescheduleCareReminder` runs, which is on every mutation and on every
 * app launch (see the shared `RescheduleAllCareReminders` use case invoked from `App`). A rule left
 * completely untouched for more than one period while the app stays unopened will miss reminders
 * in between; there is currently no background rescheduling to cover that gap.
 */
class IosCareNotificationScheduler : CareNotificationScheduler {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    init {
        center.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound,
            completionHandler = { _, _ -> }
        )
    }

    override suspend fun schedule(request: CareReminderRequest) {
        val requestId = requestIdentifier(request.ruleId)

        // Replace whatever was previously scheduled for this rule.
        center.removePendingNotificationRequestsWithIdentifiers(listOf(requestId))

        val content = UNMutableNotificationContent().apply {
            setTitle(getString(Res.string.notification_title))
            setBody(bodyFor(request))
            setSound(UNNotificationSound.defaultSound)
        }

        val timeZone = TimeZone.currentSystemDefault()
        val localDateTime = request.at.toLocalDateTime(timeZone)
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

        val nativeRequest = UNNotificationRequest.requestWithIdentifier(
            identifier = requestId,
            content = content,
            trigger = trigger
        )
        center.addNotificationRequest(nativeRequest, withCompletionHandler = null)
    }

    override fun cancel(ruleId: CareRuleId) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(requestIdentifier(ruleId)))
    }

    private fun requestIdentifier(ruleId: CareRuleId): String = "care-rule-${ruleId.value}"

    private suspend fun bodyFor(request: CareReminderRequest): String {
        val days = request.daysSinceLastCareAtFireTime
        return if (days == null) {
            when (request.type) {
                CareType.WATER -> getString(Res.string.notification_water, request.plantName)
                CareType.FERTILIZE -> getString(Res.string.notification_fertilize, request.plantName)
                CareType.REPOT -> getString(Res.string.notification_repot, request.plantName)
            }
        } else {
            when (request.type) {
                CareType.WATER -> getString(Res.string.notification_water_overdue, request.plantName, days)
                CareType.FERTILIZE -> getString(Res.string.notification_fertilize_overdue, request.plantName, days)
                CareType.REPOT -> getString(Res.string.notification_repot_overdue, request.plantName, days)
            }
        }
    }
}

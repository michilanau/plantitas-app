package org.mlanau.project.plant.application

import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import org.jetbrains.compose.resources.getString
import org.mlanau.project.notification.domain.port.ScheduledNotification
import org.mlanau.project.plant.domain.model.CareReminder
import org.mlanau.project.plant.domain.model.CareType
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.notification_fertilize
import plantitas_app.shared.generated.resources.notification_fertilize_overdue
import plantitas_app.shared.generated.resources.notification_repot
import plantitas_app.shared.generated.resources.notification_repot_overdue
import plantitas_app.shared.generated.resources.notification_title
import plantitas_app.shared.generated.resources.notification_water
import plantitas_app.shared.generated.resources.notification_water_overdue

/**
 * The notifications that show a rule's [reminders], in the order they will fire.
 *
 * Their ids all descend from the rule's own, so the series as a whole replaces the previous one
 * rather than stacking with it, and the first keeps the rule's bare id — the only one a platform
 * that chains its reminders at fire time ever registers.
 *
 * A reminder that is merely due says what to do; an overdue one says how long it has been waiting,
 * counted from [org.mlanau.project.plant.domain.model.CareTask.Pending.dueAt] (the day it was
 * first owed) rather than from the last care, so "N days late" keeps growing for a plant that has
 * never been watered at all.
 */
internal suspend fun careReminderNotifications(
    reminders: List<CareReminder>,
    plantName: String,
    timeZone: TimeZone
): List<ScheduledNotification> {
    val title = getString(Res.string.notification_title)
    return reminders.mapIndexed { index, reminder ->
        val task = reminder.task
        ScheduledNotification(
            id = careReminderNotificationId(task.careRuleId).inSeries(index),
            at = reminder.at,
            title = title,
            body = reminderBody(reminder, plantName, timeZone)
        )
    }
}

private suspend fun reminderBody(reminder: CareReminder, plantName: String, timeZone: TimeZone): String {
    val task = reminder.task
    if (!task.isOverdue) {
        return when (task.type) {
            CareType.WATER -> getString(Res.string.notification_water, plantName)
            CareType.FERTILIZE -> getString(Res.string.notification_fertilize, plantName)
            CareType.REPOT -> getString(Res.string.notification_repot, plantName)
        }
    }
    val daysLate = task.dueAt.daysUntil(reminder.at, timeZone)
    return when (task.type) {
        CareType.WATER -> getString(Res.string.notification_water_overdue, plantName, daysLate)
        CareType.FERTILIZE -> getString(Res.string.notification_fertilize_overdue, plantName, daysLate)
        CareType.REPOT -> getString(Res.string.notification_repot_overdue, plantName, daysLate)
    }
}

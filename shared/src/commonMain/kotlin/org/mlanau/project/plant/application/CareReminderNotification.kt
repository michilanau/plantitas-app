package org.mlanau.project.plant.application

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import org.jetbrains.compose.resources.getString
import org.mlanau.project.notification.domain.port.ScheduledNotification
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.CareTask
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
 * The single notification a rule shows at [at].
 *
 * There is exactly one per rule — the id is derived from [CareRuleId], so each new alarm replaces
 * the previous one instead of stacking — which is what lets an overdue task nag daily without the
 * user ever seeing two reminders for the same plant and care.
 *
 * A task that is merely due says what to do; an overdue one says how long it has been waiting,
 * counted from [CareTask.Pending.dueAt] (the day it was first owed) rather than from the last
 * care, so "N days late" keeps growing for a plant that has never been watered at all.
 */
internal suspend fun careReminderNotification(
    task: CareTask.Pending,
    plantName: String,
    at: Instant,
    timeZone: TimeZone
): ScheduledNotification {
    val title = getString(Res.string.notification_title)
    val body = if (task.isOverdue) {
        val daysLate = task.dueAt.daysUntil(at, timeZone)
        when (task.type) {
            CareType.WATER -> getString(Res.string.notification_water_overdue, plantName, daysLate)
            CareType.FERTILIZE -> getString(Res.string.notification_fertilize_overdue, plantName, daysLate)
            CareType.REPOT -> getString(Res.string.notification_repot_overdue, plantName, daysLate)
        }
    } else {
        when (task.type) {
            CareType.WATER -> getString(Res.string.notification_water, plantName)
            CareType.FERTILIZE -> getString(Res.string.notification_fertilize, plantName)
            CareType.REPOT -> getString(Res.string.notification_repot, plantName)
        }
    }
    return ScheduledNotification(
        id = careReminderNotificationId(task.careRuleId),
        at = at,
        title = title,
        body = body
    )
}

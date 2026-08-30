package org.mlanau.project.plant.application

import kotlin.time.Clock
import org.mlanau.project.notification.domain.port.NotificationScheduler
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.port.CareRuleRepository
import org.mlanau.project.plant.domain.port.CareTaskRepository
import org.mlanau.project.plant.domain.port.PlantRepository
import org.mlanau.project.plant.domain.service.CareScheduler
import org.mlanau.project.shared.time.SystemTimeZoneProvider
import org.mlanau.project.shared.time.TimeZoneProvider

/**
 * Re-arms a single rule's reminder. This is what chains one alarm to the next: a platform alarm
 * fires once and doesn't repeat, so the receiver that showed it calls this to schedule the
 * following one.
 *
 * The instant always comes from [CareScheduler.nextReminderAt], which is strictly in the future,
 * so this call can never re-trigger itself immediately.
 */
class SyncCareReminder(
    private val careRuleRepository: CareRuleRepository,
    private val careTaskRepository: CareTaskRepository,
    private val plantRepository: PlantRepository,
    private val scheduler: CareScheduler,
    private val notifications: NotificationScheduler,
    private val clock: Clock = Clock.System,
    private val timeZoneProvider: TimeZoneProvider = SystemTimeZoneProvider
) {
    suspend operator fun invoke(ruleId: CareRuleId) {
        val notificationId = careReminderNotificationId(ruleId)
        val rule = careRuleRepository.findById(ruleId)
        if (rule == null || !rule.notificationsEnabled) {
            notifications.cancel(notificationId)
            return
        }

        val plantId = rule.plantId
        val plant = plantId?.let { plantRepository.findById(it) }
        if (plant == null) {
            notifications.cancel(notificationId)
            return
        }

        val now = clock.now()
        val timeZone = timeZoneProvider()
        val lastCareAt = careTaskRepository.lastCareDate(plantId, rule.type)

        val task = scheduler.nextPending(rule, lastCareAt, now, timeZone)
        val at = scheduler.nextReminderAt(rule, lastCareAt, now, timeZone)
        if (task == null || at == null) {
            notifications.cancel(notificationId)
            return
        }

        notifications.schedule(careReminderNotification(task, plant.name, at, timeZone))
    }
}

package org.mlanau.project.plant.application

import kotlin.time.Clock
import kotlinx.coroutines.flow.first
import kotlinx.datetime.daysUntil
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.repository.CareRepository
import org.mlanau.project.plant.domain.repository.PlantRepository
import org.mlanau.project.plant.domain.service.CareNotificationScheduler
import org.mlanau.project.plant.domain.service.CareOccurrenceScheduler
import org.mlanau.project.plant.domain.service.CareReminderRequest
import org.mlanau.project.shared.time.SystemTimeZoneProvider
import org.mlanau.project.shared.time.TimeZoneProvider

/**
 * Re-schedules the platform reminder for one rule, or for every active rule of one (plant, type).
 * This — not the platform adapters — is where "when" and "what figure" are decided, because it's
 * the only layer with access to both [CareOccurrenceScheduler] and the logged-care anchor.
 *
 * Always goes through [CareOccurrenceScheduler.nextReminderAt], never `nextOccurrence`: the latter
 * can return an instant at or before "now" (a collapsed overdue occurrence), and scheduling a
 * platform alarm for a past instant fires it immediately — which, the moment its receiver
 * reschedules "next" the same way, would loop forever. `nextReminderAt` is guaranteed strictly
 * later than "now" by construction, so that can't happen here.
 */
class RescheduleCareReminder(
    private val careRepository: CareRepository,
    private val plantRepository: PlantRepository,
    private val scheduler: CareOccurrenceScheduler,
    private val notificationScheduler: CareNotificationScheduler,
    private val clock: Clock = Clock.System,
    private val timeZoneProvider: TimeZoneProvider = SystemTimeZoneProvider
) {
    suspend operator fun invoke(ruleId: CareRuleId) {
        val rule = careRepository.getCareRule(ruleId) ?: return
        reschedule(rule)
    }

    /** Every active rule of this (plant, type) — used when the anchor moved without a specific
     * rule behind it (an ad hoc log, or undoing one), since the anchor is shared across every rule
     * of that (plant, type) rather than owned by a single one. */
    suspend operator fun invoke(plantId: PlantId, type: CareType) {
        careRepository.getCareRules(plantId).first()
            .filter { it.type == type }
            .forEach { reschedule(it) }
    }

    private suspend fun reschedule(rule: CareRule) {
        val ruleId = rule.id ?: return

        if (!rule.active || !rule.notificationsEnabled) {
            notificationScheduler.cancel(ruleId)
            return
        }

        val plant = plantRepository.findById(rule.plantId) ?: return
        val now = clock.now()
        val timeZone = timeZoneProvider()
        val lastCareAt = careRepository.getLastCareByTypeForPlant(rule.plantId).first()[rule.type]

        val at = scheduler.nextReminderAt(rule, lastCareAt, now, timeZone)
        if (at == null) {
            notificationScheduler.cancel(ruleId)
            return
        }

        notificationScheduler.schedule(
            CareReminderRequest(
                ruleId = ruleId,
                plantId = rule.plantId,
                plantName = plant.name,
                type = rule.type,
                at = at,
                daysSinceLastCareAtFireTime = lastCareAt?.daysUntil(at, timeZone)
            )
        )
    }
}

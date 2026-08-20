package org.mlanau.project.plant.application

import kotlin.time.Clock
import kotlin.time.Instant
import org.mlanau.project.plant.domain.model.CareDetails
import org.mlanau.project.plant.domain.model.CareLog
import org.mlanau.project.plant.domain.model.CareLogId
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.model.type
import org.mlanau.project.plant.domain.repository.CareRepository
import org.mlanau.project.plant.domain.service.CareOccurrence

/**
 * Records that a care actually happened — the only way a [CareOccurrence] becomes "done": this
 * creates a [CareLog], it never mutates the occurrence, which stays a prediction and is never
 * persisted at all. Two entry points: completing a specific occurrence the scheduler produced, or
 * logging one ad hoc for a day (present or past) that never had a planned occurrence — both end up
 * validated the same way, by [CareLog.create], which is what actually enforces "not in the future".
 *
 * Either way the anchor for this (plant, type) just moved, so the affected reminder(s) are
 * re-scheduled afterwards: the occurrence path knows exactly which rule produced it, but the ad hoc
 * path doesn't (or the log may not come from a rule at all), so it re-schedules every active rule
 * of that (plant, type) instead — the anchor is shared across all of them.
 */
class LogCare(
    private val careRepository: CareRepository,
    private val rescheduleCareReminder: RescheduleCareReminder,
    private val clock: Clock = Clock.System
) {
    suspend operator fun invoke(
        occurrence: CareOccurrence,
        performedAt: Instant = clock.now(),
        note: String? = null
    ): Result<CareLogId> = runCatchingDomainErrors {
        val log = CareLog.create(
            plantId = occurrence.plantId,
            careRuleId = occurrence.careRuleId,
            details = occurrence.details,
            performedAt = performedAt,
            now = clock.now(),
            scheduledAt = occurrence.scheduledAt,
            note = note
        )
        val savedId = requireNotNull(careRepository.saveCareLog(log).id)
        rescheduleCareReminder(occurrence.careRuleId)
        savedId
    }

    suspend operator fun invoke(
        plantId: PlantId,
        careRuleId: CareRuleId?,
        details: CareDetails,
        performedAt: Instant,
        note: String? = null
    ): Result<CareLogId> = runCatchingDomainErrors {
        val log = CareLog.create(
            plantId = plantId,
            careRuleId = careRuleId,
            details = details,
            performedAt = performedAt,
            now = clock.now(),
            note = note
        )
        val savedId = requireNotNull(careRepository.saveCareLog(log).id)
        rescheduleCareReminder(plantId, details.type)
        savedId
    }
}

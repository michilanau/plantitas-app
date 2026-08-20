package org.mlanau.project.plant.application

import org.mlanau.project.plant.domain.model.CareLogId
import org.mlanau.project.plant.domain.repository.CareRepository

/**
 * Reverses a [LogCare] call: deleting the log is enough for the anchor itself — it's derived as
 * `MAX(performedAt)`, so it recomputes on its own the moment the row is gone. What doesn't recompute
 * on its own is any reminder already scheduled against the old anchor, so this re-schedules every
 * active rule of the log's (plant, type) afterwards — the same breadth [LogCare]'s ad hoc path uses,
 * since a log's own [org.mlanau.project.plant.domain.model.CareLog.careRuleId] may be `null` (the
 * rule that produced it no longer exists) or may not be the only rule of that type.
 */
class UndoCareLog(
    private val careRepository: CareRepository,
    private val rescheduleCareReminder: RescheduleCareReminder
) {
    suspend operator fun invoke(id: CareLogId): Result<Unit> {
        return runCatchingDomainErrors {
            // Read before deleting: once the row is gone, so is the (plantId, type) it belonged to.
            val log = careRepository.getCareLog(id) ?: return@runCatchingDomainErrors
            careRepository.deleteCareLog(id)
            rescheduleCareReminder(log.plantId, log.type)
        }
    }
}

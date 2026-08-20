package org.mlanau.project.plant.application

import kotlin.time.Clock
import org.mlanau.project.plant.domain.repository.CareRepository
import org.mlanau.project.plant.domain.service.CareOccurrence

/**
 * "Not this time" for a scheduled or overdue [CareOccurrence] — moves the rule's `dismissedBefore`
 * forward, which suppresses it without touching the anchor (the actual care rhythm is unaffected;
 * only what was declined is).
 *
 * `maxOf(now, occurrence.scheduledAt)` rather than just `occurrence.scheduledAt` is what makes
 * dismissing a *collapsed* overdue occurrence clear the entire missed range in one tap: if only the
 * occurrence's own (oldest) slot were recorded, the next-oldest missed slot would immediately
 * resurface as overdue on the next read. Dismissing a still-future occurrence is unaffected by the
 * `maxOf` (its `scheduledAt` is already later than `now`).
 */
class DismissCareOccurrence(
    private val careRepository: CareRepository,
    private val rescheduleCareReminder: RescheduleCareReminder,
    private val clock: Clock = Clock.System
) {
    suspend operator fun invoke(occurrence: CareOccurrence): Result<Unit> {
        return runCatchingDomainErrors {
            val now = clock.now()
            careRepository.updateDismissedBefore(occurrence.careRuleId, maxOf(now, occurrence.scheduledAt))
            rescheduleCareReminder(occurrence.careRuleId)
        }
    }
}

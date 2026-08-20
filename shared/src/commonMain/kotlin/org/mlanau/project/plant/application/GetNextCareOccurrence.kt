package org.mlanau.project.plant.application

import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.repository.CareRepository
import org.mlanau.project.plant.domain.service.CareOccurrence
import org.mlanau.project.plant.domain.service.CareOccurrenceScheduler
import org.mlanau.project.plant.domain.service.OccurrenceStatus

/**
 * What a plant most urgently needs right now, across all its care rules: an overdue occurrence
 * beats a merely scheduled one regardless of which is chronologically earlier — a watering that's
 * three days overdue matters more than a fertilizing due in an hour.
 */
class GetNextCareOccurrence(
    private val repository: CareRepository,
    private val scheduler: CareOccurrenceScheduler,
    private val clock: Clock = Clock.System
) {
    operator fun invoke(plantId: PlantId): Flow<CareOccurrence?> {
        return combine(
            repository.getCareRules(plantId),
            repository.getLastCareByTypeForPlant(plantId)
        ) { rules, lastCareByType ->
            val now = clock.now()
            rules
                .mapNotNull { rule -> scheduler.nextOccurrence(rule, lastCareByType[rule.type], now) }
                .minWithOrNull(compareBy({ it.status != OccurrenceStatus.OVERDUE }, { it.scheduledAt }))
        }
    }
}

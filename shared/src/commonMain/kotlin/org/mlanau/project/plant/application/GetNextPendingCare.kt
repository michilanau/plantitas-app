package org.mlanau.project.plant.application

import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.port.CareRuleRepository
import org.mlanau.project.plant.domain.port.CareTaskRepository
import org.mlanau.project.plant.domain.service.CareScheduler

class GetNextPendingCare(
    private val careRuleRepository: CareRuleRepository,
    private val careTaskRepository: CareTaskRepository,
    private val scheduler: CareScheduler,
    private val clock: Clock = Clock.System
) {
    operator fun invoke(plantId: PlantId): Flow<CareTask.Pending?> {
        return combine(
            careRuleRepository.observeByPlant(plantId),
            careTaskRepository.observeLastCareDates(plantId)
        ) { rules, lastCareByType ->
            val now = clock.now()
            rules
                .mapNotNull { rule -> scheduler.nextPending(rule, lastCareByType[rule.type], now) }
                .minWithOrNull(compareBy({ !it.isOverdue }, { it.dueAt }))
        }
    }
}

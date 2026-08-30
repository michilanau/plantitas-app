package org.mlanau.project.plant.application

import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.port.CareRuleRepository
import org.mlanau.project.plant.domain.port.CareTaskRepository
import org.mlanau.project.plant.domain.service.CareScheduler

/**
 * The single most pressing pending care per plant, for the plant list. An overdue task always
 * wins over a merely scheduled one, and among equals the earliest [CareTask.Pending.dueAt] — the
 * same ordering [GetNextPendingCare] applies for one plant.
 */
class GetNextPendingCareForPlants(
    private val careRuleRepository: CareRuleRepository,
    private val careTaskRepository: CareTaskRepository,
    private val scheduler: CareScheduler,
    private val clock: Clock = Clock.System
) {
    operator fun invoke(): Flow<Map<PlantId, CareTask.Pending>> {
        return combine(
            careRuleRepository.observeAll(),
            careTaskRepository.observeLastCareDates()
        ) { rules, lastCareByPlant ->
            val now = clock.now()
            rules
                .mapNotNull { rule ->
                    val plantId = rule.plantId ?: return@mapNotNull null
                    scheduler.nextPending(rule, lastCareByPlant[plantId]?.get(rule.type), now)
                }
                .groupBy { it.plantId }
                .mapValues { (_, tasks) ->
                    tasks.minWith(compareBy({ !it.isOverdue }, { it.dueAt }))
                }
        }
    }
}

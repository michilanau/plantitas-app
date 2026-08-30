package org.mlanau.project.plant.application

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.plant.domain.port.CareRuleRepository
import org.mlanau.project.plant.domain.port.CareTaskRepository
import org.mlanau.project.plant.domain.service.CareScheduler

class GetCalendarTasks(
    private val careRuleRepository: CareRuleRepository,
    private val careTaskRepository: CareTaskRepository,
    private val scheduler: CareScheduler,
    private val clock: Clock = Clock.System
) {
    operator fun invoke(from: Instant, until: Instant): Flow<List<CareTask>> {
        return combine(
            careRuleRepository.observeAll(),
            careTaskRepository.observeLastCareDates(),
            careTaskRepository.observeInRange(from, until)
        ) { rules, lastCareDates, doneInRange ->
            val now = clock.now()
            val pending = scheduler.pendingIn(rules, lastCareDates, from, until, now)
            (pending + doneInRange).sortedBy { it.at }
        }
    }
}

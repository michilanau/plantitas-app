package org.mlanau.project.plant.application

import kotlin.time.Clock
import org.mlanau.project.plant.domain.exception.CareRuleNotFoundException
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.port.CareRuleRepository

class ResumeCareRule(
    private val repository: CareRuleRepository,
    private val clock: Clock = Clock.System
) {
    suspend operator fun invoke(id: CareRuleId): Result<Unit> {
        return runCatchingDomainErrors {
            val rule = repository.findById(id) ?: throw CareRuleNotFoundException()
            repository.save(rule.resumedAt(clock.now()))
        }
    }
}

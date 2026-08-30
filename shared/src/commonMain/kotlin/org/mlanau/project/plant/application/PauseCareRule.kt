package org.mlanau.project.plant.application

import org.mlanau.project.plant.domain.exception.CareRuleNotFoundException
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.port.CareRuleRepository

class PauseCareRule(
    private val repository: CareRuleRepository
) {
    suspend operator fun invoke(id: CareRuleId): Result<Unit> {
        return runCatchingDomainErrors {
            val rule = repository.findById(id) ?: throw CareRuleNotFoundException()
            repository.save(rule.paused())
        }
    }
}

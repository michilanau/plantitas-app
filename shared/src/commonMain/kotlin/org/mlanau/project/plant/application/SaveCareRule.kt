package org.mlanau.project.plant.application

import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.repository.CareRepository

class SaveCareRule(
    private val repository: CareRepository,
    private val rescheduleCareReminder: RescheduleCareReminder
) {
    /**
     * Saves a care rule, optionally overriding the plantId.
     * This handles the case where a new plant is created and its rules
     * need to be assigned the newly generated plantId.
     */
    suspend operator fun invoke(rule: CareRule, plantId: PlantId = rule.plantId): Result<Unit> {
        return runCatchingDomainErrors {
            val ruleWithPlantId = if (rule.plantId == plantId) rule else rule.assignedTo(plantId)
            val savedRuleId = repository.saveCareRule(ruleWithPlantId)
            rescheduleCareReminder(savedRuleId)
        }
    }
}

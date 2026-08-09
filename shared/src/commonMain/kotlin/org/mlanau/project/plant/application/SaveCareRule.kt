package org.mlanau.project.plant.application

import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.FertilizeCareRule
import org.mlanau.project.plant.domain.model.RepotCareRule
import org.mlanau.project.plant.domain.model.WaterCareRule
import org.mlanau.project.plant.domain.repository.CareRepository

class SaveCareRule(
    private val repository: CareRepository
) {
    /**
     * Saves a care rule, optionally overriding the plantId.
     * This handles the case where a new plant is created and its rules
     * need to be assigned the newly generated plantId.
     */
    suspend operator fun invoke(rule: CareRule, plantId: Int = rule.plantId): Result<Unit> {
        return runCatching {
            val ruleWithPlantId = if (rule.plantId == plantId) rule else when (rule) {
                is WaterCareRule -> rule.copy(plantId = plantId)
                is FertilizeCareRule -> rule.copy(plantId = plantId)
                is RepotCareRule -> rule.copy(plantId = plantId)
            }
            repository.saveCareRule(ruleWithPlantId)
        }
    }
}

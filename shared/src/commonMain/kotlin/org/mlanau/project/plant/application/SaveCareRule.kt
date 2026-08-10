package org.mlanau.project.plant.application

import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.FertilizeCareRule
import org.mlanau.project.plant.domain.model.RepotCareRule
import org.mlanau.project.plant.domain.model.WaterCareRule
import org.mlanau.project.plant.domain.repository.CareRepository
import org.mlanau.project.plant.domain.repository.PlantRepository
import org.mlanau.project.shared.notification.NotificationService
import kotlinx.coroutines.flow.first

class SaveCareRule(
    private val repository: CareRepository,
    private val plantRepository: PlantRepository,
    private val notificationService: NotificationService
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
            
            // Clean up pending events so they are re-generated with new rule parameters
            rule.id?.let { repository.deletePendingEventsByRuleId(it) }
            
            // Handle notifications
            val plant = plantRepository.findById(plantId)
            if (plant != null) {
                notificationService.scheduleNextNotification(ruleWithPlantId, plant.name)
            }
            Result.success(Unit)
        }.getOrElse { Result.failure(it) }
    }
}

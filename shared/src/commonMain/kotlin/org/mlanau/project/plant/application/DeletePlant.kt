package org.mlanau.project.plant.application

import kotlinx.coroutines.flow.first
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.repository.CareRepository
import org.mlanau.project.plant.domain.repository.PlantRepository
import org.mlanau.project.plant.domain.service.CareNotificationScheduler
import org.mlanau.project.plant.domain.service.ImageStorage

class DeletePlant(
    private val repository: PlantRepository,
    private val careRepository: CareRepository,
    private val careNotificationScheduler: CareNotificationScheduler,
    private val imageStorage: ImageStorage
) {
    suspend operator fun invoke(id: PlantId): Result<Unit> {
        return runCatchingDomainErrors {
            // Fetched before deleting: once the plant's row is gone its photo path and its rules'
            // ids are too, and a still-scheduled alarm has no way to know the plant it belonged to
            // was removed.
            val imageUrl = repository.findById(id)?.imageUrl
            val ruleIds = careRepository.getCareRules(id).first().mapNotNull { it.id }

            repository.delete(id)

            imageUrl?.let { imageStorage.delete(it) }
            ruleIds.forEach { careNotificationScheduler.cancel(it) }
        }
    }
}

package org.mlanau.project.plant.application

import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.port.ImageStorage
import org.mlanau.project.plant.domain.port.PlantRepository

class DeletePlant(
    private val repository: PlantRepository,
    private val imageStorage: ImageStorage
) {
    suspend operator fun invoke(id: PlantId): Result<Unit> {
        return runCatchingDomainErrors {
            // Fetched before deleting: once the plant's row is gone so is its photo path. Its
            // rules and tasks fall by ON DELETE CASCADE; CareReminderSync notices them missing from
            // the next reconciliation and cancels their alarms on its own.
            val imageUrl = repository.findById(id)?.imageUrl

            repository.delete(id)

            imageUrl?.let { imageStorage.delete(it) }
        }
    }
}

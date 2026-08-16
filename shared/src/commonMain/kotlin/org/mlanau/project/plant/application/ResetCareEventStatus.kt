package org.mlanau.project.plant.application

import org.mlanau.project.plant.domain.model.*
import org.mlanau.project.plant.domain.repository.CareRepository

class ResetCareEventStatus(
    private val careRepository: CareRepository
) {
    suspend operator fun invoke(event: CareEvent): Result<Unit> {
        return runCatching {
            if (event.id != null) {
                careRepository.updateEventStatus(event.id!!, CareEventStatus.PENDING, null)
            } else {
                val materializedEvent = when (event) {
                    is WaterCareEvent -> event.copy(status = CareEventStatus.PENDING, completedAt = null)
                    is FertilizeCareEvent -> event.copy(status = CareEventStatus.PENDING, completedAt = null)
                    is RepotCareEvent -> event.copy(status = CareEventStatus.PENDING, completedAt = null)
                }
                careRepository.saveCareEvent(materializedEvent)
            }
            Result.success(Unit)
        }.getOrElse { Result.failure(it) }
    }
}

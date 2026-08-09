package org.mlanau.project.plant.application

import kotlin.time.Clock
import org.mlanau.project.plant.domain.model.*
import org.mlanau.project.plant.domain.repository.CareRepository

class ToggleCareEventStatus(
    private val careRepository: CareRepository
) {
    suspend operator fun invoke(event: CareEvent): Result<Unit> {
        return runCatching {
            val newStatus = if (event.status == CareEventStatus.DONE) CareEventStatus.PENDING else CareEventStatus.DONE
            val completedAt = if (newStatus == CareEventStatus.DONE) Clock.System.now() else null

            if (event.id != null) {
                careRepository.updateEventStatus(event.id!!, newStatus, completedAt)
            } else {
                val materializedEvent = when (event) {
                    is WaterCareEvent -> event.copy(status = newStatus, completedAt = completedAt)
                    is FertilizeCareEvent -> event.copy(status = newStatus, completedAt = completedAt)
                    is RepotCareEvent -> event.copy(status = newStatus, completedAt = completedAt)
                }
                careRepository.saveCareEvent(materializedEvent)
            }
        }
    }
}

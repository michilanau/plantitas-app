package org.mlanau.project.plant.application

import kotlinx.coroutines.flow.first
import org.mlanau.project.plant.domain.model.*
import org.mlanau.project.plant.domain.repository.CareRepository
import org.mlanau.project.plant.domain.repository.PlantRepository
import org.mlanau.project.shared.notification.NotificationService

class SkipCareEvent(
    private val careRepository: CareRepository,
    private val plantRepository: PlantRepository,
    private val notificationService: NotificationService
) {
    suspend operator fun invoke(event: CareEvent): Result<Unit> {
        return runCatching {
            val newStatus = CareEventStatus.SKIPPED
            
            if (event.id != null) {
                careRepository.updateEventStatus(event.id!!, newStatus, null)
            } else {
                val materializedEvent = when (event) {
                    is WaterCareEvent -> event.copy(status = newStatus, completedAt = null)
                    is FertilizeCareEvent -> event.copy(status = newStatus, completedAt = null)
                    is RepotCareEvent -> event.copy(status = newStatus, completedAt = null)
                }
                careRepository.saveCareEvent(materializedEvent)
            }

            // Reschedule notification to the next available event
            val rules = careRepository.getCareRules(event.plantId).first()
            val rule = rules.find { it.id == event.careRuleId }
            val plant = plantRepository.findById(event.plantId)
            
            if (rule != null && plant != null) {
                notificationService.scheduleNextNotification(rule, plant.name)
            }
            Result.success(Unit)
        }.getOrElse { Result.failure(it) }
    }
}

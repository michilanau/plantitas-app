package org.mlanau.project.plant.application

import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import org.mlanau.project.plant.domain.model.*
import org.mlanau.project.plant.domain.repository.CareRepository
import org.mlanau.project.plant.domain.repository.PlantRepository
import org.mlanau.project.shared.notification.NotificationService

class RescheduleCareEvent(
    private val careRepository: CareRepository,
    private val plantRepository: PlantRepository,
    private val notificationService: NotificationService
) {
    suspend operator fun invoke(event: CareEvent, newDate: Instant): Result<Unit> {
        return runCatching {
            val updatedEvent = when (event) {
                is WaterCareEvent -> event.copy(scheduledAt = newDate)
                is FertilizeCareEvent -> event.copy(scheduledAt = newDate)
                is RepotCareEvent -> event.copy(scheduledAt = newDate)
            }

            careRepository.saveCareEvent(updatedEvent)

            // Reschedule notification
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

package org.mlanau.project.plant.application

import kotlin.time.Clock
import kotlinx.datetime.*
import org.mlanau.project.plant.domain.model.*
import org.mlanau.project.plant.domain.repository.CareRepository
import org.mlanau.project.plant.domain.repository.PlantRepository
import org.mlanau.project.shared.notification.NotificationService
import kotlinx.coroutines.flow.first

class ToggleCareEventStatus(
    private val careRepository: CareRepository,
    private val plantRepository: PlantRepository,
    private val notificationService: NotificationService
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

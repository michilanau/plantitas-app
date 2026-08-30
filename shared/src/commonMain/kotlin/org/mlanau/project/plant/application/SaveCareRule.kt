package org.mlanau.project.plant.application

import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.port.CareRuleRepository

class SaveCareRule(
    private val repository: CareRuleRepository
) {
    suspend operator fun invoke(rule: CareRule, plantId: PlantId): Result<Unit> {
        return runCatchingDomainErrors {
            val target = if (rule.plantId == plantId) rule else rule.assignedTo(plantId)
            // A plant has at most one rule per care type, so saving a rule for a type that is
            // already taken edits the existing one rather than inserting a second. The UNIQUE
            // index would reject the insert anyway; failing a save the user explicitly asked for
            // is worse than applying it to the rule they can only have meant.
            val existing = repository.findByPlantAndType(plantId, target.type)
            val toSave = if (existing == null || existing.id == target.id) {
                target
            } else {
                existing.withSchedule(
                    everyDays = target.everyDays,
                    startDate = target.startDate,
                    notificationTime = target.notificationTime,
                    notificationsEnabled = target.notificationsEnabled,
                    details = target.details
                )
            }
            // CareReminderSync picks up the saved rule on its next reconciliation.
            repository.save(toSave)
        }
    }
}

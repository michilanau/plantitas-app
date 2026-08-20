package org.mlanau.project.plant.application

import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.repository.CareRepository
import org.mlanau.project.plant.domain.service.CareNotificationScheduler

class DeleteCareRule(
    private val repository: CareRepository,
    private val careNotificationScheduler: CareNotificationScheduler
) {
    suspend operator fun invoke(id: CareRuleId): Result<Unit> {
        return runCatchingDomainErrors {
            repository.deleteCareRule(id)
            // Otherwise a rule deleted from the UI would keep alarming for a task that no longer
            // exists, since AlarmManager/UNUserNotificationCenter have no idea the rule was removed.
            careNotificationScheduler.cancel(id)
        }
    }
}

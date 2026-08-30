package org.mlanau.project.plant.application

import org.mlanau.project.notification.domain.port.NotificationScheduler
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.port.CareRuleRepository

class DeleteCareRule(
    private val repository: CareRuleRepository,
    private val notifications: NotificationScheduler
) {
    suspend operator fun invoke(id: CareRuleId): Result<Unit> {
        return runCatchingDomainErrors {
            repository.delete(id)
            // CareReminderSync would notice the rule missing on its next reconciliation, but only
            // while it is subscribed. Cancelling here means a deleted rule's alarm is gone the
            // moment it is deleted, whoever is listening.
            notifications.cancel(careReminderNotificationId(id))
        }
    }
}

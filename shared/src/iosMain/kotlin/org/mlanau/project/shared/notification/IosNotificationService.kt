package org.mlanau.project.shared.notification

import org.mlanau.project.plant.domain.model.CareRule

class IosNotificationService : NotificationService {
    override fun scheduleNextNotification(rule: CareRule, plantName: String) {
        // TODO: Implement iOS notifications
    }

    override fun cancelNotifications(rule: CareRule) {
        // TODO: Implement iOS notifications
    }
}

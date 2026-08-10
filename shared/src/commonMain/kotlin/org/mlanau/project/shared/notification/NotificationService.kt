package org.mlanau.project.shared.notification

import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.CareEvent

interface NotificationService {
    /**
     * Schedules a notification for the next occurrence of a care rule.
     */
    fun scheduleNextNotification(rule: CareRule, plantName: String)

    /**
     * Cancels any pending notifications for a care rule.
     */
    fun cancelNotifications(rule: CareRule)
}

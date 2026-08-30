package org.mlanau.project.notification.domain.port

interface NotificationScheduler {
    suspend fun schedule(notification: ScheduledNotification)

    fun cancel(id: NotificationId)
}

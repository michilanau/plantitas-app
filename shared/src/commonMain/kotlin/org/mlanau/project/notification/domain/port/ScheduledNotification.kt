package org.mlanau.project.notification.domain.port

import kotlin.time.Instant

data class ScheduledNotification(
    val id: NotificationId,
    val at: Instant,
    val title: String,
    val body: String
)

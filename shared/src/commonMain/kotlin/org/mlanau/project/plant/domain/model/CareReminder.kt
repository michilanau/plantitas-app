package org.mlanau.project.plant.domain.model

import kotlin.time.Instant

/**
 * One link of a rule's reminder chain: the task exactly as it will stand — due, or overdue and by
 * how much — when the platform shows this reminder at [at].
 */
data class CareReminder(
    val task: CareTask.Pending,
    val at: Instant
)

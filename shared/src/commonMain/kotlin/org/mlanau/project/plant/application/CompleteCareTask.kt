package org.mlanau.project.plant.application

import kotlin.time.Clock
import kotlin.time.Instant
import org.mlanau.project.plant.domain.model.CareDetails
import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.plant.domain.model.CareTaskId
import org.mlanau.project.plant.domain.port.CareTaskRepository
import org.mlanau.project.shared.time.SystemTimeZoneProvider
import org.mlanau.project.shared.time.TimeZoneProvider

class CompleteCareTask(
    private val careTaskRepository: CareTaskRepository,
    private val clock: Clock = Clock.System,
    private val timeZoneProvider: TimeZoneProvider = SystemTimeZoneProvider
) {
    suspend operator fun invoke(
        pending: CareTask.Pending,
        performedAt: Instant = clock.now(),
        care: CareDetails = pending.care,
        note: String? = null
    ): Result<CareTaskId> = runCatchingDomainErrors {
        val now = clock.now()
        val done = pending.complete(
            performedAt = performedAt,
            now = now,
            timeZone = timeZoneProvider(),
            care = care,
            note = note
        )
        val saved = careTaskRepository.save(done)
        requireNotNull(saved.id)
    }
}

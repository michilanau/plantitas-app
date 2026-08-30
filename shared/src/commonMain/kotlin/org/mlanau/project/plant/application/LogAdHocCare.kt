package org.mlanau.project.plant.application

import kotlin.time.Clock
import kotlin.time.Instant
import org.mlanau.project.plant.domain.exception.MissingPersistedIdException
import org.mlanau.project.plant.domain.model.CareDetails
import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.plant.domain.model.CareTaskId
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.port.CareTaskRepository

class LogAdHocCare(
    private val careTaskRepository: CareTaskRepository,
    private val clock: Clock = Clock.System
) {
    suspend operator fun invoke(
        plantId: PlantId,
        care: CareDetails,
        performedAt: Instant,
        note: String? = null
    ): Result<CareTaskId> = runCatchingDomainErrors {
        val done = CareTask.Done.create(
            plantId = plantId,
            careRuleId = null,
            care = care,
            performedAt = performedAt,
            now = clock.now(),
            note = note
        )
        val saved = careTaskRepository.save(done)
        saved.id ?: throw MissingPersistedIdException()
    }
}

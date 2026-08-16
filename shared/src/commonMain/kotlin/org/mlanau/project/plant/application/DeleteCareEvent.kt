package org.mlanau.project.plant.application

import org.mlanau.project.plant.domain.repository.CareRepository

class DeleteCareEvent(
    private val careRepository: CareRepository
) {
    suspend operator fun invoke(eventId: Int): Result<Unit> {
        return runCatching {
            careRepository.deleteCareEvent(eventId)
            Result.success(Unit)
        }.getOrElse { Result.failure(it) }
    }
}

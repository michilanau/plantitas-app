package org.mlanau.project.plant.application

import org.mlanau.project.plant.domain.model.CareTaskId
import org.mlanau.project.plant.domain.port.CareTaskRepository

class DeleteCareTask(
    private val careTaskRepository: CareTaskRepository
) {
    suspend operator fun invoke(id: CareTaskId): Result<Unit> {
        return runCatchingDomainErrors {
            careTaskRepository.delete(id)
        }
    }
}

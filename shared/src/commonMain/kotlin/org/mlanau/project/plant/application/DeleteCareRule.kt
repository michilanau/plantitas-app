package org.mlanau.project.plant.application

import org.mlanau.project.plant.domain.repository.CareRepository

class DeleteCareRule(
    private val repository: CareRepository
) {
    suspend operator fun invoke(id: Int): Result<Unit> {
        return runCatching {
            repository.deleteCareRule(id)
        }
    }
}

package org.mlanau.project.plant.application

import kotlinx.coroutines.flow.Flow
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.repository.CareRepository

class GetCareRules(
    private val repository: CareRepository
) {
    operator fun invoke(plantId: Int): Flow<List<CareRule>> {
        return repository.getCareRules(plantId)
    }
}

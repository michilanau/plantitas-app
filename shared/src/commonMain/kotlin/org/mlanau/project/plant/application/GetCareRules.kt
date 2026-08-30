package org.mlanau.project.plant.application

import kotlinx.coroutines.flow.Flow
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.port.CareRuleRepository

class GetCareRules(
    private val repository: CareRuleRepository
) {
    operator fun invoke(plantId: PlantId): Flow<List<CareRule>> {
        return repository.observeByPlant(plantId)
    }
}

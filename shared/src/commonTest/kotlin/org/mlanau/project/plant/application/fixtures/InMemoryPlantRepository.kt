package org.mlanau.project.plant.application.fixtures

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.repository.PlantRepository

/** In-memory [PlantRepository] for application-layer tests, so use cases can be exercised without
 * a real database. */
class InMemoryPlantRepository : PlantRepository {
    private val plants = MutableStateFlow<List<Plant>>(emptyList())
    private var nextId = 1

    override fun findAll(): Flow<List<Plant>> = plants

    override suspend fun findById(id: PlantId): Plant? = plants.value.find { it.id == id }

    override suspend fun save(plant: Plant): PlantId {
        val id = plant.id ?: PlantId(nextId++)
        val saved = plant.copy(id = id)
        plants.update { list -> list.filterNot { it.id == id } + saved }
        return id
    }

    override suspend fun delete(id: PlantId) {
        plants.update { list -> list.filterNot { it.id == id } }
    }
}

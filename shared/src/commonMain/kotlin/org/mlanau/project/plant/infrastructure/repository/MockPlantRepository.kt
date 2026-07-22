package org.mlanau.project.plant.infrastructure.repository

import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.repository.PlantRepository

class MockPlantRepository : PlantRepository {
    private val plants = mutableListOf(
        Plant(1, "Monstera Deliciosa", "Planta tropical con hojas agujereadas."),
        Plant(2, "Cactus de Asiento de Suegra", "Cactus globoso con espinas amarillas."),
        Plant(3, "Poto", "Planta trepadora muy resistente y fácil de cuidar."),
        Plant(4, "Lengua de suegra", "Ideal para purificar el aire y muy resistente.")
    )

    override suspend fun findAll(): List<Plant> {
        return plants.toList()
    }

    override suspend fun save(plant: Plant) {
        val nextId = (plants.maxOfOrNull { it.id ?: 0 } ?: 0) + 1
        val plantWithId = if (plant.id == null) plant.copy(id = nextId) else plant
        plants.add(plantWithId)
    }
}

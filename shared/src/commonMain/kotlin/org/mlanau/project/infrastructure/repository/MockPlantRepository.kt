package org.mlanau.project.infrastructure.repository

import org.mlanau.project.domain.model.Plant
import org.mlanau.project.domain.repository.PlantRepository

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
        val newPlant = plant.copy(id = (plants.maxOfOrNull { it.id } ?: 0) + 1)
        plants.add(newPlant)
    }
}

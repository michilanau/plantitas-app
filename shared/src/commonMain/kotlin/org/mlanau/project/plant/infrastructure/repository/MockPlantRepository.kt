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
        if (plant.id != null) {
            // Lógica de Update
            val index = plants.indexOfFirst { it.id == plant.id }
            if (index != -1) {
                plants[index] = plant
            } else {
                plants.add(plant)
            }
        } else {
            // Lógica de Insert
            val nextId = (plants.maxOfOrNull { it.id ?: 0 } ?: 0) + 1
            plants.add(plant.copy(id = nextId))
        }
    }
}

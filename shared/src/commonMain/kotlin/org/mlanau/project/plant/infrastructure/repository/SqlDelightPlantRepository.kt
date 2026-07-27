package org.mlanau.project.plant.infrastructure.repository

import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.LightNeed
import org.mlanau.project.plant.domain.model.PotSize
import org.mlanau.project.plant.domain.repository.PlantRepository
import org.mlanau.project.plant.infrastructure.persistence.PlantDb
import org.mlanau.project.plant.infrastructure.persistence.PlantEntity

class SqlDelightPlantRepository(database: PlantDb) : PlantRepository {
    private val queries = database.plantDbQueries

    override suspend fun findAll(): List<Plant> {
        return queries.selectAll().executeAsList().map { it.toDomain() }
    }

    override suspend fun save(plant: Plant) {
        if (plant.id != null) {
            queries.updatePlant(
                name = plant.name,
                description = plant.description,
                location = plant.location,
                lightNeed = plant.lightNeed?.name,
                potSize = plant.potSize?.name,
                id = plant.id.toLong()
            )
        } else {
            queries.insertPlant(
                name = plant.name,
                description = plant.description,
                location = plant.location,
                lightNeed = plant.lightNeed?.name,
                potSize = plant.potSize?.name
            )
        }
    }

    override suspend fun delete(id: Int) {
        queries.deletePlant(id.toLong())
    }

    private fun PlantEntity.toDomain(): Plant {
        return Plant(
            id = id.toInt(),
            name = name,
            description = description,
            location = location,
            lightNeed = lightNeed?.let { LightNeed.valueOf(it) },
            potSize = potSize?.let { PotSize.valueOf(it) }
        )
    }
}

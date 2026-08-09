package org.mlanau.project.plant.infrastructure.persistence

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.LightNeed
import org.mlanau.project.plant.domain.model.PotSize
import org.mlanau.project.plant.domain.repository.PlantRepository
import kotlin.time.Instant

class SqlDelightPlantRepository(database: PlantDb) : PlantRepository {
    private val queries = database.plantDbQueries

    override fun findAll(): Flow<List<Plant>> {
        return queries.selectAll().asFlow().mapToList(Dispatchers.IO).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun findById(id: Int): Plant? {
        return queries.selectPlantById(id.toLong()).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun save(plant: Plant): Int {
        val createdAtIso = plant.createdAt.toString()
        if (plant.id != null) {
            queries.updatePlant(
                name = plant.name,
                description = plant.description,
                location = plant.location,
                lightNeed = plant.lightNeed?.name,
                potSize = plant.potSize?.name,
                imageUrl = plant.imageUrl,
                createdAt = createdAtIso,
                id = plant.id.toLong()
            )
            return plant.id
        } else {
            queries.insertPlant(
                name = plant.name,
                description = plant.description,
                location = plant.location,
                lightNeed = plant.lightNeed?.name,
                potSize = plant.potSize?.name,
                imageUrl = plant.imageUrl,
                createdAt = createdAtIso
            )
            return queries.lastInsertId().executeAsOne().toInt()
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
            potSize = potSize?.let { PotSize.valueOf(it) },
            imageUrl = imageUrl,
            createdAt = Instant.parse(createdAt)
        )
    }
}

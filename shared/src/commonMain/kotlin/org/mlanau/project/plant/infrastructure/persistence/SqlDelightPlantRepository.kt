package org.mlanau.project.plant.infrastructure.persistence

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mlanau.project.plant.domain.model.LightNeed
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.model.PotSize
import org.mlanau.project.plant.domain.port.PlantRepository

class SqlDelightPlantRepository(database: PlantDb) : PlantRepository {
    private val queries = database.plantQueries

    override fun observeAll(): Flow<List<Plant>> {
        return queries.selectAll().asFlow().mapToList(Dispatchers.IO).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun findById(id: PlantId): Plant? {
        return queries.selectPlantById(id.value.toLong()).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun save(plant: Plant): Plant {
        val createdAtIso = plant.createdAt.toDbString()
        val id = plant.id
        if (id != null) {
            queries.updatePlant(
                name = plant.name,
                description = plant.description,
                location = plant.location,
                lightNeed = plant.lightNeed?.name,
                potSize = plant.potSize?.name,
                imageUrl = plant.imageUrl,
                createdAt = createdAtIso,
                id = id.value.toLong()
            )
            return plant
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
            return plant.withId(PlantId(queries.lastInsertId().executeAsOne().toInt()))
        }
    }

    override suspend fun delete(id: PlantId) {
        queries.deletePlant(id.value.toLong())
    }

    private fun PlantEntity.toDomain(): Plant {
        return Plant.restore(
            id = PlantId(id.toInt()),
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

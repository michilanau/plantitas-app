package org.mlanau.project.plant.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant
import org.mlanau.project.plant.domain.exception.EmptyPlantNameException
import org.mlanau.project.plant.domain.exception.InvalidIdException

class PlantTest {

    private val createdAt = Instant.parse("2026-01-10T00:00:00Z")

    @Test
    fun `create rejects a blank name`() {
        assertFailsWith<EmptyPlantNameException> {
            Plant.create(name = "   ", description = null, createdAt = createdAt)
        }
    }

    @Test
    fun `create trims the name`() {
        val plant = Plant.create(name = "  Ficus  ", description = null, createdAt = createdAt)
        assertEquals("Ficus", plant.name)
    }

    @Test
    fun `create normalizes a blank description to null`() {
        val plant = Plant.create(name = "Ficus", description = "   ", createdAt = createdAt)
        assertEquals(null, plant.description)
    }

    @Test
    fun `create builds a new plant with no id`() {
        val plant = Plant.create(name = "Ficus", description = null, createdAt = createdAt)
        assertEquals(null, plant.id)
    }

    @Test
    fun `restore rejects a blank name too`() {
        assertFailsWith<EmptyPlantNameException> {
            Plant.restore(
                id = PlantId(1),
                name = "   ",
                description = null,
                location = null,
                lightNeed = null,
                potSize = null,
                imageUrl = null,
                createdAt = createdAt
            )
        }
    }

    @Test
    fun `PlantId rejects a non-positive value`() {
        assertFailsWith<InvalidIdException> { PlantId(0) }
        assertFailsWith<InvalidIdException> { PlantId(-1) }
    }
}

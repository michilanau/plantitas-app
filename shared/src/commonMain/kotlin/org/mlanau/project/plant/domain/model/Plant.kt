package org.mlanau.project.plant.domain.model

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.*
import org.mlanau.project.plant.domain.exceptions.EmptyPlantNameException

data class Plant(
    val id: Int? = null,
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val lightNeed: LightNeed? = null,
    val potSize: PotSize? = null,
    val createdAt: Instant = Clock.System.now()
) {
    init {
        if (name.isBlank()) {
            throw EmptyPlantNameException()
        }
    }
}

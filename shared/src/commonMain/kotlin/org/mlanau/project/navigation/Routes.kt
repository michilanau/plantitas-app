package org.mlanau.project.navigation

import kotlinx.serialization.Serializable

/**
 * Navigation Compose destinations. Each route carries only identifiers, never a domain entity
 * (e.g. [PlantDetail] carries a plant id, not a [org.mlanau.project.plant.domain.model.Plant]):
 * the destination that owns that data loads it itself, the same way a deep link would have to.
 */
@Serializable
data object Home

@Serializable
data object Calendar

@Serializable
data class PlantDetail(val plantId: Int)

/** `plantId == null` means "create a new plant". */
@Serializable
data class PlantForm(val plantId: Int? = null)

@Serializable
data object Settings

@Serializable
data object About

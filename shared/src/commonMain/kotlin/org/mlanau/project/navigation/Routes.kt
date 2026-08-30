package org.mlanau.project.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Home

@Serializable
data object Calendar

@Serializable
data class PlantDetail(val plantId: Int)

@Serializable
data class PlantForm(val plantId: Int? = null)

@Serializable
data object Settings

@Serializable
data object About

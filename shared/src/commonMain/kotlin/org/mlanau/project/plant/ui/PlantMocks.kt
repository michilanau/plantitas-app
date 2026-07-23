package org.mlanau.project.plant.ui

import org.mlanau.project.plant.application.HomeUiState
import org.mlanau.project.plant.domain.model.Plant

object PlantMocks {
    val samplePlants = listOf(
        Plant(1, "Monstera Deliciosa", "Planta tropical con hojas agujereadas."),
        Plant(2, "Cactus de Asiento de Suegra", "Cactus globoso con espinas amarillas."),
        Plant(3, "Poto", "Planta trepadora muy resistente."),
        Plant(4, "Lengua de suegra", "Ideal para purificar el aire.")
    )

    val homeUiStateSuccess = HomeUiState(
        plants = samplePlants,
        isLoading = false
    )

    val homeUiStateLoading = HomeUiState(
        isLoading = true
    )
}

package org.mlanau.project.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mlanau.project.plant.application.HomeViewModel
import org.mlanau.project.plant.application.FindAllPlants
import org.mlanau.project.plant.application.SavePlant
import org.mlanau.project.plant.domain.repository.PlantRepository
import org.mlanau.project.plant.infrastructure.repository.MockPlantRepository

val appModule = module {
    singleOf(::MockPlantRepository) bind PlantRepository::class
    factoryOf(::FindAllPlants)
    factoryOf(::SavePlant)
    viewModel { HomeViewModel(get(), get()) }
}

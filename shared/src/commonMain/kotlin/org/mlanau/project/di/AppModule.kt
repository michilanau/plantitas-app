package org.mlanau.project.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mlanau.project.application.HomeViewModel
import org.mlanau.project.application.FindAllPlants
import org.mlanau.project.application.SavePlant
import org.mlanau.project.domain.repository.PlantRepository
import org.mlanau.project.infrastructure.repository.MockPlantRepository

val appModule = module {
    singleOf(::MockPlantRepository) bind PlantRepository::class
    factoryOf(::FindAllPlants)
    factoryOf(::SavePlant)
    viewModel { HomeViewModel(get(), get()) }
}

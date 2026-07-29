package org.mlanau.project.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mlanau.project.plant.application.DeletePlant
import org.mlanau.project.plant.application.FindAllPlants
import org.mlanau.project.plant.application.SavePlant
import org.mlanau.project.plant.domain.repository.PlantRepository
import org.mlanau.project.plant.infrastructure.persistence.PlantDb
import org.mlanau.project.plant.infrastructure.persistence.SqlDelightPlantRepository
import org.mlanau.project.plant.presentation.home.HomeViewModel
import org.mlanau.project.shared.database.DatabaseDriverFactory

val plantModule = module {
    single { 
        val driver = get<DatabaseDriverFactory>().createDriver()
        PlantDb(driver)
    }

    singleOf(::SqlDelightPlantRepository) bind PlantRepository::class
    factoryOf(::FindAllPlants)
    factoryOf(::SavePlant)
    factoryOf(::DeletePlant)
    viewModelOf(::HomeViewModel)
}

package org.mlanau.project.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import com.russhwolf.settings.Settings
import com.russhwolf.settings.ObservableSettings
import org.mlanau.project.plant.application.HomeViewModel
import org.mlanau.project.plant.application.FindAllPlants
import org.mlanau.project.plant.application.SavePlant
import org.mlanau.project.plant.application.DeletePlant
import org.mlanau.project.plant.domain.repository.PlantRepository
import org.mlanau.project.plant.infrastructure.repository.SqlDelightPlantRepository
import org.mlanau.project.plant.infrastructure.persistence.DatabaseDriverFactory
import org.mlanau.project.plant.infrastructure.persistence.PlantDb
import org.mlanau.project.settings.application.SettingsViewModel
import org.mlanau.project.settings.domain.repository.SettingsRepository
import org.mlanau.project.settings.infrastructure.repository.PersistentSettingsRepository

val appModule = module {
    single { Settings() as ObservableSettings }
    singleOf(::PersistentSettingsRepository) bind SettingsRepository::class
    
    single { 
        val driver = get<DatabaseDriverFactory>().createDriver()
        PlantDb(driver)
    }

    singleOf(::SqlDelightPlantRepository) bind PlantRepository::class
    factoryOf(::FindAllPlants)
    factoryOf(::SavePlant)
    factoryOf(::DeletePlant)
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get()) }
}

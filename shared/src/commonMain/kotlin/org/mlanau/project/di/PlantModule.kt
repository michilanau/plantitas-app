package org.mlanau.project.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mlanau.project.plant.application.DeletePlant
import org.mlanau.project.plant.application.FindAllPlants
import org.mlanau.project.plant.application.FindPlantById
import org.mlanau.project.plant.application.CreatePlant
import org.mlanau.project.plant.application.DeleteCareRule
import org.mlanau.project.plant.application.GenerateCareEvents
import org.mlanau.project.plant.application.GetCalendarEvents
import org.mlanau.project.plant.application.GetCareRules
import org.mlanau.project.plant.application.GetNextCareEvent
import org.mlanau.project.plant.application.SaveCareRule
import org.mlanau.project.plant.application.ToggleCareEventStatus
import org.mlanau.project.plant.application.UpdatePlant
import org.mlanau.project.plant.domain.repository.CareRepository
import org.mlanau.project.plant.domain.repository.PlantRepository
import org.mlanau.project.plant.infrastructure.persistence.PlantDb
import org.mlanau.project.plant.infrastructure.persistence.SqlDelightCareRepository
import org.mlanau.project.plant.infrastructure.persistence.SqlDelightPlantRepository
import org.mlanau.project.plant.presentation.home.HomeViewModel
import org.mlanau.project.plant.presentation.form.PlantFormViewModel
import org.mlanau.project.plant.presentation.calendar.CalendarViewModel
import org.mlanau.project.plant.presentation.detail.PlantDetailViewModel
import org.mlanau.project.shared.database.DatabaseDriverFactory

val plantModule = module {
    single {
        val driver = get<DatabaseDriverFactory>().createDriver()
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
        PlantDb(driver)
    }

    singleOf(::SqlDelightPlantRepository) bind PlantRepository::class
    singleOf(::SqlDelightCareRepository) bind CareRepository::class

    // Plant use cases
    factoryOf(::FindAllPlants)
    factoryOf(::FindPlantById)
    factoryOf(::CreatePlant)
    factoryOf(::UpdatePlant)
    factoryOf(::DeletePlant)

    // Care use cases
    factoryOf(::GetCareRules)
    factoryOf(::SaveCareRule)
    factoryOf(::DeleteCareRule)
    factoryOf(::GenerateCareEvents)
    factoryOf(::GetCalendarEvents)
    factoryOf(::GetNextCareEvent)
    factoryOf(::ToggleCareEventStatus)

    // ViewModels
    viewModelOf(::HomeViewModel)
    viewModelOf(::PlantFormViewModel)
    viewModelOf(::CalendarViewModel)
    viewModelOf(::PlantDetailViewModel)
}

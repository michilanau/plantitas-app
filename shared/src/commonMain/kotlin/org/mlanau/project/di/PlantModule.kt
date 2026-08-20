package org.mlanau.project.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mlanau.project.plant.application.DeletePlant
import org.mlanau.project.plant.application.DismissCareOccurrence
import org.mlanau.project.plant.application.FindAllPlants
import org.mlanau.project.plant.application.FindPlantById
import org.mlanau.project.plant.application.CreatePlant
import org.mlanau.project.plant.application.DeleteCareRule
import org.mlanau.project.plant.application.GetCalendarEntries
import org.mlanau.project.plant.application.GetCareRules
import org.mlanau.project.plant.application.GetNextCareOccurrence
import org.mlanau.project.plant.application.GetPlantCareHistory
import org.mlanau.project.plant.application.LogCare
import org.mlanau.project.plant.application.MigrateBase64PlantImages
import org.mlanau.project.plant.application.RescheduleAllCareReminders
import org.mlanau.project.plant.application.RescheduleCareReminder
import org.mlanau.project.plant.application.SaveCareRule
import org.mlanau.project.plant.application.UndoCareLog
import org.mlanau.project.plant.application.UpdatePlant
import org.mlanau.project.plant.domain.repository.CareRepository
import org.mlanau.project.plant.domain.repository.PlantRepository
import org.mlanau.project.plant.domain.service.CareOccurrenceScheduler
import org.mlanau.project.plant.infrastructure.persistence.PlantDb
import org.mlanau.project.plant.infrastructure.persistence.SqlDelightCareRepository
import org.mlanau.project.plant.infrastructure.persistence.SqlDelightPlantRepository
import org.mlanau.project.plant.presentation.home.HomeViewModel
import org.mlanau.project.plant.presentation.form.PlantFormViewModel
import org.mlanau.project.plant.presentation.calendar.CalendarViewModel
import org.mlanau.project.plant.presentation.detail.PlantDetailViewModel
import org.mlanau.project.shared.database.DatabaseDriverFactory

val plantModule = module {
    // Foreign key enforcement is configured per-platform inside each DatabaseDriverFactory
    // (AndroidSqliteDriver.Callback.onOpen / NativeSqliteDriver's onConfiguration), since both
    // drivers pool multiple connections and a single `driver.execute(...)` here would only reach
    // one of them.
    single { PlantDb(get<DatabaseDriverFactory>().createDriver()) }

    singleOf(::SqlDelightPlantRepository) bind PlantRepository::class
    singleOf(::SqlDelightCareRepository) bind CareRepository::class

    // Domain services
    singleOf(::CareOccurrenceScheduler)

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
    factoryOf(::MigrateBase64PlantImages)
    factoryOf(::LogCare)
    factoryOf(::UndoCareLog)
    factoryOf(::DismissCareOccurrence)
    factoryOf(::GetPlantCareHistory)
    factoryOf(::GetCalendarEntries)
    factoryOf(::GetNextCareOccurrence)
    factoryOf(::RescheduleCareReminder)
    factoryOf(::RescheduleAllCareReminders)

    // ViewModels
    viewModelOf(::HomeViewModel)
    viewModelOf(::PlantFormViewModel)
    viewModelOf(::CalendarViewModel)
    viewModelOf(::PlantDetailViewModel)
}

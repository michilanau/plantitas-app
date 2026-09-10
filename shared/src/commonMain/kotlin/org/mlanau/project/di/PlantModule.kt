package org.mlanau.project.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mlanau.project.plant.application.CareReminderSync
import org.mlanau.project.plant.application.CompleteCareTask
import org.mlanau.project.plant.application.CreatePlant
import org.mlanau.project.plant.application.DeleteCareRule
import org.mlanau.project.plant.application.DeleteCareTask
import org.mlanau.project.plant.application.DeletePlant
import org.mlanau.project.plant.application.FindAllPlants
import org.mlanau.project.plant.application.FindPlantById
import org.mlanau.project.plant.application.GetCalendarTasks
import org.mlanau.project.plant.application.GetCareRules
import org.mlanau.project.plant.application.GetNextPendingCare
import org.mlanau.project.plant.application.GetNextPendingCareForPlants
import org.mlanau.project.plant.application.GetPlantCareHistory
import org.mlanau.project.plant.application.GetRecentPlantCareHistory
import org.mlanau.project.plant.application.LogAdHocCare
import org.mlanau.project.plant.application.SaveCareRule
import org.mlanau.project.plant.application.SyncCareReminder
import org.mlanau.project.plant.application.UpdatePlant
import org.mlanau.project.plant.domain.port.CareRuleRepository
import org.mlanau.project.plant.domain.port.CareTaskRepository
import org.mlanau.project.plant.domain.port.PlantRepository
import org.mlanau.project.plant.domain.service.CareScheduler
import org.mlanau.project.plant.infrastructure.persistence.PlantDb
import org.mlanau.project.plant.infrastructure.persistence.SqlDelightCareRuleRepository
import org.mlanau.project.plant.infrastructure.persistence.SqlDelightCareTaskRepository
import org.mlanau.project.plant.infrastructure.persistence.SqlDelightPlantRepository
import org.mlanau.project.plant.presentation.home.HomeViewModel
import org.mlanau.project.plant.presentation.form.PlantFormViewModel
import org.mlanau.project.plant.presentation.calendar.CalendarViewModel
import org.mlanau.project.plant.presentation.detail.PlantDetailViewModel
import org.mlanau.project.plant.presentation.history.PlantHistoryViewModel
import org.mlanau.project.shared.database.DatabaseDriverFactory

val plantModule = module {
    // Foreign key enforcement is configured per-platform inside each DatabaseDriverFactory
    // (AndroidSqliteDriver.Callback.onOpen / NativeSqliteDriver's onConfiguration), since both
    // drivers pool multiple connections and a single `driver.execute(...)` here would only reach
    // one of them.
    single { PlantDb(get<DatabaseDriverFactory>().createDriver()) }

    singleOf(::SqlDelightPlantRepository) bind PlantRepository::class
    singleOf(::SqlDelightCareRuleRepository) bind CareRuleRepository::class
    singleOf(::SqlDelightCareTaskRepository) bind CareTaskRepository::class

    singleOf(::CareScheduler)
    singleOf(::CareReminderSync)

    factoryOf(::FindAllPlants)
    factoryOf(::FindPlantById)
    factoryOf(::CreatePlant)
    factoryOf(::UpdatePlant)
    factoryOf(::DeletePlant)

    factoryOf(::GetCareRules)
    factoryOf(::SaveCareRule)
    factoryOf(::DeleteCareRule)
    factoryOf(::CompleteCareTask)
    factoryOf(::LogAdHocCare)
    factoryOf(::DeleteCareTask)
    factoryOf(::GetPlantCareHistory)
    factoryOf(::GetRecentPlantCareHistory)
    factoryOf(::GetCalendarTasks)
    factoryOf(::GetNextPendingCare)
    factoryOf(::GetNextPendingCareForPlants)
    factoryOf(::SyncCareReminder)

    viewModelOf(::HomeViewModel)
    viewModelOf(::PlantFormViewModel)
    viewModelOf(::CalendarViewModel)
    viewModelOf(::PlantDetailViewModel)
    viewModelOf(::PlantHistoryViewModel)
}

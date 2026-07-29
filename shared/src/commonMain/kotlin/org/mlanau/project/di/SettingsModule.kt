package org.mlanau.project.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mlanau.project.settings.domain.repository.SettingsRepository
import org.mlanau.project.settings.infrastructure.persistence.PersistentSettingsRepository
import org.mlanau.project.settings.presentation.SettingsViewModel

val settingsModule = module {
    singleOf(::PersistentSettingsRepository) bind SettingsRepository::class
    viewModelOf(::SettingsViewModel)
}

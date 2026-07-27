package org.mlanau.project.di

import org.koin.dsl.module
import org.mlanau.project.plant.infrastructure.persistence.IosDatabaseDriverFactory
import org.mlanau.project.plant.infrastructure.persistence.DatabaseDriverFactory

actual val platformModule = module {
    single<DatabaseDriverFactory> { IosDatabaseDriverFactory() }
}

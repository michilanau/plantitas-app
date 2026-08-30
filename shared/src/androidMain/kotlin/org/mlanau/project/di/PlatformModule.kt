package org.mlanau.project.di

import org.koin.dsl.module
import org.mlanau.project.plant.domain.port.ImageStorage
import org.mlanau.project.plant.infrastructure.storage.AndroidImageStorage
import org.mlanau.project.shared.database.AndroidDatabaseDriverFactory
import org.mlanau.project.shared.database.DatabaseDriverFactory

actual val platformModule = module {
    single<DatabaseDriverFactory> { AndroidDatabaseDriverFactory(get()) }
    single<ImageStorage> { AndroidImageStorage(get()) }
}

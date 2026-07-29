package org.mlanau.project.di

import org.koin.dsl.module
import org.mlanau.project.shared.database.AndroidDatabaseDriverFactory
import org.mlanau.project.shared.database.DatabaseDriverFactory

actual val platformModule = module {
    single<DatabaseDriverFactory> { AndroidDatabaseDriverFactory(get()) }
}

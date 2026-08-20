package org.mlanau.project.di

import org.koin.dsl.module
import org.mlanau.project.shared.database.IosDatabaseDriverFactory
import org.mlanau.project.shared.database.DatabaseDriverFactory
import org.mlanau.project.plant.infrastructure.notification.IosCareNotificationScheduler
import org.mlanau.project.plant.domain.service.CareNotificationScheduler
import org.mlanau.project.plant.domain.service.ImageStorage
import org.mlanau.project.plant.infrastructure.storage.IosImageStorage

actual val platformModule = module {
    single<DatabaseDriverFactory> { IosDatabaseDriverFactory() }
    single<CareNotificationScheduler> { IosCareNotificationScheduler() }
    single<ImageStorage> { IosImageStorage() }
}

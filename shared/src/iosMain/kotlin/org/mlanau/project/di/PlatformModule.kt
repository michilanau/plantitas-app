package org.mlanau.project.di

import org.koin.dsl.module
import org.mlanau.project.shared.database.IosDatabaseDriverFactory
import org.mlanau.project.shared.database.DatabaseDriverFactory
import org.mlanau.project.notification.domain.port.NotificationScheduler
import org.mlanau.project.notification.infrastructure.IosNotificationScheduler
import org.mlanau.project.plant.domain.port.ImageStorage
import org.mlanau.project.plant.infrastructure.storage.IosImageStorage

actual val platformModule = module {
    single<DatabaseDriverFactory> { IosDatabaseDriverFactory() }
    single<NotificationScheduler> { IosNotificationScheduler() }
    single<ImageStorage> { IosImageStorage() }
}

package org.mlanau.project.di

import org.koin.dsl.module
import org.mlanau.project.shared.database.IosDatabaseDriverFactory
import org.mlanau.project.shared.database.DatabaseDriverFactory
import org.mlanau.project.shared.notification.IosNotificationService
import org.mlanau.project.shared.notification.NotificationService

actual val platformModule = module {
    single<DatabaseDriverFactory> { IosDatabaseDriverFactory() }
    single<NotificationService> { IosNotificationService() }
}

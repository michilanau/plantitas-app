package org.mlanau.project

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.mlanau.project.di.initKoin
import org.mlanau.project.notification.domain.port.NotificationScheduler
import org.mlanau.project.notification.infrastructure.AndroidNotificationScheduler

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MainApplication)
            modules(module {
                single<NotificationScheduler> { AndroidNotificationScheduler(get()) }
            })
        }
    }
}

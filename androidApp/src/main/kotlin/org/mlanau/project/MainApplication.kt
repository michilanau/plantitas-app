package org.mlanau.project

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.mlanau.project.di.initKoin

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MainApplication)
        }
    }
}

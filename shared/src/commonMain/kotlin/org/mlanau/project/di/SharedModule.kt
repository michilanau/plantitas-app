package org.mlanau.project.di

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import org.koin.dsl.module

val sharedModule = module {
    single { Settings() as ObservableSettings }
}

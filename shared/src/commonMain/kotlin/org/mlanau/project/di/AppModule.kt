package org.mlanau.project.di

import org.koin.dsl.module

val appModule = module {
    includes(sharedModule, plantModule, settingsModule, platformModule)
}

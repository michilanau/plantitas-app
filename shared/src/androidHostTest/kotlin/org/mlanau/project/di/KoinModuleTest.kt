package org.mlanau.project.di

import android.content.Context
import org.koin.test.verify.verify
import org.mlanau.project.shared.notification.NotificationService
import kotlin.test.Test

class KoinModuleTest {
    @Test
    fun checkAllModules() {
        appModule.verify(
            extraTypes = listOf(
                Context::class,
                NotificationService::class
            )
        )
    }
}

package org.mlanau.project.di

import android.content.Context
import org.koin.test.verify.verify
import org.mlanau.project.plant.domain.service.CareNotificationScheduler
import kotlin.test.Test

class KoinModuleTest {
    @Test
    fun checkAllModules() {
        appModule.verify(
            extraTypes = listOf(
                Context::class,
                // Bound in :androidApp's MainApplication (needs the app's own Context there),
                // not in shared's platformModule — see AndroidCareNotificationScheduler's doc.
                CareNotificationScheduler::class
            )
        )
    }
}

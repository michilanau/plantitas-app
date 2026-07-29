package org.mlanau.project.di

import android.content.Context
import org.koin.test.verify.verify
import kotlin.test.Test

class KoinModuleTest {
    @Test
    fun checkAllModules() {
        appModule.verify(
            extraTypes = listOf(
                Context::class
            )
        )
    }
}

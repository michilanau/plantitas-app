package org.mlanau.project.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf

val LocalIosLocale = staticCompositionLocalOf { "es" }

actual object LocalAppLocale {
    @Composable
    actual infix fun provides(languageCode: String): ProvidedValue<*> {
        return LocalIosLocale.provides(languageCode)
    }
}

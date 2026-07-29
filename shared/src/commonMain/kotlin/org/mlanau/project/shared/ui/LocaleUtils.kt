package org.mlanau.project.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.key

expect object LocalAppLocale {
    @Composable
    infix fun provides(languageCode: String): ProvidedValue<*>
}

@Composable
fun AppLocaleWrapper(
    languageCode: String,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAppLocale provides languageCode
    ) {
        key(languageCode) {
            content()
        }
    }
}

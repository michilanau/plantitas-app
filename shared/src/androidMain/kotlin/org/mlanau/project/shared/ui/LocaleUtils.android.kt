package org.mlanau.project.shared.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

actual object LocalAppLocale {
    @Composable
    actual infix fun provides(languageCode: String): ProvidedValue<*> {
        val configuration = Configuration(LocalConfiguration.current)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        configuration.setLocale(locale)
        
        val resources = LocalContext.current.resources
        resources.updateConfiguration(configuration, resources.displayMetrics)
        
        return LocalConfiguration.provides(configuration)
    }
}

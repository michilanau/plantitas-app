package org.mlanau.project.di

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import kotlin.time.Clock
import org.koin.dsl.module
import org.mlanau.project.shared.time.SystemTimeZoneProvider
import org.mlanau.project.shared.time.TimeZoneProvider

val sharedModule = module {
    single { Settings() as ObservableSettings }

    // Ports for ambient time state, so use cases depend on an injected seam instead of calling
    // Clock.System / TimeZone.currentSystemDefault() directly.
    single<Clock> { Clock.System }
    single<TimeZoneProvider> { SystemTimeZoneProvider }
}

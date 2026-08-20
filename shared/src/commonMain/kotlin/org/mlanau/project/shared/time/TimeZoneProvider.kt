package org.mlanau.project.shared.time

import kotlinx.datetime.TimeZone

/**
 * Wraps [TimeZone.currentSystemDefault] behind an injectable seam, the same way [kotlin.time.Clock]
 * already wraps "now". Use cases and domain services take this instead of calling
 * `TimeZone.currentSystemDefault()` directly, so tests can supply a fixed zone.
 */
fun interface TimeZoneProvider {
    operator fun invoke(): TimeZone
}

val SystemTimeZoneProvider = TimeZoneProvider { TimeZone.currentSystemDefault() }

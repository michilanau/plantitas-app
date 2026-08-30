package org.mlanau.project.shared.time

import kotlinx.datetime.TimeZone

fun interface TimeZoneProvider {
    operator fun invoke(): TimeZone
}

val SystemTimeZoneProvider = TimeZoneProvider { TimeZone.currentSystemDefault() }

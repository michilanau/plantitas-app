package org.mlanau.project.shared.ui

import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object DateFormatUtils {
    fun formatTime(localDateTime: LocalDateTime): String {
        val hour = localDateTime.hour.toString().padStart(2, '0')
        val minute = localDateTime.minute.toString().padStart(2, '0')
        return "$hour:$minute"
    }

    fun formatTime(instant: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
        return formatTime(instant.toLocalDateTime(timeZone))
    }

    fun formatDate(localDateTime: LocalDateTime): String {
        val day = localDateTime.day.toString().padStart(2, '0')
        val month = localDateTime.monthNumber.toString().padStart(2, '0')
        return "$day/$month/${localDateTime.year}"
    }

    fun formatDate(instant: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
        return formatDate(instant.toLocalDateTime(timeZone))
    }

    fun formatDateTime(localDateTime: LocalDateTime): String {
        return "${formatDate(localDateTime)} ${formatTime(localDateTime)}"
    }

    fun formatDateTime(instant: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
        return formatDateTime(instant.toLocalDateTime(timeZone))
    }
}

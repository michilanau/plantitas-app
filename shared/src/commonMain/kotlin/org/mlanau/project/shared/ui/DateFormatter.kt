package org.mlanau.project.shared.ui

import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.time.Instant
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

/**
 * Date and time formatting whose shape follows the app's chosen language rather than the device
 * region — the app already lets the user pick the language explicitly, so that is the signal.
 * `es` uses `dd/MM/yyyy` and a 24-hour clock; `en` uses `MM/dd/yyyy` and a 12-hour clock.
 *
 * kotlinx-datetime has no locale support, so this is deliberately just the two shapes the app
 * ships, not a general localization layer.
 */
class DateFormatter(languageCode: String) {

    private val english = languageCode == "en"

    /** Which day a calendar week starts on: Sunday for `en`, Monday everywhere else. */
    val firstDayOfWeek: DayOfWeek = if (english) DayOfWeek.SUNDAY else DayOfWeek.MONDAY

    /** Whether times are shown (and picked) on a 24-hour clock. `en` uses AM/PM. */
    val uses24HourClock: Boolean = !english

    private val dateFormat = if (english) {
        LocalDate.Format { monthNumber(); char('/'); day(); char('/'); year() }
    } else {
        LocalDate.Format { day(); char('/'); monthNumber(); char('/'); year() }
    }

    private val timeFormat = if (english) {
        LocalTime.Format { amPmHour(); char(':'); minute(); char(' '); amPmMarker("AM", "PM") }
    } else {
        LocalTime.Format { hour(); char(':'); minute() }
    }

    fun formatDate(date: LocalDate): String = dateFormat.format(date)

    fun formatDate(instant: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
        dateFormat.format(instant.toLocalDateTime(timeZone).date)

    fun formatTime(time: LocalTime): String = timeFormat.format(time)

    fun formatTime(instant: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
        timeFormat.format(instant.toLocalDateTime(timeZone).time)

    fun formatDateTime(instant: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
        val localDateTime = instant.toLocalDateTime(timeZone)
        return "${dateFormat.format(localDateTime.date)} ${timeFormat.format(localDateTime.time)}"
    }
}

val LocalDateFormatter = staticCompositionLocalOf { DateFormatter("es") }

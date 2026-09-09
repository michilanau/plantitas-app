package org.mlanau.project.shared.ui

import androidx.compose.runtime.Composable
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.stringResource
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.weekday_friday
import plantitas_app.shared.generated.resources.weekday_monday
import plantitas_app.shared.generated.resources.weekday_saturday
import plantitas_app.shared.generated.resources.weekday_short_friday
import plantitas_app.shared.generated.resources.weekday_short_monday
import plantitas_app.shared.generated.resources.weekday_short_saturday
import plantitas_app.shared.generated.resources.weekday_short_sunday
import plantitas_app.shared.generated.resources.weekday_short_thursday
import plantitas_app.shared.generated.resources.weekday_short_tuesday
import plantitas_app.shared.generated.resources.weekday_short_wednesday
import plantitas_app.shared.generated.resources.weekday_sunday
import plantitas_app.shared.generated.resources.weekday_thursday
import plantitas_app.shared.generated.resources.weekday_tuesday
import plantitas_app.shared.generated.resources.weekday_wednesday

/** Localized weekday names. Shared by the calendar grid and the Home header's date line. */
@Composable
fun weekdayName(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> stringResource(Res.string.weekday_monday)
    DayOfWeek.TUESDAY -> stringResource(Res.string.weekday_tuesday)
    DayOfWeek.WEDNESDAY -> stringResource(Res.string.weekday_wednesday)
    DayOfWeek.THURSDAY -> stringResource(Res.string.weekday_thursday)
    DayOfWeek.FRIDAY -> stringResource(Res.string.weekday_friday)
    DayOfWeek.SATURDAY -> stringResource(Res.string.weekday_saturday)
    DayOfWeek.SUNDAY -> stringResource(Res.string.weekday_sunday)
}

@Composable
fun weekdayShortName(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> stringResource(Res.string.weekday_short_monday)
    DayOfWeek.TUESDAY -> stringResource(Res.string.weekday_short_tuesday)
    DayOfWeek.WEDNESDAY -> stringResource(Res.string.weekday_short_wednesday)
    DayOfWeek.THURSDAY -> stringResource(Res.string.weekday_short_thursday)
    DayOfWeek.FRIDAY -> stringResource(Res.string.weekday_short_friday)
    DayOfWeek.SATURDAY -> stringResource(Res.string.weekday_short_saturday)
    DayOfWeek.SUNDAY -> stringResource(Res.string.weekday_short_sunday)
}

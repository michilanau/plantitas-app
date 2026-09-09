package org.mlanau.project.shared.ui

import androidx.compose.runtime.Composable
import kotlinx.datetime.Month
import org.jetbrains.compose.resources.stringResource
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.month_april
import plantitas_app.shared.generated.resources.month_august
import plantitas_app.shared.generated.resources.month_december
import plantitas_app.shared.generated.resources.month_february
import plantitas_app.shared.generated.resources.month_january
import plantitas_app.shared.generated.resources.month_july
import plantitas_app.shared.generated.resources.month_june
import plantitas_app.shared.generated.resources.month_march
import plantitas_app.shared.generated.resources.month_may
import plantitas_app.shared.generated.resources.month_november
import plantitas_app.shared.generated.resources.month_october
import plantitas_app.shared.generated.resources.month_september

/** Localized month names. Shared by the calendar header and the Home header's date line. */
@Composable
fun monthName(month: Month): String = when (month) {
    Month.JANUARY -> stringResource(Res.string.month_january)
    Month.FEBRUARY -> stringResource(Res.string.month_february)
    Month.MARCH -> stringResource(Res.string.month_march)
    Month.APRIL -> stringResource(Res.string.month_april)
    Month.MAY -> stringResource(Res.string.month_may)
    Month.JUNE -> stringResource(Res.string.month_june)
    Month.JULY -> stringResource(Res.string.month_july)
    Month.AUGUST -> stringResource(Res.string.month_august)
    Month.SEPTEMBER -> stringResource(Res.string.month_september)
    Month.OCTOBER -> stringResource(Res.string.month_october)
    Month.NOVEMBER -> stringResource(Res.string.month_november)
    Month.DECEMBER -> stringResource(Res.string.month_december)
}

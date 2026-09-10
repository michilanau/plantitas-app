package org.mlanau.project.plant.presentation.component

import androidx.compose.runtime.Composable
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.domain.model.CareTask
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.care_due_in_days
import plantitas_app.shared.generated.resources.care_due_today
import plantitas_app.shared.generated.resources.care_due_tomorrow
import plantitas_app.shared.generated.resources.care_overdue_days
import plantitas_app.shared.generated.resources.care_overdue_missed_count

/** Calendar days from today to the day [this] is due; zero for today and for anything already late. */
fun CareTask.Pending.daysUntilDue(timeZone: TimeZone = TimeZone.currentSystemDefault()): Int =
    Clock.System.now().toLocalDateTime(timeZone).date
        .daysUntil(dueAt.toLocalDateTime(timeZone).date)
        .coerceAtLeast(0)

/**
 * When a pending care is due, in lowercase so it can follow a verb or a care name: "hoy",
 * "mañana", "en 3 días", or — for an overdue one — how late it is, counted from the day it was first
 * owed (the same measure the reminder notification uses) plus the backlog it stands for.
 */
@Composable
fun dueLabel(task: CareTask.Pending): String {
    val timeZone = TimeZone.currentSystemDefault()
    if (task.isOverdue) {
        val daysLate = task.dueAt.daysUntil(Clock.System.now(), timeZone).coerceAtLeast(0)
        val lateness = if (daysLate == 0) {
            stringResource(Res.string.care_due_today)
        } else {
            pluralStringResource(Res.plurals.care_overdue_days, daysLate, daysLate)
        }
        return if (task.missedCount > 1) {
            "$lateness  ${stringResource(Res.string.care_overdue_missed_count, task.missedCount)}"
        } else {
            lateness
        }
    }
    return when (val days = task.daysUntilDue(timeZone)) {
        0 -> stringResource(Res.string.care_due_today)
        1 -> stringResource(Res.string.care_due_tomorrow)
        else -> pluralStringResource(Res.plurals.care_due_in_days, days, days)
    }
}
